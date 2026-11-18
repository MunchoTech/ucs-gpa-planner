# Review of the first version

The first version was a single HTML page with inline JavaScript, published as a web page. This is
the assessment that shaped the rebuild.

## What it was

- One HTML file: grade table, weights (Full 1, Half 0.5, Not counted 0), class bands and a
  hardcoded course template inside the page script.
- State was one JavaScript object saved in browser storage; a "backup code" was base64 JSON.
- The planner computed the average needed over ungraded courses and three grade "plans".
- Recommendations included a keyword-based "weakest subject area" guess.
- Sample data was the requesting student's own transcript.

## Retained

- The weighted formula and the per-year and cumulative calculation (verified against two real
  transcripts during the first build, both matched).
- Treating ungraded courses as "remaining" for planning.
- The idea of suggested grade combinations and a what-if projection.

## Corrected

| Issue | Correction |
|---|---|
| S grade was excluded from the GPA | The official table gives S 1.00. Industrial Training is now modelled as a 0-credit course instead (inferred, documented) |
| Pass band was "below 2.68" only by assumption, with a gap in the source table | Confirmed from the official UNZA QA guidelines |
| Weights 1 / 0.5 | Official credits 30 / 15 (same GPA, matches university numbers) |
| Floating-point arithmetic for class comparisons | Exact integer arithmetic in hundredths |
| Boundary rounding was implicit (rounded to 2 dp) | Explicit, configurable policy; three decimals shown near a boundary |
| Temporary grades silently excluded | Excluded, flagged, with the "if counted as 0.00" GPA shown |
| Invalid data could produce a number with no warning | Errors are listed, the GPA is marked Provisional, no GPA is shown when nothing is graded |
| Rules spread across the script | One configurable rules module with provenance for each rule |

## Redesigned

- The whole interface: from one long scrolling page to a four-tab app (Home, Calculator, Planner,
  Insights) plus Settings, with a new UCS design system, typography and charts.
- The calculation is now explained step by step for every year and for the cumulative GPA.
- The planner: target chips including a custom GPA, a feasibility state, honest handling of
  impossible targets, realistic combinations, and per-course hypothetical grades kept separate
  from real grades.
- Storage: app-private file with atomic writes, explicit backup files.

## Discarded

- The personal transcript as sample data, replaced by the fictional student Chanda Mwila.
- The keyword-based "weakest area" recommendation: it guessed at causes from course titles.
  Insights now only state facts from the entered results.
- Generic study tips presented as personalised advice.
- The web page as the delivery format: replaced by a native Android app.
