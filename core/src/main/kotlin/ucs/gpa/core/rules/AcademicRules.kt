package ucs.gpa.core.rules

import ucs.gpa.core.model.CourseLoad

enum class GradeKind { PASS, FAIL, TEMPORARY }

/**
 * A grade and its grade-point value (GPV). Points are stored in hundredths (A+ = 500, C+ = 237)
 * so every calculation is exact integer arithmetic.
 */
data class GradeDefinition(
    val code: String,
    val pointsHundredths: Int,
    val kind: GradeKind,
    val description: String,
) {
    val points: Double get() = pointsHundredths / 100.0
}

/** Where a rule comes from. The app shows this next to each rule. */
enum class Provenance {
    /** Stated in an official UNZA document. */
    OFFICIAL,
    /** Not written down, but implied by official results we checked. */
    INFERRED,
    /** Not documented; a reasonable default the app has to pick. Configurable. */
    ASSUMPTION,
}

data class RuleNote(
    val id: String,
    val title: String,
    val detail: String,
    val provenance: Provenance,
    val source: String? = null,
)

/** A degree class. [minHundredths] is the inclusive lower bound (375 = 3.75). */
data class ClassBand(
    val id: String,
    val name: String,
    val minHundredths: Int,
) {
    val minGpa: Double get() = minHundredths / 100.0
}

/** What to do with temporary grades (DF, WP, IN) whose effect on the credit total is not documented. */
enum class TemporaryGradePolicy {
    /** Leave the course out of the GPA until the grade is resolved, and flag it. */
    EXCLUDE_AND_FLAG,
    /** Count the course's credits with 0.00 points. */
    COUNT_AS_ZERO,
}

/** What to do with a repeated attempt of a course. */
enum class RepeatAttemptPolicy {
    /** The repeat does not contribute to the GPA at all. */
    EXCLUDE,
    /** The repeat's credits count with 0.00 points. */
    COUNT_AS_ZERO,
}

/** How a GPA is compared against class boundaries. */
enum class BoundaryPolicy {
    /** Compare the exact GPA (3.749 is below 3.75). */
    EXACT,
    /** Round to two decimals first, half up (3.745 becomes 3.75). */
    ROUND_HALF_UP_TWO_DECIMALS,
}

/**
 * Every academic rule the engine uses, in one place. Nothing else in the app hardcodes a grade
 * value, a credit value or a class boundary.
 */
data class AcademicRules(
    val id: String,
    val institution: String,
    val revision: String,
    val grades: List<GradeDefinition>,
    val fullCourseCredits: Int,
    val halfCourseCredits: Int,
    val creditsPerYear: Int,
    val bands: List<ClassBand>,
    val temporaryGradePolicy: TemporaryGradePolicy,
    val repeatAttemptPolicy: RepeatAttemptPolicy,
    val boundaryPolicy: BoundaryPolicy,
    /** Grades the planner may suggest, best first. */
    val planningGrades: List<String>,
    val maxCustomCredits: Int,
    val notes: List<RuleNote>,
) {
    init {
        require(grades.map { it.code.uppercase() }.toSet().size == grades.size) { "Grade codes must be unique" }
        require(bands.isNotEmpty()) { "At least one class band is required" }
        require(bands.zipWithNext().all { (a, b) -> a.minHundredths > b.minHundredths }) { "Bands must be sorted from highest to lowest" }
        require(bands.last().minHundredths == 0) { "The lowest band must start at 0" }
        require(fullCourseCredits > 0 && halfCourseCredits > 0)
        require(planningGrades.all { code -> grades.any { it.code == code && it.kind == GradeKind.PASS } })
    }

    fun grade(code: String?): GradeDefinition? {
        val c = code?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return grades.firstOrNull { it.code.equals(c, ignoreCase = true) }
    }

    fun band(id: String?): ClassBand? = bands.firstOrNull { it.id == id }

    /** Bands a student can aim for (everything above the lowest band). */
    val targetBands: List<ClassBand> get() = bands.dropLast(1)

    val maxPointsHundredths: Int get() = grades.maxOf { it.pointsHundredths }

    val minPassPointsHundredths: Int get() = grades.filter { it.kind == GradeKind.PASS }.minOf { it.pointsHundredths }

    val planningGradeDefinitions: List<GradeDefinition> get() = planningGrades.map { grade(it)!! }

    /** Credits for a course, or null when a custom value is missing or out of range. */
    fun creditsFor(load: CourseLoad, customCredits: Int?): Int? = when (load) {
        CourseLoad.FULL -> fullCourseCredits
        CourseLoad.HALF -> halfCourseCredits
        CourseLoad.CUSTOM -> customCredits?.takeIf { it in 0..maxCustomCredits }
    }
}
