package ucs.gpa.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import ucs.gpa.core.calc.BoundaryProximity
import ucs.gpa.core.calc.RecordResult
import ucs.gpa.core.calc.YearResult
import ucs.gpa.core.format.Format
import ucs.gpa.core.model.StudentProfile
import ucs.gpa.core.plan.CombinationKind
import ucs.gpa.core.plan.Feasibility
import ucs.gpa.data.DemoData
import ucs.gpa.ui.components.ClickableText
import ucs.gpa.ui.theme.Radii
import ucs.gpa.ui.theme.Ucs
import ucs.gpa.ui.theme.UcsColors

/** A GPA that animates when it changes. Never shows 0.00 for "no data". */
@Composable
fun AnimatedGpa(value: Double?, style: TextStyle, color: Color, modifier: Modifier = Modifier, near: BoundaryProximity? = null) {
    val anim = remember { Animatable((value ?: 0.0).toFloat()) }
    LaunchedEffect(value) {
        if (value != null) anim.animateTo(value.toFloat(), tween(650, easing = FastOutSlowInEasing))
    }
    val shown = if (value == null) "–" else if (kotlin.math.abs(anim.value - value) < 0.0005f) Format.gpa(value, near) else Format.gpa(anim.value.toDouble())
    Text(
        shown,
        style = style,
        color = color,
        modifier = modifier.semantics { contentDescription = value?.let { "GPA ${Format.gpa(it, near)}" } ?: "No GPA yet" },
        maxLines = 1,
    )
}

/** Shown on every main screen while the demo is active. */
@Composable
fun DemoBanner(onStartOwn: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ucs.colors
    Row(
        modifier.fillMaxWidth().clip(Radii.control).background(c.accentSoft).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).testTag("demo-banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Science, null, tint = c.onAccentSoft, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text("Demo record", style = Ucs.type.labelSmall, color = c.onAccentSoft)
            Text("${DemoData.STUDENT_NAME} is a fictional student.", style = Ucs.type.caption, color = c.onAccentSoft)
        }
        ClickableText("Use my own", onStartOwn, color = c.onAccentSoft, modifier = Modifier.padding(end = 6.dp))
    }
}

fun profileLine(profile: StudentProfile): String = buildList {
    add("Year ${profile.currentYearOfStudy} of ${profile.programmeYears}")
    if (profile.programme.isNotBlank()) add(profile.programme)
}.joinToString(" · ")

/** The year the calculator opens on: the current year of study if it has courses, else the latest graded year. */
fun defaultYear(result: RecordResult, profile: StudentProfile): YearResult? =
    result.years.firstOrNull { it.year.yearOfStudy == profile.currentYearOfStudy && it.courses.isNotEmpty() }
        ?: result.years.lastOrNull { it.hasGrades }
        ?: result.years.firstOrNull()

fun yearStatus(y: YearResult): String {
    val graded = y.summary.gradedCourses
    val total = y.courses.size
    return when {
        total == 0 -> "No courses yet"
        y.isComplete -> "$total courses · ${y.summary.countedCredits} credits"
        graded == 0 -> "Not graded · $total courses"
        else -> "$graded of $total graded"
    }
}

data class FeasibilityStyle(val label: String, val container: Color, val content: Color)

fun feasibilityStyle(f: Feasibility, c: UcsColors): FeasibilityStyle = when (f) {
    Feasibility.SECURED -> FeasibilityStyle("Secured", c.positiveSoft, c.positive)
    Feasibility.SECURED_IF_PASSED -> FeasibilityStyle("Secured if you pass", c.positiveSoft, c.positive)
    Feasibility.ACHIEVABLE -> FeasibilityStyle("Achievable", c.primarySoft, c.primary)
    Feasibility.NOT_ACHIEVABLE -> FeasibilityStyle("Out of reach", c.dangerSoft, c.danger)
    Feasibility.MET -> FeasibilityStyle("Reached", c.positiveSoft, c.positive)
    Feasibility.MISSED -> FeasibilityStyle("Not reached", c.dangerSoft, c.danger)
    Feasibility.NEEDS_DATA -> FeasibilityStyle("Needs results", c.surfaceSubtle, c.ink2)
}

fun combinationTitle(kind: CombinationKind): String = when (kind) {
    CombinationKind.SAME_GRADE -> "Same grade everywhere"
    CombinationKind.STRONG_FULL -> "Lean on your full courses"
    CombinationKind.FEWEST_STEPS -> "Fewest grade steps"
}

fun plural(n: Int, one: String, many: String = one + "s") = if (n == 1) "$n $one" else "$n $many"
