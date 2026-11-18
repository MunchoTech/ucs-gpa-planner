package ucs.gpa.core.rules

/**
 * The University of Zambia rules as currently understood.
 *
 * Official source: UNZA Quality Assurance Directorate, "Credit Accumulation, Grade Point Average and
 * Degree Classification" (unza.zm, article files 2019-04). Anything not in that document is marked
 * [Provenance.INFERRED] or [Provenance.ASSUMPTION] in [notes] and exposed as a policy value so it can
 * be changed in one place once confirmed. See docs/GPA_METHODOLOGY.md.
 */
object UnzaRules {

    const val SOURCE = "UNZA Quality Assurance Directorate, \"Credit Accumulation, Grade Point Average and Degree Classification\""

    val grades = listOf(
        GradeDefinition("A+", 500, GradeKind.PASS, "Distinction"),
        GradeDefinition("A", 400, GradeKind.PASS, "Distinction"),
        GradeDefinition("B+", 350, GradeKind.PASS, "Meritorious"),
        GradeDefinition("B", 300, GradeKind.PASS, "Credit"),
        GradeDefinition("C+", 237, GradeKind.PASS, "Credit"),
        GradeDefinition("C", 100, GradeKind.PASS, "Pass"),
        GradeDefinition("S", 100, GradeKind.PASS, "Satisfactory"),
        GradeDefinition("D+", 0, GradeKind.FAIL, "Fail"),
        GradeDefinition("D", 0, GradeKind.FAIL, "Fail"),
        GradeDefinition("LT", 0, GradeKind.FAIL, "Fail (left without permission)"),
        GradeDefinition("NE", 0, GradeKind.FAIL, "Fail (not examined)"),
        GradeDefinition("DF", 0, GradeKind.TEMPORARY, "Deferred examination"),
        GradeDefinition("WP", 0, GradeKind.TEMPORARY, "Left with permission"),
        GradeDefinition("IN", 0, GradeKind.TEMPORARY, "Incomplete assessment"),
    )

    val bands = listOf(
        ClassBand("distinction", "Distinction", 375),
        ClassBand("merit", "Merit", 325),
        ClassBand("credit", "Credit", 268),
        ClassBand("pass", "Pass", 0),
    )

    val notes = listOf(
        RuleNote(
            "grade-points", "Grade-point values",
            "A+ 5.00, A 4.00, B+ 3.50, B 3.00, C+ 2.37, C 1.00, S 1.00. Fail grades (D+, D, LT, NE) and temporary grades (DF, WP, IN) are 0.00.",
            Provenance.OFFICIAL, "$SOURCE, Table 1",
        ),
        RuleNote(
            "formula", "GPA formula",
            "GPA = Σ(course credits × grade-point value) ÷ total course credits. It is worked out for each academic year and over the whole programme.",
            Provenance.OFFICIAL, "$SOURCE, section 3",
        ),
        RuleNote(
            "credits", "Course credits",
            "A full course is 30 credits and a half course 15. A full academic year is generally 120 credits, and a four-year degree 480. Some courses carry other values, so a custom credit value can be entered.",
            Provenance.OFFICIAL, "$SOURCE, section 1",
        ),
        RuleNote(
            "classification", "Degree classification",
            "The degree class comes from the overall GPA of all courses taken in the programme, every year included: Distinction 3.75 and above, Merit 3.25–3.74, Credit 2.68–3.24, Pass below 2.68.",
            Provenance.OFFICIAL, "$SOURCE, section 4 and Table 2",
        ),
        RuleNote(
            "earned-credits", "Credits earned",
            "Credits are earned only for courses passed. Failed courses still count in the GPA with 0.00 points.",
            Provenance.OFFICIAL, "$SOURCE, section 1",
        ),
        RuleNote(
            "failed-credits", "Failed courses in the credit total",
            "The document defines the divisor as the credits a student may accumulate in the period, which the app reads as every credit-bearing course taken, including failed ones.",
            Provenance.INFERRED, "$SOURCE, section 3 (definition of TCs)",
        ),
        RuleNote(
            "industrial-training", "Industrial Training (CSC 3009)",
            "Treated as a non-credit course, so it is left out of the GPA. A real transcript reviewed while building the app was classed Distinction, which is only possible if this course carries no credits.",
            Provenance.INFERRED, "Comparison with an official SIS transcript",
        ),
        RuleNote(
            "temporary", "Temporary grades (DF, WP, IN)",
            "The document gives them 0.00 points but does not say whether their credits count. The app leaves them out until resolved, flags them, and shows what the GPA would be if they counted as 0.00.",
            Provenance.ASSUMPTION, null,
        ),
        RuleNote(
            "repeats", "Repeated courses",
            "The document says a repeated course has a grade-point value of zero and does not contribute to the GPA or the classification; its credits are earned if passed. The app leaves repeat attempts out of the GPA. Whether their credits enter the divisor is not stated.",
            Provenance.ASSUMPTION, "$SOURCE, sections 2 and 4",
        ),
        RuleNote(
            "rounding", "Rounding at class boundaries",
            "The document does not say how a GPA is rounded before classification. The app compares the exact value, so 3.749 is Merit, and warns when a GPA is within 0.005 of the next class.",
            Provenance.ASSUMPTION, null,
        ),
        RuleNote(
            "course-catalogue", "Course list and full/half designations",
            "The built-in Computer Science course list and which courses are full courses come from student transcripts, not an official catalogue. Departments can change them; every course can be edited.",
            Provenance.ASSUMPTION, "Student transcripts (Software Engineering stream)",
        ),
    )

    val current: AcademicRules = AcademicRules(
        id = "unza-2019",
        institution = "University of Zambia",
        revision = "QA Directorate guidelines (published 2019)",
        grades = grades,
        fullCourseCredits = 30,
        halfCourseCredits = 15,
        creditsPerYear = 120,
        bands = bands,
        temporaryGradePolicy = TemporaryGradePolicy.EXCLUDE_AND_FLAG,
        repeatAttemptPolicy = RepeatAttemptPolicy.EXCLUDE,
        boundaryPolicy = BoundaryPolicy.EXACT,
        planningGrades = listOf("A+", "A", "B+", "B", "C+", "C"),
        maxCustomCredits = 120,
        notes = notes,
    )
}
