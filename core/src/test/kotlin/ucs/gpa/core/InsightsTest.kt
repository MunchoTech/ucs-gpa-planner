package ucs.gpa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ucs.gpa.core.calc.GpaCalculator
import ucs.gpa.core.insight.Insight
import ucs.gpa.core.insight.InsightEngine
import ucs.gpa.core.plan.TargetPlanner

class InsightsTest {

    @Test fun `no insights without graded courses`() {
        val r = record(year(1, full(null), half(null)))
        val result = GpaCalculator.record(r, rules)
        assertTrue(InsightEngine.generate(result, TargetPlanner.analyse(r, rules), rules).isEmpty())
    }

    @Test fun `strongest year and trend need two graded years`() {
        val one = GpaCalculator.record(record(year(1, full("A"), full("B"), full("B+"))), rules)
        val insights = InsightEngine.generate(one, null, rules)
        assertTrue(insights.none { it is Insight.StrongestYear || it is Insight.Trend })

        val two = GpaCalculator.record(record(year(1, full("B"), full("B")), year(2, full("A"), full("A"))), rules)
        val all = InsightEngine.generate(two, null, rules)
        val strongest = all.filterIsInstance<Insight.StrongestYear>().single()
        assertEquals(2, strongest.best.year.yearOfStudy)
        val trend = all.filterIsInstance<Insight.Trend>().single()
        assertClose(1.0, trend.change)
    }

    @Test fun `tied years are reported as ties`() {
        val r = GpaCalculator.record(record(year(1, full("A")), year(2, half("A"), half("A"))), rules)
        val strongest = InsightEngine.generate(r, null, rules).filterIsInstance<Insight.StrongestYear>().single()
        assertEquals(1, strongest.tiedWith.size)
    }

    @Test fun `top contributors are ranked by credits times points`() {
        val r = GpaCalculator.record(record(year(1, half("A+"), full("B"), half("C"), full("A"))), rules)
        val top = InsightEngine.generate(r, null, rules).filterIsInstance<Insight.TopContributors>().single()
        // full A = 120, full B = 90, half A+ = 75
        assertEquals(listOf(12000L, 9000L, 7500L), top.courses.map { it.weightedHundredths })
    }

    @Test fun `below-average courses are only those under the GPA`() {
        val r = GpaCalculator.record(record(year(1, full("A"), full("A"), half("B"), half("C+"))), rules)
        val below = InsightEngine.generate(r, null, rules).filterIsInstance<Insight.BelowAverage>().single()
        assertTrue(below.courses.all { it.first.grade!!.points < below.gpa })
        assertEquals("C+", below.courses.first().first.grade!!.code)
    }

    @Test fun `weight influence uses completed plus remaining credits`() {
        val rec = record(year(1, full("A"), full("A"), full("A"), full("A")), year(2, full(null), full(null), full(null), full(null)))
        val result = GpaCalculator.record(rec, rules)
        val influence = InsightEngine.generate(result, TargetPlanner.analyse(rec, rules), rules)
            .filterIsInstance<Insight.CourseWeightInfluence>().single()
        assertEquals(240, influence.totalCredits)
        assertClose(30 * 0.5 / 240, influence.fullStep)
        assertClose(influence.fullStep / 2, influence.halfStep)
        assertTrue(influence.usesRemainingCourses)
    }
}
