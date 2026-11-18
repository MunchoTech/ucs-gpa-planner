package ucs.gpa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ucs.gpa.core.format.Format
import ucs.gpa.core.model.PlannerTarget
import ucs.gpa.core.plan.Feasibility
import ucs.gpa.core.plan.GradeCombination
import ucs.gpa.core.plan.RemainingCourse
import ucs.gpa.core.plan.TargetAnalysis
import ucs.gpa.core.plan.TargetPlanner
import ucs.gpa.ui.ScreenContext
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.BandChip
import ucs.gpa.ui.components.Banner
import ucs.gpa.ui.components.ClassScale
import ucs.gpa.ui.components.ClickableText
import ucs.gpa.ui.components.EmptyState
import ucs.gpa.ui.components.GradeSegments
import ucs.gpa.ui.components.Hairline
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.Pill
import ucs.gpa.ui.components.PrimaryButton
import ucs.gpa.ui.components.ProgressTrack
import ucs.gpa.ui.components.SectionHeader
import ucs.gpa.ui.components.SelectChip
import ucs.gpa.ui.components.Stat
import ucs.gpa.ui.components.TopBar
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.nav.Route
import ucs.gpa.ui.theme.Ucs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(ctx: ScreenContext) {
    val d = ctx.derived
    val a = d.analysis
    val rules = ctx.rules
    val record = ctx.record
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var customText by remember { mutableStateOf(record.target.customGpa?.let { Format.decimals(it, 2) } ?: "") }
    var customOpen by remember { mutableStateOf(record.target.bandId == null) }
    val isCustom = record.target.bandId == null

    ScreenList(
        state = listState,
        topBar = { TopBar("Planner", subtitle = "What you need for the class you want") },
    ) {
        if (ctx.state.isDemo) item { DemoBanner(onStartOwn = { ctx.nav.navigate(Route.Setup) }) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Overline("Target")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("targets")) {
                    rules.targetBands.forEach { b ->
                        SelectChip(
                            b.name,
                            selected = record.target.bandId == b.id,
                            onClick = { customOpen = false; ctx.store.setTarget(record.target.copy(bandId = b.id, customGpa = null)) },
                            sub = Format.decimals(b.minGpa, 2),
                            modifier = Modifier.testTag("target-${b.id}"),
                        )
                    }
                    SelectChip(
                        "Custom",
                        selected = isCustom || customOpen,
                        onClick = { customOpen = true },
                        sub = if (isCustom) Format.decimals(a.target.gpa, 2) else null,
                        modifier = Modifier.testTag("target-custom"),
                    )
                }
                if (customOpen) {
                    val value = customText.toDoubleOrNull()
                    val max = rules.maxPointsHundredths / 100.0
                    val error = when {
                        customText.isBlank() -> null
                        value == null -> "Enter a number such as 3.50."
                        value <= 0 || value > max -> "Enter a GPA between 0.01 and ${Format.decimals(max, 2)}."
                        else -> null
                    }
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field(
                            value = customText,
                            onChange = { v -> customText = v.filter { it.isDigit() || it == '.' }.take(4) },
                            label = "Target GPA",
                            placeholder = "3.50",
                            keyboard = KeyboardType.Decimal,
                            error = error,
                            mono = true,
                            modifier = Modifier.weight(1f),
                            tag = "custom-gpa",
                        )
                        PrimaryButton(
                            "Apply",
                            onClick = { ctx.store.setTarget(record.target.copy(bandId = null, customGpa = value)) },
                            enabled = value != null && error == null,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item { RequirementCard(ctx, a) }

        if (a.remaining.isEmpty()) {
            item { NoRemaining(ctx, a) }
            return@ScreenList
        }

        if (d.combinations.isNotEmpty()) {
            item { SectionHeader("Ways to get there") }
            d.combinations.forEach { combo ->
                item(key = "combo-${combo.kind}") {
                    CombinationCard(ctx, combo, a, onTry = {
                        ctx.store.applyPlannedGrades(combo.assignments)
                        scope.launch { listState.animateScrollToItem(whatIfIndex(ctx)) }
                    })
                }
            }
        }

        item(key = "what-if") {
            SectionHeader(
                "What if",
                action = if (d.projection != null) "Clear all" else null,
                onAction = { ctx.store.clearPlannedGrades() },
            )
        }
        item { ProjectionCard(ctx) }
        item { QuickSet(ctx, a.remaining) }

        a.remaining.groupBy { it.yearOfStudy }.toSortedMap().forEach { (yos, courses) ->
            item(key = "wy-$yos") {
                UcsCard {
                    Overline("Year $yos · ${courses.sumOf { it.credits }} credits")
                    Spacer(Modifier.height(8.dp))
                    courses.forEachIndexed { i, rc ->
                        if (i > 0) Hairline(Modifier.padding(vertical = 10.dp))
                        WhatIfRow(ctx, rc)
                    }
                }
            }
        }
        item {
            Text(
                "Hypothetical grades are kept for planning only. They never change your real results.",
                style = Ucs.type.caption,
                color = Ucs.colors.ink3,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun whatIfIndex(ctx: ScreenContext): Int {
    // demo banner + target + requirement + header + combinations, then "What if"
    val base = (if (ctx.state.isDemo) 1 else 0) + 2
    val combos = ctx.derived.combinations.size
    return base + (if (combos > 0) 1 + combos else 0)
}

@Composable
private fun RequirementCard(ctx: ScreenContext, a: TargetAnalysis) {
    val c = Ucs.colors
    val style = feasibilityStyle(a.feasibility, c)
    val current = a.currentGpa
    UcsCard(modifier = Modifier.testTag("requirement")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline(if (a.remainingCredits > 0) "Average needed in remaining courses" else "Result", modifier = Modifier.weight(1f))
            Pill(style.label, style.container, style.content, modifier = Modifier.testTag("feasibility"))
        }
        Spacer(Modifier.height(6.dp))
        val big = when (a.feasibility) {
            Feasibility.ACHIEVABLE, Feasibility.SECURED_IF_PASSED -> Format.gpa(a.requiredAverage)
            Feasibility.SECURED -> "Secured"
            Feasibility.NOT_ACHIEVABLE -> Format.gpa(a.requiredAverage)
            Feasibility.MET, Feasibility.MISSED -> Format.gpa(current)
            Feasibility.NEEDS_DATA -> "–"
        }
        Text(big, style = Ucs.type.bigNumber, color = if (a.feasibility == Feasibility.NOT_ACHIEVABLE) c.danger else c.ink, modifier = Modifier.testTag("required"))
        Spacer(Modifier.height(4.dp))
        Text(explanation(a, ctx), style = Ucs.type.bodySmall, color = c.ink2)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Stat("Current GPA", Format.gpa(current), modifier = Modifier.weight(1f))
            Stat("Completed", "${a.current.countedCredits}", sub = "cr", modifier = Modifier.weight(1f))
            Stat(if (a.isEstimate) "Remaining (est.)" else "Remaining", "${a.remainingCredits}", sub = "cr", modifier = Modifier.weight(1f))
        }
        val total = a.totalCredits
        if (total > 0) {
            Spacer(Modifier.height(12.dp))
            ProgressTrack(a.current.countedCredits.toFloat() / total)
            Text("${a.current.countedCredits * 100 / total}% of planned credits completed", style = Ucs.type.caption, color = c.ink3)
        }
        if (current != null || ctx.derived.projection != null) {
            Spacer(Modifier.height(14.dp))
            ClassScale(current, ctx.rules, projected = ctx.derived.projection?.gpa)
        }
        if (a.feasibility == Feasibility.NOT_ACHIEVABLE) {
            val best = TargetPlanner.bestReachableBand({ t -> ctx.derived.bandAnalyses[t.band?.id] ?: a }, ctx.rules)
            Spacer(Modifier.height(12.dp))
            Banner(
                text = "A+ in every remaining course would finish at ${Format.gpa(a.bestPossible)}." +
                    (best?.let { " ${it.name} is still within reach." } ?: " No higher class is reachable."),
                action = best?.let { "Aim for ${it.name}" },
                onAction = best?.let { b -> { ctx.store.setTarget(ctx.record.target.copy(bandId = b.id, customGpa = null)) } },
            )
        }
    }
}

private fun explanation(a: TargetAnalysis, ctx: ScreenContext): String {
    val t = a.target.label
    val cur = a.currentGpa
    return when (a.feasibility) {
        Feasibility.ACHIEVABLE -> {
            val vs = cur?.let { if (a.requiredAverage!! > it + 0.005) "higher than your ${Format.gpa(it)} so far" else "at or below your ${Format.gpa(it)} so far" }
            "To finish at ${Format.decimals(a.target.gpa, 2)} or better ($t), average ${Format.gpa(a.requiredAverage)} across your remaining ${a.remainingCredits} credits" + (vs?.let { ", $it." } ?: ".")
        }
        Feasibility.SECURED_IF_PASSED -> "Passing every remaining course (C or better) keeps you at $t."
        Feasibility.SECURED -> "Your results so far keep you at $t on GPA alone, whatever you score in the remaining courses. You still need to pass them to graduate."
        Feasibility.NOT_ACHIEVABLE -> "You would need an average above the top grade (${Format.decimals(ctx.rules.maxPointsHundredths / 100.0, 2)}), so $t cannot be reached."
        Feasibility.MET -> "All entered courses are graded and you are at $t."
        Feasibility.MISSED -> "All entered courses are graded and your GPA is below $t."
        Feasibility.NEEDS_DATA -> "Enter your grades and upcoming courses in the calculator to plan."
    }
}

@Composable
private fun NoRemaining(ctx: ScreenContext, a: TargetAnalysis) {
    val profile = ctx.record.profile
    val yearsEntered = ctx.record.years.count { it.courses.isNotEmpty() }
    val suggestion = ((profile.programmeYears - yearsEntered).coerceAtLeast(1)) * ctx.rules.creditsPerYear
    var text by remember { mutableStateOf(ctx.record.target.estimatedRemainingCredits?.toString() ?: "") }
    UcsCard {
        EmptyState(
            Icons.AutoMirrored.Outlined.EventNote,
            "No upcoming courses",
            "Add your remaining courses in the calculator and leave their grade empty. Or plan with an estimate of the credits you still have to take (about ${ctx.rules.creditsPerYear} a year).",
            primary = "Open calculator" to { ctx.nav.selectTab(Route.Calculator) },
        )
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(
                value = text,
                onChange = { v -> text = v.filter { it.isDigit() }.take(3) },
                label = "Credits remaining",
                placeholder = "$suggestion",
                keyboard = KeyboardType.Number,
                mono = true,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                "Estimate",
                onClick = { ctx.store.setTarget(ctx.record.target.copy(estimatedRemainingCredits = text.toIntOrNull()?.takeIf { it > 0 })) },
                enabled = (text.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun CombinationCard(ctx: ScreenContext, combo: GradeCombination, a: TargetAnalysis, onTry: () -> Unit) {
    val c = Ucs.colors
    val names = a.remaining.associate { it.course.id to it.course.shortTitle }
    UcsCard(modifier = Modifier.testTag("combo-${combo.kind}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(combinationTitle(combo.kind), style = Ucs.type.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
            if (combo.demanding) Pill("Needs A+", c.warningSoft, c.warning, modifier = Modifier.padding(end = 8.dp))
            Text("→ ", style = Ucs.type.mono, color = c.ink3)
            Text(Format.gpa(combo.finalGpa), style = Ucs.type.mono.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            combo.groups.forEach { g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.grade, style = Ucs.type.titleSmall, color = c.ink, modifier = Modifier.width(36.dp))
                    val parts = buildList {
                        if (g.fullCourses > 0) add(plural(g.fullCourses, "full course"))
                        if (g.halfCourses > 0) add(plural(g.halfCourses, "half course"))
                        if (g.otherCourses > 0) add(plural(g.otherCourses, "other course"))
                    }.joinToString(", ")
                    val detail = if (g.courseIds.size <= 2) g.courseIds.joinToString(", ") { names[it] ?: "" } else parts
                    Text(detail, style = Ucs.type.bodySmall, color = c.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        ClickableText("Try this in What if", onTry)
    }
}

@Composable
private fun ProjectionCard(ctx: ScreenContext) {
    val c = Ucs.colors
    val p = ctx.derived.projection
    val a = ctx.derived.analysis
    UcsCard(borderColor = if (p != null) c.accent else c.line, modifier = Modifier.testTag("projection")) {
        Overline("Projected final GPA")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedGpa(p?.gpa, Ucs.type.bigNumber, c.ink, modifier = Modifier.testTag("projected-gpa"))
            if (p?.band != null) BandChip(p.band)
            if (p?.meetsTarget != null) {
                Pill(
                    if (p.meetsTarget == true) "Meets target" else "Below target",
                    if (p.meetsTarget == true) c.positiveSoft else c.surfaceSubtle,
                    if (p.meetsTarget == true) c.positive else c.ink2,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                p == null -> "Pick a grade for any remaining course below, or try one of the combinations above."
                p.isComplete -> "Every remaining course has a hypothetical grade."
                else -> "Based on ${p.assignedCount} of ${a.remaining.size} remaining courses. The other ${p.unassignedCredits} credits are not included until you pick a grade."
            },
            style = Ucs.type.caption,
            color = if (p != null && !p.isComplete) c.warning else c.ink3,
        )
    }
}

@Composable
private fun QuickSet(ctx: ScreenContext, remaining: List<RemainingCourse>) {
    val rules = ctx.rules
    val full = remaining.filter { it.credits == rules.fullCourseCredits }
    val other = remaining.filter { it.credits != rules.fullCourseCredits }
    fun common(list: List<RemainingCourse>) = list.map { it.course.plannedGrade }.distinct().singleOrNull()
    UcsCard {
        Overline("Set several at once")
        Spacer(Modifier.height(10.dp))
        if (full.isNotEmpty()) {
            Text("All full courses (${full.size})", style = Ucs.type.labelSmall, color = Ucs.colors.ink2)
            Spacer(Modifier.height(6.dp))
            GradeSegments(rules.planningGrades, common(full), onSelect = { g -> ctx.store.applyPlannedGradesOrClear(full, g) }, modifier = Modifier.testTag("set-full"))
            Spacer(Modifier.height(12.dp))
        }
        if (other.isNotEmpty()) {
            Text(if (full.isEmpty()) "All courses (${other.size})" else "All half courses (${other.size})", style = Ucs.type.labelSmall, color = Ucs.colors.ink2)
            Spacer(Modifier.height(6.dp))
            GradeSegments(rules.planningGrades, common(other), onSelect = { g -> ctx.store.applyPlannedGradesOrClear(other, g) }, modifier = Modifier.testTag("set-half"))
        }
    }
}

private fun ucs.gpa.data.AppStore.applyPlannedGradesOrClear(courses: List<RemainingCourse>, grade: String?) {
    if (grade == null) courses.forEach { setPlannedGrade(it.course.id, null) }
    else applyPlannedGrades(courses.associate { it.course.id to grade })
}

@Composable
private fun WhatIfRow(ctx: ScreenContext, rc: RemainingCourse) {
    val c = Ucs.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rc.course.shortTitle, style = Ucs.type.label, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (rc.credits == ctx.rules.fullCourseCredits) "Full · ${rc.credits}" else "${rc.credits} cr", style = Ucs.type.caption, color = c.ink3)
        }
        GradeSegments(ctx.rules.planningGrades, rc.course.plannedGrade, onSelect = { ctx.store.setPlannedGrade(rc.course.id, it) })
    }
}
