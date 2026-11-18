package ucs.gpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ucs.gpa.core.calc.CourseResult
import ucs.gpa.core.calc.CourseStatus
import ucs.gpa.core.calc.GpaSummary
import ucs.gpa.core.calc.YearResult
import ucs.gpa.core.format.Format
import ucs.gpa.ui.ScreenContext
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.BandChip
import ucs.gpa.ui.components.Banner
import ucs.gpa.ui.components.Hairline
import ucs.gpa.ui.components.KeyValueRow
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.TopBar
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.theme.Ucs

/** Shows every step of a GPA calculation, so a student can check it by hand. */
@Composable
fun BreakdownScreen(ctx: ScreenContext, yearId: String?) {
    val result = ctx.derived.result
    val year = yearId?.let { result.year(it) }
    if (yearId != null && year != null) YearBreakdown(ctx, year) else CumulativeBreakdown(ctx)
}

@Composable
private fun YearBreakdown(ctx: ScreenContext, y: YearResult) {
    val counted = y.courses.filter { it.includedInGpa }
    val leftOut = y.courses.filter { !it.includedInGpa }
    ScreenList(topBar = { TopBar("How ${y.year.label} is calculated", onBack = { ctx.nav.back() }) }) {
        item {
            Step(1, "Multiply each course's credits by its grade points") {
                counted.forEachIndexed { i, c ->
                    if (i > 0) Hairline()
                    ProductRow(c)
                }
                if (counted.isEmpty()) Text("No graded courses yet.", style = Ucs.type.bodySmall, color = Ucs.colors.ink2)
            }
        }
        item { Totals(2, y.summary) }
        item { Divide(3, y.summary, y) }
        if (leftOut.isNotEmpty()) item { LeftOut(leftOut) }
        item { PlainLanguage() }
    }
}

@Composable
private fun CumulativeBreakdown(ctx: ScreenContext) {
    val result = ctx.derived.result
    val graded = result.years.filter { it.hasGrades }
    val naive = graded.mapNotNull { it.gpa }.takeIf { it.isNotEmpty() }?.average()
    ScreenList(topBar = { TopBar("How your cumulative GPA is calculated", onBack = { ctx.nav.back() }) }) {
        item {
            Step(1, "Total the grade points and credits for each year") {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text("Year", style = Ucs.type.caption, color = Ucs.colors.ink3, modifier = Modifier.weight(1f))
                    Text("Points", style = Ucs.type.caption, color = Ucs.colors.ink3, modifier = Modifier.weight(1f))
                    Text("Credits", style = Ucs.type.caption, color = Ucs.colors.ink3, modifier = Modifier.weight(0.8f))
                    Text("GPA", style = Ucs.type.caption, color = Ucs.colors.ink3, modifier = Modifier.weight(0.7f))
                }
                Hairline()
                result.years.forEach { y ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(y.year.label, style = Ucs.type.bodySmall, color = Ucs.colors.ink, modifier = Modifier.weight(1f))
                        val none = y.summary.countedCredits == 0
                        Text(if (none) "–" else Format.points(y.summary.weightedHundredths), style = Ucs.type.mono, color = if (none) Ucs.colors.ink3 else Ucs.colors.ink, modifier = Modifier.weight(1f))
                        Text(if (none) "–" else "${y.summary.countedCredits}", style = Ucs.type.mono, color = if (none) Ucs.colors.ink3 else Ucs.colors.ink, modifier = Modifier.weight(0.8f))
                        Text(Format.gpa(y.gpa), style = Ucs.type.mono, color = Ucs.colors.ink2, modifier = Modifier.weight(0.7f))
                    }
                }
            }
        }
        item { Totals(2, result.overall) }
        item {
            Step(3, "Divide total points by total credits") {
                DivisionLine(result.overall)
                Spacer(Modifier.height(8.dp))
                BandChip(result.band)
            }
        }
        if (naive != null && graded.size >= 2) {
            item {
                val sameCredits = graded.map { it.summary.countedCredits }.distinct().size == 1
                Banner(
                    title = "Not an average of yearly GPAs",
                    text = if (sameCredits) "Each year counts by its credits. Your graded years have the same credits, so the plain average (${Format.gpa(naive)}) happens to match. It won't once a year has a different credit total."
                    else "Each year counts by its credits. Averaging your yearly GPAs would give ${Format.gpa(naive)}, which is not how UNZA calculates it.",
                )
            }
        }
        item { PlainLanguage() }
    }
}

@Composable
private fun Step(n: Int, title: String, content: @Composable () -> Unit) {
    UcsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(Ucs.colors.ink), contentAlignment = Alignment.Center) {
                Text("$n", style = Ucs.type.caption.copy(fontWeight = FontWeight.SemiBold), color = Ucs.colors.canvas)
            }
            Text(title, style = Ucs.type.titleSmall, color = Ucs.colors.ink)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ProductRow(c: CourseResult) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(c.course.shortTitle, style = Ucs.type.bodySmall, color = Ucs.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${c.course.code.ifBlank { "" }} ${c.grade?.code ?: ""}".trim(), style = Ucs.type.monoSmall, color = Ucs.colors.ink3)
        }
        Text("${c.credits} × ${Format.decimals(c.grade!!.points, 2)}", style = Ucs.type.monoSmall, color = Ucs.colors.ink2)
        Text(Format.points(c.weightedHundredths), style = Ucs.type.mono, color = Ucs.colors.ink, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun Totals(n: Int, s: GpaSummary) {
    Step(n, "Add them up") {
        KeyValueRow("Total grade points", Format.points(s.weightedHundredths))
        KeyValueRow("Total credits", "${s.countedCredits}")
    }
}

@Composable
private fun Divide(n: Int, s: GpaSummary, y: YearResult) {
    Step(n, "Divide points by credits") {
        DivisionLine(s)
        if (y.hasGrades) {
            Spacer(Modifier.height(8.dp))
            BandChip(y.band)
        }
    }
}

@Composable
private fun DivisionLine(s: GpaSummary) {
    val text = if (s.countedCredits == 0) "No credits counted yet, so there is no GPA."
    else "${Format.points(s.weightedHundredths)} ÷ ${s.countedCredits} = ${Format.decimals(s.gpa!!, 4)}"
    Text(text, style = Ucs.type.mono.copy(fontSize = Ucs.type.number.fontSize * 0.85f), color = Ucs.colors.ink, modifier = Modifier.testTag("division"))
    if (s.countedCredits > 0) Text("Shown as ${Format.gpa(s.gpa)} (two decimals).", style = Ucs.type.caption, color = Ucs.colors.ink3)
}

@Composable
private fun LeftOut(courses: List<CourseResult>) {
    UcsCard {
        Overline("Not counted")
        Spacer(Modifier.height(6.dp))
        courses.forEach { c ->
            val reason = when (c.status) {
                CourseStatus.NOT_GRADED -> "no grade yet"
                CourseStatus.TEMPORARY -> "temporary grade ${c.grade?.code}"
                CourseStatus.EXCLUDED -> if (c.credits == 0) "non-credit course" else "marked as not counted"
                CourseStatus.REPEAT_EXCLUDED -> "repeat attempt"
                CourseStatus.INVALID -> "details need fixing"
                CourseStatus.COUNTED -> ""
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(c.course.shortTitle, style = Ucs.type.bodySmall, color = Ucs.colors.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(reason, style = Ucs.type.caption, color = Ucs.colors.ink3)
            }
        }
    }
}

@Composable
private fun PlainLanguage() {
    UcsCard(color = Ucs.colors.surfaceSubtle, borderColor = Ucs.colors.surfaceSubtle) {
        Overline("In plain words")
        Spacer(Modifier.height(6.dp))
        Text(
            "Think of every course as a box. A full course is a big box (30 credits), a half course a small one (15). " +
                "Your grade says how full each box gets: A is 4 points per credit, B+ is 3.5. " +
                "Add up all the points, add up the sizes of all the boxes, and divide. Bigger boxes count more.",
            style = Ucs.type.bodySmall,
            color = Ucs.colors.ink2,
        )
    }
}
