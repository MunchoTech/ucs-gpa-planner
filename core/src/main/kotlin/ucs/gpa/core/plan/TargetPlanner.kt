package ucs.gpa.core.plan

import ucs.gpa.core.calc.Classifier
import ucs.gpa.core.calc.GpaCalculator
import ucs.gpa.core.calc.GpaSummary
import ucs.gpa.core.calc.RecordResult
import ucs.gpa.core.model.Course
import ucs.gpa.core.model.PlannerTarget
import ucs.gpa.core.model.StudentRecord
import ucs.gpa.core.rules.AcademicRules
import ucs.gpa.core.rules.ClassBand

/** A credit-bearing course with no final grade yet. */
data class RemainingCourse(
    val yearId: String,
    val yearOfStudy: Int,
    val course: Course,
    val credits: Int,
)

enum class Feasibility {
    /** Target already met and no courses remain. */
    MET,
    /** No courses remain and the target was not reached. */
    MISSED,
    /** Reached whatever the remaining grades (on GPA alone). */
    SECURED,
    /** Reached as long as every remaining course is passed (C or better). */
    SECURED_IF_PASSED,
    /** Possible with the right grades. */
    ACHIEVABLE,
    /** Not possible even with the top grade in every remaining course. */
    NOT_ACHIEVABLE,
    /** Nothing graded and nothing remaining: no basis for a plan. */
    NEEDS_DATA,
}

data class ResolvedTarget(val hundredths: Int, val band: ClassBand?) {
    val gpa: Double get() = hundredths / 100.0
    val label: String get() = band?.name ?: "GPA ${"%.2f".format(java.util.Locale.ROOT, gpa)}"
}

data class TargetAnalysis(
    val target: ResolvedTarget,
    val current: GpaSummary,
    val remaining: List<RemainingCourse>,
    val remainingCredits: Int,
    /** True when [remainingCredits] is a student estimate rather than entered courses. */
    val isEstimate: Boolean,
    /** Average GPV needed across the remaining credits, or null when nothing remains. */
    val requiredAverage: Double?,
    val feasibility: Feasibility,
    /** Final GPA with the top grade in every remaining course. */
    val bestPossible: Double?,
    /** Final GPA with the lowest pass in every remaining course. */
    val lowestPassing: Double?,
) {
    val currentGpa: Double? get() = current.gpa
    val totalCredits: Int get() = current.countedCredits + remainingCredits
}

object TargetPlanner {

    fun resolve(target: PlannerTarget, rules: AcademicRules): ResolvedTarget {
        val band = rules.band(target.bandId)
        if (band != null && band.minHundredths > 0) return ResolvedTarget(band.minHundredths, band)
        val custom = target.customGpa
        if (custom != null && custom > 0 && custom <= rules.maxPointsHundredths / 100.0) {
            val h = Math.round(custom * 100).toInt()
            val matching = rules.bands.firstOrNull { it.minHundredths == h && h > 0 }
            return ResolvedTarget(h, matching)
        }
        val top = rules.targetBands.first()
        return ResolvedTarget(top.minHundredths, top)
    }

    fun remainingCourses(result: RecordResult): List<RemainingCourse> =
        result.years.flatMap { y ->
            y.courses.filter { it.isPending }.map { RemainingCourse(y.year.id, y.year.yearOfStudy, it.course, it.credits!!) }
        }

    fun analyse(
        record: StudentRecord,
        rules: AcademicRules,
        target: ResolvedTarget = resolve(record.target, rules),
        estimatedRemainingCredits: Int? = null,
    ): TargetAnalysis {
        val result = GpaCalculator.record(record, rules)
        val remaining = remainingCourses(result)
        val entered = remaining.sumOf { it.credits }
        val useEstimate = entered == 0 && estimatedRemainingCredits != null && estimatedRemainingCredits > 0
        val r = if (useEstimate) estimatedRemainingCredits!! else entered
        return analyse(result.overall, remaining.takeUnless { useEstimate } ?: emptyList(), r, useEstimate, target, rules)
    }

    fun analyse(
        current: GpaSummary,
        remaining: List<RemainingCourse>,
        remainingCredits: Int,
        isEstimate: Boolean,
        target: ResolvedTarget,
        rules: AcademicRules,
    ): TargetAnalysis {
        val c = current.countedCredits.toLong()
        val w = current.weightedHundredths
        val r = remainingCredits.toLong()
        val t = target.hundredths.toLong()

        if (r == 0L) {
            val feasibility = when {
                c == 0L -> Feasibility.NEEDS_DATA
                w >= t * c -> Feasibility.MET
                else -> Feasibility.MISSED
            }
            return TargetAnalysis(target, current, remaining, 0, isEstimate, null, feasibility, current.gpa, current.gpa)
        }

        // Points (in hundredths, times credits) still needed from the remaining credits.
        val needed = t * (c + r) - w
        val required = needed / 100.0 / r
        val max = rules.maxPointsHundredths.toLong()
        val minPass = rules.minPassPointsHundredths.toLong()
        val feasibility = when {
            needed <= 0 -> Feasibility.SECURED
            needed <= minPass * r -> Feasibility.SECURED_IF_PASSED
            needed <= max * r -> Feasibility.ACHIEVABLE
            else -> Feasibility.NOT_ACHIEVABLE
        }
        val best = (w + max * r) / 100.0 / (c + r)
        val lowest = (w + minPass * r) / 100.0 / (c + r)
        return TargetAnalysis(target, current, remaining, remainingCredits, isEstimate, required, feasibility, best, lowest)
    }

    /** How much one grade step (default 0.50, e.g. B+ to A) in a course of [credits] moves the final GPA. */
    fun stepImpact(credits: Int, totalCredits: Int, stepHundredths: Int = 50): Double =
        if (totalCredits <= 0) 0.0 else credits * stepHundredths / 100.0 / totalCredits

    /** The best class still reachable, given the remaining credits. */
    fun bestReachableBand(analysisFor: (ResolvedTarget) -> TargetAnalysis, rules: AcademicRules): ClassBand? =
        rules.targetBands.firstOrNull {
            val a = analysisFor(ResolvedTarget(it.minHundredths, it))
            a.feasibility != Feasibility.NOT_ACHIEVABLE && a.feasibility != Feasibility.MISSED && a.feasibility != Feasibility.NEEDS_DATA
        }

    internal fun classifyFinal(weighted: Long, credits: Int, rules: AcademicRules): ClassBand? =
        if (credits > 0) Classifier.classify(weighted, credits, rules) else null
}
