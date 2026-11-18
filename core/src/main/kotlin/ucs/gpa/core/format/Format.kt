package ucs.gpa.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import ucs.gpa.core.calc.BoundaryProximity

/** Locale-independent number formatting (always a dot, never a comma). */
object Format {
    fun decimals(value: Double, places: Int, mode: RoundingMode = RoundingMode.HALF_UP): String =
        BigDecimal(value.toString()).setScale(places, mode).toPlainString()

    /**
     * A GPA to two decimals. Just below a class boundary two decimals would round up to the boundary
     * (3.749 shows as "3.75" yet is Merit), so three decimals are shown, truncated, instead.
     */
    fun gpa(value: Double?, near: BoundaryProximity? = null): String = when {
        value == null -> "–"
        near != null -> decimals(value, 3, RoundingMode.DOWN)
        else -> decimals(value, 2)
    }

    fun signed(value: Double, places: Int = 2): String {
        val s = decimals(kotlin.math.abs(value), places)
        return when {
            s.trimEnd('0', '.').let { it.isEmpty() || it == "0" } -> "±$s"
            value > 0 -> "+$s"
            else -> "−$s"
        }
    }

    fun points(hundredths: Long): String = decimals(hundredths / 100.0, 2)

    fun credits(credits: Int): String = credits.toString()
}
