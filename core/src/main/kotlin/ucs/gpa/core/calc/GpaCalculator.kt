package ucs.gpa.core.calc

import ucs.gpa.core.model.AcademicYear
import ucs.gpa.core.model.Course
import ucs.gpa.core.model.StudentRecord
import ucs.gpa.core.rules.AcademicRules
import ucs.gpa.core.rules.BoundaryPolicy
import ucs.gpa.core.rules.ClassBand
import ucs.gpa.core.rules.GradeDefinition
import ucs.gpa.core.rules.GradeKind
import ucs.gpa.core.rules.RepeatAttemptPolicy
import ucs.gpa.core.rules.TemporaryGradePolicy

enum class CourseStatus {
    /** Graded and included in the GPA. */
    COUNTED,
    /** No grade yet: still to be taken or awaiting results. */
    NOT_GRADED,
    /** Has a temporary grade (DF, WP, IN). */
    TEMPORARY,
    /** Left out on purpose: marked as not counted, or carries 0 credits. */
    EXCLUDED,
    /** A repeated attempt the rules leave out. */
    REPEAT_EXCLUDED,
    /** Information is missing or wrong, so the course cannot be used. */
    INVALID,
}

enum class Severity { INFO, WARNING, ERROR }

enum class IssueCode {
    MISSING_CREDITS,
    INVALID_CREDITS,
    UNKNOWN_GRADE,
    MISSING_NAME,
    DUPLICATE_CODE,
    TEMPORARY_GRADE,
    REPEAT_EXCLUDED,
    NEAR_BOUNDARY,
}

data class Issue(
    val code: IssueCode,
    val severity: Severity,
    val message: String,
    val courseId: String? = null,
    val yearId: String? = null,
)

data class CourseResult(
    val course: Course,
    val yearId: String,
    val status: CourseStatus,
    /** Resolved credit value, or null when the course has invalid credit information. */
    val credits: Int?,
    val grade: GradeDefinition?,
    /** True when the course's credits are part of the GPA divisor. */
    val includedInGpa: Boolean,
    val issues: List<Issue>,
) {
    /** credits × grade points, in hundredths. Zero when the course is not included. */
    val weightedHundredths: Long
        get() = if (includedInGpa && credits != null) credits.toLong() * (grade?.pointsHundredths ?: 0) else 0L

    val isPassed: Boolean get() = includedInGpa && grade?.kind == GradeKind.PASS

    /** Still to be completed or resolved: counts as remaining for planning. */
    val isPending: Boolean
        get() = credits != null && credits > 0 && (status == CourseStatus.NOT_GRADED || (status == CourseStatus.TEMPORARY && !includedInGpa))
}

/** Totals for a set of courses. All arithmetic is exact. */
data class GpaSummary(
    val countedCredits: Int,
    val weightedHundredths: Long,
    val earnedCredits: Int,
    val pendingCredits: Int,
    val temporaryCreditsExcluded: Int,
    val gradedCourses: Int,
    val totalCourses: Int,
) {
    val gpa: Double? get() = if (countedCredits > 0) weightedHundredths / 100.0 / countedCredits else null

    val weightedPoints: Double get() = weightedHundredths / 100.0

    /** True when the GPA is at least [minHundredths] (e.g. 375), compared exactly. */
    fun atLeast(minHundredths: Int): Boolean =
        countedCredits > 0 && weightedHundredths >= minHundredths.toLong() * countedCredits

    operator fun plus(other: GpaSummary) = GpaSummary(
        countedCredits + other.countedCredits,
        weightedHundredths + other.weightedHundredths,
        earnedCredits + other.earnedCredits,
        pendingCredits + other.pendingCredits,
        temporaryCreditsExcluded + other.temporaryCreditsExcluded,
        gradedCourses + other.gradedCourses,
        totalCourses + other.totalCourses,
    )

    companion object {
        val EMPTY = GpaSummary(0, 0, 0, 0, 0, 0, 0)

        fun of(courses: List<CourseResult>): GpaSummary = GpaSummary(
            countedCredits = courses.filter { it.includedInGpa }.sumOf { it.credits ?: 0 },
            weightedHundredths = courses.sumOf { it.weightedHundredths },
            earnedCredits = courses.filter { it.isPassed }.sumOf { it.credits ?: 0 },
            pendingCredits = courses.filter { it.isPending }.sumOf { it.credits ?: 0 },
            temporaryCreditsExcluded = courses.filter { it.status == CourseStatus.TEMPORARY && !it.includedInGpa }.sumOf { it.credits ?: 0 },
            gradedCourses = courses.count { it.course.isGraded },
            totalCourses = courses.size,
        )
    }
}

/** A GPA just below the next class boundary (closer than 0.005). */
data class BoundaryProximity(val band: ClassBand, val gap: Double)

data class YearResult(
    val year: AcademicYear,
    val courses: List<CourseResult>,
    val summary: GpaSummary,
    val band: ClassBand?,
    val issues: List<Issue>,
) {
    val gpa: Double? get() = summary.gpa
    val isEmpty: Boolean get() = courses.isEmpty()
    val isComplete: Boolean get() = courses.isNotEmpty() && summary.pendingCredits == 0 && courses.none { it.status == CourseStatus.INVALID }
    val hasGrades: Boolean get() = summary.countedCredits > 0
}

data class RecordResult(
    val years: List<YearResult>,
    val overall: GpaSummary,
    val band: ClassBand?,
    val issues: List<Issue>,
    /** Cumulative GPA after each year, in year order (null while nothing is graded yet). */
    val cumulativeAfterYear: List<Double?>,
    /** GPA if excluded temporary grades counted as 0.00, when there are any. */
    val gpaIfTemporaryCountedAsZero: Double?,
    val nearBoundary: BoundaryProximity?,
) {
    val gpa: Double? get() = overall.gpa
    val hasErrors: Boolean get() = issues.any { it.severity == Severity.ERROR }
    fun year(id: String): YearResult? = years.firstOrNull { it.year.id == id }
    fun course(id: String): CourseResult? = years.asSequence().flatMap { it.courses }.firstOrNull { it.course.id == id }
}

object Classifier {
    fun classify(summary: GpaSummary, rules: AcademicRules): ClassBand? {
        if (summary.countedCredits <= 0) return null
        return classify(summary.weightedHundredths, summary.countedCredits, rules)
    }

    /** Classifies the GPA weighted/credits (weighted in hundredths) without floating-point error. */
    fun classify(weightedHundredths: Long, credits: Int, rules: AcademicRules): ClassBand {
        require(credits > 0)
        return when (rules.boundaryPolicy) {
            BoundaryPolicy.EXACT ->
                rules.bands.first { weightedHundredths >= it.minHundredths.toLong() * credits }
            BoundaryPolicy.ROUND_HALF_UP_TWO_DECIMALS -> {
                val rounded = Math.floorDiv(2 * weightedHundredths + credits, 2L * credits)
                rules.bands.first { rounded >= it.minHundredths }
            }
        }
    }

    fun classify(gpa: Double, rules: AcademicRules): ClassBand {
        val hundredths = when (rules.boundaryPolicy) {
            BoundaryPolicy.EXACT -> gpa * 100 + 1e-9
            BoundaryPolicy.ROUND_HALF_UP_TWO_DECIMALS -> Math.floor(gpa * 100 + 0.5 + 1e-9)
        }
        return rules.bands.first { hundredths >= it.minHundredths }
    }

    /** The next class up, or null at the top. */
    fun nextBand(band: ClassBand, rules: AcademicRules): ClassBand? {
        val i = rules.bands.indexOfFirst { it.id == band.id }
        return if (i > 0) rules.bands[i - 1] else null
    }

    fun nearBoundary(summary: GpaSummary, rules: AcademicRules): BoundaryProximity? {
        val band = classify(summary, rules) ?: return null
        val next = nextBand(band, rules) ?: return null
        val gpa = summary.gpa ?: return null
        val gap = next.minGpa - gpa
        return if (gap > 0 && gap < 0.005) BoundaryProximity(next, gap) else null
    }
}

object GpaCalculator {

    fun course(course: Course, yearId: String, rules: AcademicRules): CourseResult {
        val issues = mutableListOf<Issue>()
        fun issue(code: IssueCode, severity: Severity, message: String) {
            issues += Issue(code, severity, message, course.id, yearId)
        }

        if (course.code.isBlank() && course.title.isBlank()) {
            issue(IssueCode.MISSING_NAME, Severity.WARNING, "A course has no code or title.")
        }

        val credits = rules.creditsFor(course.load, course.customCredits)
        if (credits == null) {
            val missing = course.customCredits == null
            issue(
                if (missing) IssueCode.MISSING_CREDITS else IssueCode.INVALID_CREDITS,
                Severity.ERROR,
                if (missing) "${course.displayName} has no credit value, so it is left out."
                else "${course.displayName} has ${course.customCredits} credits, which is outside 0–${rules.maxCustomCredits}. It is left out.",
            )
            return CourseResult(course, yearId, CourseStatus.INVALID, null, rules.grade(course.grade), false, issues)
        }

        val grade = rules.grade(course.grade)
        if (course.isGraded && grade == null) {
            issue(IssueCode.UNKNOWN_GRADE, Severity.ERROR, "${course.displayName} has a grade \"${course.grade}\" that UNZA does not use. It is left out until corrected.")
            return CourseResult(course, yearId, CourseStatus.INVALID, credits, null, false, issues)
        }

        if (!course.countsTowardGpa || credits == 0) {
            return CourseResult(course, yearId, CourseStatus.EXCLUDED, credits, grade, false, issues)
        }

        if (course.isRepeat && rules.repeatAttemptPolicy == RepeatAttemptPolicy.EXCLUDE) {
            issue(IssueCode.REPEAT_EXCLUDED, Severity.INFO, "${course.displayName} is a repeat attempt, which does not count towards the GPA.")
            return CourseResult(course, yearId, CourseStatus.REPEAT_EXCLUDED, credits, grade, false, issues)
        }

        if (grade == null) {
            return CourseResult(course, yearId, CourseStatus.NOT_GRADED, credits, null, false, issues)
        }

        if (course.isRepeat) {
            // COUNT_AS_ZERO: credits count, points do not.
            val zero = grade.copy(pointsHundredths = 0)
            return CourseResult(course, yearId, CourseStatus.COUNTED, credits, zero, true, issues)
        }

        if (grade.kind == GradeKind.TEMPORARY) {
            val included = rules.temporaryGradePolicy == TemporaryGradePolicy.COUNT_AS_ZERO
            issue(
                IssueCode.TEMPORARY_GRADE, Severity.WARNING,
                if (included) "${course.displayName} has a temporary grade (${grade.code}), counted as 0.00 until resolved."
                else "${course.displayName} has a temporary grade (${grade.code}) and is left out until it is resolved.",
            )
            return CourseResult(course, yearId, CourseStatus.TEMPORARY, credits, grade, included, issues)
        }

        return CourseResult(course, yearId, CourseStatus.COUNTED, credits, grade, true, issues)
    }

    fun year(year: AcademicYear, rules: AcademicRules): YearResult {
        val courses = year.courses.map { course(it, year.id, rules) }
        val issues = courses.flatMap { it.issues }.toMutableList()

        year.courses
            .filter { it.code.isNotBlank() && !it.isRepeat }
            .groupBy { it.code.trim().uppercase().replace(" ", "") }
            .filter { it.value.size > 1 }
            .forEach { (_, dupes) ->
                issues += Issue(
                    IssueCode.DUPLICATE_CODE, Severity.WARNING,
                    "${dupes.first().code.trim()} appears ${dupes.size} times in ${year.label}. Mark extra attempts as repeats or remove them.",
                    dupes.first().id, year.id,
                )
            }

        val summary = GpaSummary.of(courses)
        return YearResult(year, courses, summary, Classifier.classify(summary, rules), issues)
    }

    fun record(record: StudentRecord, rules: AcademicRules): RecordResult {
        val years = record.sortedYears.map { year(it, rules) }
        val overall = years.fold(GpaSummary.EMPTY) { acc, y -> acc + y.summary }

        var running = GpaSummary.EMPTY
        val cumulative = years.map { y ->
            running += y.summary
            running.gpa
        }

        val issues = years.flatMap { it.issues }.toMutableList()
        val near = Classifier.nearBoundary(overall, rules)
        if (near != null) {
            issues += Issue(
                IssueCode.NEAR_BOUNDARY, Severity.INFO,
                "Your GPA is less than 0.005 below ${near.band.name}. UNZA does not document how GPAs are rounded at class boundaries.",
            )
        }

        val ifZero = if (overall.temporaryCreditsExcluded > 0) {
            val credits = overall.countedCredits + overall.temporaryCreditsExcluded
            overall.weightedHundredths / 100.0 / credits
        } else null

        return RecordResult(years, overall, Classifier.classify(overall, rules), issues, cumulative, ifZero, near)
    }
}
