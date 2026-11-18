package ucs.gpa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import ucs.gpa.core.calc.GpaCalculator
import ucs.gpa.core.model.PlannerTarget
import ucs.gpa.core.plan.CombinationKind
import ucs.gpa.core.plan.Feasibility
import ucs.gpa.core.plan.ResolvedTarget
import ucs.gpa.core.plan.Scenarios
import ucs.gpa.core.plan.TargetPlanner

class PlannerTest {

    private val distinction = ResolvedTarget(375, rules.band("distinction"))

    /** 120 credits at 3.625 completed (435 points); a year of 2 full + 5 half courses (135 credits) to go. */
    private fun finalYearRecord() = record(
        year(1, half("A"), half("B+"), half("B"), half("A"), half("B+"), half("B"), full("A")),
        year(2, full(null), full(null), half(null), half(null), half(null), half(null), half(null)),
    )

    @Test fun `required average over the remaining credits`() {
        val a = TargetPlanner.analyse(finalYearRecord(), rules, distinction)
        assertEquals(135, a.remainingCredits)
        assertEquals(7, a.remaining.size)
        // (3.75 × 255 − 435) / 135 = 3.86111
        assertClose((3.75 * 255 - 435) / 135, a.requiredAverage, 1e-9)
        assertEquals(Feasibility.ACHIEVABLE, a.feasibility)
        assertClose((435 + 5.0 * 135) / 255, a.bestPossible)
    }

    @Test fun `secured when even the lowest results keep the target`() {
        val r = record(year(1, full("A+"), full("A+"), full("A+")), year(2, half(null)))
        val a = TargetPlanner.analyse(r, rules, distinction)
        // needs (3.75 × 105 − 450) / 15 = −3.75 → secured
        assertEquals(Feasibility.SECURED, a.feasibility)
        assertTrue(a.requiredAverage!! <= 0)
    }

    @Test fun `secured if passed when a C average is enough`() {
        val r = record(year(1, full("A"), full("A"), full("A")), year(2, full(null)))
        val merit = ResolvedTarget(325, rules.band("merit"))
        val a = TargetPlanner.analyse(r, rules, merit)
        // needs (3.25 × 120 − 360) / 30 = 1.00 → exactly the lowest pass
        assertEquals(Feasibility.SECURED_IF_PASSED, a.feasibility)
    }

    @Test fun `impossible targets are never reported as achievable`() {
        val r = record(year(1, full("C"), full("C"), full("C")), year(2, half(null)))
        val a = TargetPlanner.analyse(r, rules, distinction)
        assertEquals(Feasibility.NOT_ACHIEVABLE, a.feasibility)
        assertTrue(a.requiredAverage!! > 5.0)
        assertClose((90.0 + 75) / 105, a.bestPossible) // three full C (1.00) plus one half A+
        assertTrue(Scenarios.combinations(a, rules).isEmpty())
    }

    @Test fun `met, missed and no-data cases without remaining courses`() {
        assertEquals(Feasibility.MET, TargetPlanner.analyse(record(year(1, full("A"))), rules, distinction).feasibility)
        assertEquals(Feasibility.MISSED, TargetPlanner.analyse(record(year(1, full("B"))), rules, distinction).feasibility)
        val none = TargetPlanner.analyse(record(), rules, distinction)
        assertEquals(Feasibility.NEEDS_DATA, none.feasibility)
        assertNull(none.requiredAverage)
    }

    @Test fun `estimated remaining credits are used only when no courses are entered`() {
        val r = record(year(1, full("B"), full("B"), full("B"), full("B")))
        val a = TargetPlanner.analyse(r, rules, distinction, estimatedRemainingCredits = 360)
        assertTrue(a.isEstimate)
        assertEquals(360, a.remainingCredits)
        assertClose((3.75 * 480 - 360) / 360, a.requiredAverage)

        val withCourses = TargetPlanner.analyse(finalYearRecord(), rules, distinction, estimatedRemainingCredits = 999)
        assertFalse(withCourses.isEstimate)
        assertEquals(135, withCourses.remainingCredits)
    }

    @Test fun `non-credit, excluded and repeat courses are not remaining`() {
        val r = record(year(3, half(null), custom(null, 0), full(null).copy(countsTowardGpa = false), full(null).copy(isRepeat = true)))
        val remaining = TargetPlanner.remainingCourses(GpaCalculator.record(r, rules))
        assertEquals(1, remaining.size)
    }

    @Test fun `temporary grades count as remaining until resolved`() {
        val r = record(year(1, full("A"), half("IN")))
        assertEquals(15, TargetPlanner.analyse(r, rules, distinction).remainingCredits)
    }

    @Test fun `custom and band targets resolve`() {
        assertEquals(375, TargetPlanner.resolve(PlannerTarget("distinction"), rules).hundredths)
        assertEquals(325, TargetPlanner.resolve(PlannerTarget("merit"), rules).hundredths)
        val custom = TargetPlanner.resolve(PlannerTarget(bandId = null, customGpa = 3.5), rules)
        assertEquals(350, custom.hundredths)
        assertNull(custom.band)
        // out-of-range custom targets fall back to the top class
        assertEquals(375, TargetPlanner.resolve(PlannerTarget(bandId = null, customGpa = 7.0), rules).hundredths)
    }

    @Test fun `every suggested combination reaches the target and they differ`() {
        val a = TargetPlanner.analyse(finalYearRecord(), rules, distinction)
        val combos = Scenarios.combinations(a, rules)
        assertTrue(combos.size >= 2)
        assertEquals(combos.size, combos.map { it.assignments }.toSet().size)
        combos.forEach { combo ->
            assertTrue(combo.finalGpa >= 3.75 - 1e-12, "${combo.kind} gives ${combo.finalGpa}")
            assertEquals("distinction", combo.band.id)
            assertEquals(a.remaining.map { it.course.id }.toSet(), combo.assignments.keys)
        }
    }

    @Test fun `same-grade combination is the lowest single grade that works`() {
        val a = TargetPlanner.analyse(finalYearRecord(), rules, distinction)
        val same = Scenarios.combinations(a, rules).first { it.kind == CombinationKind.SAME_GRADE }
        // Required average 3.861 → B+ (3.50) is not enough everywhere, A (4.00) is.
        assertEquals(setOf("A"), same.assignments.values.toSet())
        val lower = Scenarios.project(a.current, a.remaining, a.remaining.associate { it.course.id to "B+" }, distinction, rules)
        assertFalse(lower.meetsTarget!!)
    }

    @Test fun `strong full course combination gives full courses the higher grade`() {
        val r = record(
            year(1, full("B+"), full("B+"), full("B+"), full("B+")),
            year(2, full(null), full(null), half(null), half(null), half(null), half(null)),
        )
        val merit = ResolvedTarget(325, rules.band("merit"))
        val a = TargetPlanner.analyse(r, rules, merit)
        val strong = Scenarios.combinations(a, rules).firstOrNull { it.kind == CombinationKind.STRONG_FULL }
        assertNotNull(strong)
        val fullIds = a.remaining.filter { it.credits == 30 }.map { it.course.id }
        val halfIds = a.remaining.filter { it.credits == 15 }.map { it.course.id }
        val fullPoints = fullIds.map { rules.grade(strong.assignments[it])!!.pointsHundredths }.distinct().single()
        val halfPoints = halfIds.map { rules.grade(strong.assignments[it])!!.pointsHundredths }.distinct().single()
        assertTrue(fullPoints > halfPoints)
    }

    @Test fun `what-if projection reports courses without a hypothetical grade`() {
        val a = TargetPlanner.analyse(finalYearRecord(), rules, distinction)
        val fulls = a.remaining.filter { it.credits == 30 }
        val partial = Scenarios.project(a.current, a.remaining, fulls.associate { it.course.id to "A" }, distinction, rules)
        assertEquals(2, partial.assignedCount)
        assertEquals(5, partial.unassignedCount)
        assertEquals(75, partial.unassignedCredits)
        assertFalse(partial.isComplete)
        // completed 435 over 120 plus two full A's (240 over 60) = 675 / 180
        assertClose(675.0 / 180, partial.gpa)

        // "A in my full courses and B+ in my half courses"
        val all = a.remaining.associate { it.course.id to if (it.credits == 30) "A" else "B+" }
        val complete = Scenarios.project(a.current, a.remaining, all, distinction, rules)
        assertTrue(complete.isComplete)
        // (435 + 240 + 75 × 3.5) / 255 = 937.5 / 255 = 3.6765: short of Distinction
        assertClose(937.5 / 255, complete.gpa)
        assertFalse(complete.meetsTarget!!)
        assertEquals("merit", complete.band!!.id)
    }

    @Test fun `a full course step moves the GPA twice as much as a half course step`() {
        val full = TargetPlanner.stepImpact(30, 480)
        val half = TargetPlanner.stepImpact(15, 480)
        assertClose(0.03125, full)
        assertClose(full / 2, half)
    }
}
