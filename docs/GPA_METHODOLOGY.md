# GPA methodology

This is how UCS GPA Planner calculates GPAs and classes, where each rule comes from, and what is
still unconfirmed. Every rule lives in one file:
`core/src/main/kotlin/ucs/gpa/core/rules/UnzaRules.kt`. Nothing else in the app hardcodes a grade
value, a credit value or a class boundary.

## Source

**UNZA Quality Assurance Directorate, "Credit Accumulation, Grade Point Average and Degree
Classification"** (published on unza.zm, 2019). This document is the authority for everything
marked *Official* below. The table shared by UNZANASA ("Proposed GPA range for UNZA") was a
proposal, and its gap between 2.25 and 2.67 does not exist in the official bands.

## Official rules

| Rule | Value |
|---|---|
| Grade points | A+ 5.00, A 4.00, B+ 3.50, B 3.00, C+ 2.37, C 1.00, S 1.00; D+, D, LT, NE 0.00 (fail); DF, WP, IN 0.00 (temporary) |
| Credits | Full course 30, half course 15. About 120 per year, 480 for a four-year degree. Some courses carry other values. |
| Formula | GPA = Σ(course credits × GPV) ÷ total course credits, per academic year and over the whole programme |
| Classification | From the overall GPA of all courses in the programme (every year): Distinction ≥ 3.75, Merit 3.25–3.74, Credit 2.68–3.24, Pass < 2.68 |
| Credits earned | Only for courses passed |
| Repeats | "The grade point value for a repeated course is equal to zero"; it "does not contribute to the GPA and any classification"; its credits are earned if passed |

Using 30 and 15 credits gives exactly the same GPA as weights of 1 and 0.5, because only the ratio
matters. The app uses the official units so its numbers match the university's (120 credits a year).

## How the app treats each case

| Situation | Treatment | Status |
|---|---|---|
| Graded course | credits × GPV, included | Official |
| Failed course (D+, D, LT, NE) | Included with 0.00 points; credits count in the divisor but are not "earned" | Divisor: *Inferred* |
| S (satisfactory) | 1.00, as the official table says | Official |
| Industrial Training (CSC 3009) | Non-credit (0 credits), so it does not affect the GPA | *Inferred* (see below) |
| No grade yet | Not counted. Treated as a remaining course by the planner | App behaviour |
| Temporary grade (DF, WP, IN) | Left out until resolved, flagged, and the GPA "if counted as 0.00" is shown | *Assumption* (configurable) |
| Repeat attempt | Left out of the GPA; the original attempt stays | *Assumption* about the divisor (configurable) |
| Marked "not counted" | Left out, shown as not counted | App behaviour |
| Missing or invalid credits, unknown grade | Left out, reported as an error, GPA marked *Provisional* | App behaviour |
| Nothing graded | No GPA is shown ("–"), never 0.00 | App behaviour |
| Cumulative GPA | Total points ÷ total credits across all years, never an average of yearly GPAs | Official |
| Class boundary | Exact comparison: 3.749 is Merit. Within 0.005 of a boundary the GPA is shown to three decimals with a note | *Assumption* (configurable) |

### Why Industrial Training is treated as non-credit

A real SIS transcript reviewed during development (graduated with "Distinction") had an S for
Industrial Training in the final year. Counting that S as 1.00 on a 15-credit course would have
put the overall GPA below 3.75, which contradicts the class actually awarded. The class only matches
if the course carries no credits. That is evidence, not a published rule, so it is marked *Inferred*
and the course can be edited.

## Configurable policies

`AcademicRules` exposes the uncertain points as values that can be changed in one place:

- `temporaryGradePolicy`: `EXCLUDE_AND_FLAG` (default) or `COUNT_AS_ZERO`
- `repeatAttemptPolicy`: `EXCLUDE` (default) or `COUNT_AS_ZERO`
- `boundaryPolicy`: `EXACT` (default) or `ROUND_HALF_UP_TWO_DECIMALS`
- `fullCourseCredits`, `halfCourseCredits`, `creditsPerYear`, `bands`, `grades`

Each policy has unit tests for both settings (`core/src/test`).

## Open questions for UCS to confirm with the School or Academic Office

1. **Temporary grades:** while a DF, WP or IN is outstanding, does SIS count the course's credits
   in the divisor (pulling the GPA down) or leave the course out?
2. **Repeats:** when a failed course is repeated, does the original failed attempt stay in the
   divisor, and does the repeat's credit total enter it?
3. **Rounding:** is the overall GPA rounded (and how) before it is compared with 3.75, 3.25 and 2.68?
4. **Non-credit courses:** is CSC 3009 Industrial Training officially 0 credits? Are there others?
5. **Course list:** the built-in Computer Science list (Software Engineering stream) and its
   full/half designations come from student transcripts. An official list per stream would let the
   app offer accurate templates for every Computer Science student.

## Planner arithmetic

- Remaining credits: every credit-bearing, counted course without a final grade (including ones
  with a temporary grade). If none are entered, a student-supplied estimate can be used.
- Required average = (target × (completed credits + remaining credits) − completed points) ÷ remaining credits.
- Feasibility: *Secured* (≤ 0), *Secured if you pass* (≤ 1.00, the lowest pass), *Achievable*
  (≤ 5.00), *Out of reach* (> 5.00). Impossible targets are never shown as achievable.
- Suggested combinations use only pass grades (A+ … C) and are checked to reach the target exactly
  (integer arithmetic). "Lean on full courses" drops the other courses by at most one grade step.
- What-if projections include only courses that have a hypothetical grade and say how many are
  left out. Hypothetical grades are stored separately and never change real results.

All arithmetic is done in integer hundredths of a grade point (A+ = 500, C+ = 237), so there is no
floating-point drift in totals or class comparisons.
