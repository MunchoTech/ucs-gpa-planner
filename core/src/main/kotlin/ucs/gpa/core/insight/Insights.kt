package ucs.gpa.core.insight

import ucs.gpa.core.calc.CourseResult
import ucs.gpa.core.calc.CourseStatus
import ucs.gpa.core.calc.RecordResult
import ucs.gpa.core.calc.YearResult
import ucs.gpa.core.plan.TargetAnalysis
import ucs.gpa.core.plan.TargetPlanner
import ucs.gpa.core.rules.AcademicRules

/**
 * Facts derived from the entered results. Each insight is only produced when there is enough data
 * for it to be true; nothing here guesses at reasons or weaknesses.
 */
sealed interface Insight {
    val id: String

    data class StrongestYear(val best: YearResult, val compared: Int, val tiedWith: List<YearResult>) : Insight {
        override val id = "strongest-year"
    }

    data class Trend(val points: List<Pair<YearResult, Double>>) : Insight {
        override val id = "trend"
        val first get() = points.first()
        val last get() = points.last()
        val change: Double get() = last.second - first.second
    }

    /** Courses ranked by credits × grade points: the biggest contributions to the GPA total. */
    data class TopContributors(val courses: List<CourseResult>, val totalWeightedHundredths: Long) : Insight {
        override val id = "top-contributors"
    }

    /** Graded courses whose grade points are below the current GPA, ranked by how far they pull it down. */
    data class BelowAverage(val courses: List<Pair<CourseResult, Double>>, val gpa: Double) : Insight {
        override val id = "below-average"
    }

    data class GradeDistribution(val counts: List<Pair<String, Int>>, val total: Int) : Insight {
        override val id = "grade-distribution"
    }

    /** How much one grade step moves the final GPA in a full course versus a half course. */
    data class CourseWeightInfluence(
        val totalCredits: Int,
        val fullCredits: Int,
        val halfCredits: Int,
        val fullStep: Double,
        val halfStep: Double,
        val usesRemainingCourses: Boolean,
    ) : Insight {
        override val id = "weight-influence"
    }

    data class TargetProgress(val analysis: TargetAnalysis) : Insight {
        override val id = "target-progress"
    }
}

object InsightEngine {

    fun generate(result: RecordResult, analysis: TargetAnalysis?, rules: AcademicRules): List<Insight> {
        val out = mutableListOf<Insight>()
        val graded = result.years.filter { it.hasGrades }
        val counted = result.years.flatMap { it.courses }.filter { it.status == CourseStatus.COUNTED && it.includedInGpa }
        if (counted.isEmpty()) return emptyList()

        if (analysis != null) out += Insight.TargetProgress(analysis)

        if (graded.size >= 2) {
            val sorted = graded.sortedByDescending { it.summary.weightedHundredths.toDouble() / it.summary.countedCredits }
            val best = sorted.first()
            val ties = sorted.drop(1).filter { sameGpa(it, best) }
            out += Insight.StrongestYear(best, graded.size, ties)
            out += Insight.Trend(graded.map { it to it.gpa!! })
        }

        if (counted.size >= 3) {
            val total = counted.sumOf { it.weightedHundredths }
            out += Insight.TopContributors(counted.sortedByDescending { it.weightedHundredths }.take(3), total)
        }

        val gpa = result.gpa
        if (gpa != null && counted.size >= 3) {
            val below = counted
                .filter { it.grade!!.points < gpa - 1e-9 }
                .map { it to it.credits!! * (gpa - it.grade!!.points) }
                .sortedByDescending { it.second }
                .take(3)
            if (below.isNotEmpty()) out += Insight.BelowAverage(below, gpa)
        }

        val distribution = counted.groupingBy { it.grade!!.code }.eachCount()
        out += Insight.GradeDistribution(
            rules.grades.filter { distribution.containsKey(it.code) }.map { it.code to distribution.getValue(it.code) },
            counted.size,
        )

        val remainingCredits = analysis?.remainingCredits ?: 0
        val total = result.overall.countedCredits + remainingCredits
        out += Insight.CourseWeightInfluence(
            totalCredits = total,
            fullCredits = rules.fullCourseCredits,
            halfCredits = rules.halfCourseCredits,
            fullStep = TargetPlanner.stepImpact(rules.fullCourseCredits, total),
            halfStep = TargetPlanner.stepImpact(rules.halfCourseCredits, total),
            usesRemainingCourses = remainingCredits > 0,
        )
        return out
    }

    private fun sameGpa(a: YearResult, b: YearResult): Boolean =
        a.summary.weightedHundredths * b.summary.countedCredits == b.summary.weightedHundredths * a.summary.countedCredits
}
