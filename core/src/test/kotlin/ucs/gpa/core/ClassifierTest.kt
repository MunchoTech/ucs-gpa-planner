package ucs.gpa.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import ucs.gpa.core.calc.Classifier
import ucs.gpa.core.calc.GpaSummary
import ucs.gpa.core.format.Format
import ucs.gpa.core.rules.BoundaryPolicy

class ClassifierTest {

    /** A summary whose GPA is exactly [gpaTimes10000] / 10000 (10 000 credits, weighted in hundredths). */
    private fun exact(gpaTimes10000: Long) = GpaSummary(10_000, gpaTimes10000 * 100, 10_000, 0, 0, 1, 1)

    @Test fun `official boundaries are inclusive at the lower end`() {
        assertEquals("distinction", Classifier.classify(exact(37500), rules)!!.id)
        assertEquals("merit", Classifier.classify(exact(37499), rules)!!.id)
        assertEquals("merit", Classifier.classify(exact(32500), rules)!!.id)
        assertEquals("credit", Classifier.classify(exact(32499), rules)!!.id)
        assertEquals("credit", Classifier.classify(exact(26800), rules)!!.id)
        assertEquals("pass", Classifier.classify(exact(26799), rules)!!.id)
        assertEquals("pass", Classifier.classify(exact(0), rules)!!.id)
        assertEquals("distinction", Classifier.classify(exact(50000), rules)!!.id)
    }

    @Test fun `exact policy does not round 3_745 up`() {
        assertEquals("merit", Classifier.classify(exact(37450), rules)!!.id)
    }

    @Test fun `rounding policy rounds half up to two decimals`() {
        val rounding = rules.copy(boundaryPolicy = BoundaryPolicy.ROUND_HALF_UP_TWO_DECIMALS)
        assertEquals("distinction", Classifier.classify(exact(37450), rounding)!!.id)
        assertEquals("merit", Classifier.classify(exact(37449), rounding)!!.id)
        assertEquals("distinction", Classifier.classify(3.745, rounding).id)
        assertEquals("merit", Classifier.classify(3.745, rules).id)
    }

    @Test fun `no credits means no class`() {
        assertNull(Classifier.classify(GpaSummary.EMPTY, rules))
    }

    @Test fun `near boundary is detected and shown with three decimals`() {
        val s = exact(37490)
        val near = Classifier.nearBoundary(s, rules)
        assertNotNull(near)
        assertEquals("distinction", near.band.id)
        assertEquals("3.749", Format.gpa(s.gpa, near))
        assertNull(Classifier.nearBoundary(exact(37000), rules))
        assertNull(Classifier.nearBoundary(exact(37500), rules))
    }

    @Test fun `gpa formatting rounds half up and ignores locale`() {
        assertEquals("4.13", Format.gpa(4.125))
        assertEquals("3.81", Format.gpa(3.8125))
        assertEquals("2.37", Format.gpa(2.37))
        assertEquals("–", Format.gpa(null))
        assertEquals("+0.03", Format.signed(0.0303))
        assertEquals("−0.31", Format.signed(-0.3125))
        assertEquals("−0.32", Format.signed(-0.315))
    }
}
