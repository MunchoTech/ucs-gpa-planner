package ucs.gpa.core.plan

import ucs.gpa.core.calc.GpaSummary
import ucs.gpa.core.rules.AcademicRules
import ucs.gpa.core.rules.ClassBand
import ucs.gpa.core.rules.GradeDefinition

/** Courses that received the same hypothetical grade in a combination. */
data class GradeGroup(
    val grade: String,
    val courseIds: List<String>,
    val fullCourses: Int,
    val halfCourses: Int,
    val otherCourses: Int,
)

enum class CombinationKind { SAME_GRADE, STRONG_FULL, FEWEST_STEPS }

data class GradeCombination(
    val kind: CombinationKind,
    /** courseId → grade code. */
    val assignments: Map<String, String>,
    val groups: List<GradeGroup>,
    val finalGpa: Double,
    val band: ClassBand,
    /** True when the combination relies on A+ grades. */
    val demanding: Boolean,
)

/** Result of applying hypothetical grades to the remaining courses. */
data class ScenarioProjection(
    val projected: GpaSummary,
    val band: ClassBand?,
    val assignedCount: Int,
    val unassignedCount: Int,
    val unassignedCredits: Int,
    val meetsTarget: Boolean?,
) {
    val gpa: Double? get() = projected.gpa
    val isComplete: Boolean get() = unassignedCount == 0
}

object Scenarios {

    /**
     * Projects the final GPA from completed results plus hypothetical grades. Courses without a
     * hypothetical grade are left out of the projection and reported in [ScenarioProjection.unassignedCount],
     * so the projection never pretends to know them.
     */
    fun project(
        current: GpaSummary,
        remaining: List<RemainingCourse>,
        assignments: Map<String, String>,
        target: ResolvedTarget?,
        rules: AcademicRules,
    ): ScenarioProjection {
        var credits = current.countedCredits
        var weighted = current.weightedHundredths
        var assigned = 0
        var unassignedCredits = 0
        remaining.forEach { rc ->
            val g = rules.grade(assignments[rc.course.id])
            if (g != null) {
                credits += rc.credits
                weighted += rc.credits.toLong() * g.pointsHundredths
                assigned++
            } else {
                unassignedCredits += rc.credits
            }
        }
        val summary = current.copy(countedCredits = credits, weightedHundredths = weighted)
        val band = TargetPlanner.classifyFinal(weighted, credits, rules)
        val meets = if (target != null && credits > 0) weighted >= target.hundredths.toLong() * credits else null
        return ScenarioProjection(summary, band, assigned, remaining.size - assigned, unassignedCredits, meets)
    }

    /**
     * Finds a few realistic grade combinations that reach [target]. Returns an empty list when the
     * target is out of reach or already secured.
     */
    fun combinations(analysis: TargetAnalysis, rules: AcademicRules): List<GradeCombination> {
        val remaining = analysis.remaining
        if (remaining.isEmpty()) return emptyList()
        if (analysis.feasibility != Feasibility.ACHIEVABLE && analysis.feasibility != Feasibility.SECURED_IF_PASSED) return emptyList()

        val ladder: List<GradeDefinition> = rules.planningGradeDefinitions.sortedBy { it.pointsHundredths } // lowest first
        val c = analysis.current.countedCredits.toLong()
        val w = analysis.current.weightedHundredths
        val t = analysis.target.hundredths.toLong()
        val totalCredits = c + remaining.sumOf { it.credits }

        fun weightedOf(grades: List<GradeDefinition>): Long =
            w + remaining.indices.sumOf { remaining[it].credits.toLong() * grades[it].pointsHundredths }

        fun meets(grades: List<GradeDefinition>) = weightedOf(grades) >= t * totalCredits

        val isFull = remaining.map { it.credits >= rules.fullCourseCredits }
        val results = mutableListOf<Pair<CombinationKind, List<GradeDefinition>>>()

        // 1. The lowest single grade that works everywhere.
        val uniformIndex = ladder.indices.firstOrNull { i -> meets(remaining.map { ladder[i] }) } ?: return emptyList()
        results += CombinationKind.SAME_GRADE to remaining.map { ladder[uniformIndex] }

        val hasFull = isFull.any { it }
        val hasHalf = isFull.any { !it }

        // 2. Lean on the full courses: other courses one step below the uniform grade, full courses
        // the lowest grade above it that still reaches the target. Never more than one step down,
        // so the suggestion stays realistic.
        if (hasFull && hasHalf && uniformIndex > 0) {
            val lo = uniformIndex - 1
            for (hi in (uniformIndex + 1) until ladder.size) {
                val grades = remaining.indices.map { i -> if (isFull[i]) ladder[hi] else ladder[lo] }
                if (meets(grades)) {
                    results += CombinationKind.STRONG_FULL to grades
                    break
                }
            }
        }

        // 4. Start one step below the uniform grade and raise the heaviest courses first.
        if (uniformIndex > 0) {
            val grades = remaining.map { ladder[uniformIndex - 1] }.toMutableList()
            val order = remaining.indices.sortedWith(compareByDescending<Int> { remaining[it].credits }.thenBy { it })
            for (i in order) {
                if (meets(grades)) break
                grades[i] = ladder[uniformIndex]
            }
            if (meets(grades)) results += CombinationKind.FEWEST_STEPS to grades
        }

        val seen = mutableSetOf<List<String>>()
        return results.mapNotNull { (kind, grades) ->
            val key = grades.map { it.code }
            if (!seen.add(key)) return@mapNotNull null
            val weighted = weightedOf(grades)
            val credits = totalCredits.toInt()
            GradeCombination(
                kind = kind,
                assignments = remaining.indices.associate { remaining[it].course.id to grades[it].code },
                groups = group(remaining, grades, rules),
                finalGpa = weighted / 100.0 / credits,
                band = TargetPlanner.classifyFinal(weighted, credits, rules)!!,
                demanding = grades.any { it.pointsHundredths == rules.maxPointsHundredths },
            )
        }
    }

    private fun group(remaining: List<RemainingCourse>, grades: List<GradeDefinition>, rules: AcademicRules): List<GradeGroup> =
        remaining.indices.groupBy { grades[it].code }
            .map { (code, idx) ->
                GradeGroup(
                    grade = code,
                    courseIds = idx.map { remaining[it].course.id },
                    fullCourses = idx.count { remaining[it].credits == rules.fullCourseCredits },
                    halfCourses = idx.count { remaining[it].credits == rules.halfCourseCredits },
                    otherCourses = idx.count { remaining[it].credits != rules.fullCourseCredits && remaining[it].credits != rules.halfCourseCredits },
                )
            }
            .sortedByDescending { rules.grade(it.grade)!!.pointsHundredths }
}
