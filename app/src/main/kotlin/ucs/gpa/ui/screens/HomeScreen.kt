package ucs.gpa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ucs.gpa.core.calc.Classifier
import ucs.gpa.core.calc.Severity
import ucs.gpa.core.calc.YearResult
import ucs.gpa.core.format.Format
import ucs.gpa.core.plan.Feasibility
import ucs.gpa.ui.ScreenContext
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.BandChip
import ucs.gpa.ui.components.Banner
import ucs.gpa.ui.components.BannerKind
import ucs.gpa.ui.components.BrandMark
import ucs.gpa.ui.components.ChartLegend
import ucs.gpa.ui.components.ClassScale
import ucs.gpa.ui.components.Dot
import ucs.gpa.ui.components.Hairline
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.ProgressTrack
import ucs.gpa.ui.components.SectionHeader
import ucs.gpa.ui.components.Stat
import ucs.gpa.ui.components.TrendChart
import ucs.gpa.ui.components.TrendPoint
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.components.bandColor
import ucs.gpa.ui.nav.Route
import ucs.gpa.ui.theme.Radii
import ucs.gpa.ui.theme.Spacing
import ucs.gpa.ui.theme.Ucs

@Composable
fun HomeScreen(ctx: ScreenContext) {
    val d = ctx.derived
    val result = d.result
    val profile = ctx.record.profile
    ScreenList(
        topBar = {
            Row(
                Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 640.dp).fillMaxWidth().heightIn(min = 60.dp).padding(start = Spacing.screen, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandMark(26.dp)
                Column(Modifier.weight(1f)) {
                    Text("GPA Planner", style = Ucs.type.title, color = Ucs.colors.ink)
                    Text("UCS · University of Zambia", style = Ucs.type.caption, color = Ucs.colors.ink3)
                }
                IconButton(onClick = { ctx.nav.navigate(Route.Settings) }, modifier = Modifier.testTag("open-settings")) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Ucs.colors.ink2)
                }
            }
        },
    ) {
        if (ctx.state.isDemo) item { DemoBanner(onStartOwn = { ctx.nav.navigate(Route.Setup) }) }

        item {
            Column(Modifier.padding(top = 4.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(profile.name.ifBlank { "Your record" }, style = Ucs.type.screenTitle, color = Ucs.colors.ink)
                Text(profileLine(profile), style = Ucs.type.bodySmall, color = Ucs.colors.ink2)
            }
        }

        item { StandingCard(ctx) }

        val problems = result.issues.filter { it.severity != Severity.INFO }
        if (problems.isNotEmpty()) {
            item {
                val first = problems.first()
                Banner(
                    title = if (problems.size == 1) "1 course needs attention" else "${problems.size} courses need attention",
                    text = first.message,
                    kind = if (problems.any { it.severity == Severity.ERROR }) BannerKind.ERROR else BannerKind.WARNING,
                    action = "Review",
                    onAction = { first.yearId?.let { ctx.nav.openYear(it) } ?: ctx.nav.selectTab(Route.Calculator) },
                )
            }
        }
        result.gpaIfTemporaryCountedAsZero?.let { ifZero ->
            item {
                Banner(
                    text = "Courses with a temporary grade are left out until resolved. If they counted as 0.00, your GPA would be ${Format.gpa(ifZero)}.",
                )
            }
        }

        item { TrendCard(ctx) }
        item { YearsCard(ctx) }
        item { TargetSummary(ctx) }
    }
}

@Composable
private fun StandingCard(ctx: ScreenContext) {
    val c = Ucs.colors
    val result = ctx.derived.result
    val rules = ctx.rules
    val gpa = result.gpa
    val band = result.band
    val profile = ctx.record.profile
    val programmeCredits = profile.programmeYears * rules.creditsPerYear
    val earned = result.overall.earnedCredits
    val completedCourses = result.years.sumOf { y -> y.courses.count { it.isPassed } }

    val marginText = when {
        gpa == null -> "Enter your grades to see your GPA and class."
        band == null -> ""
        else -> {
            val next = Classifier.nextBand(band, rules)
            if (next == null) "${Format.decimals(gpa - band.minGpa, 2)} above the ${band.name} line"
            else "${Format.decimals(next.minGpa - gpa, 2)} below ${next.name} (${Format.decimals(next.minGpa, 2)})"
        }
    }

    Column(
        Modifier.fillMaxWidth().clip(Radii.card).background(c.hero).padding(Spacing.lg).testTag("standing-card"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Cumulative GPA", color = c.onHeroMuted, modifier = Modifier.weight(1f))
            Text("All years", style = Ucs.type.caption, color = c.onHeroMuted)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AnimatedGpa(gpa, Ucs.type.heroNumber, c.onHero, near = result.nearBoundary, modifier = Modifier.testTag("cumulative-gpa"))
            Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BandChip(band, onDark = true)
            }
        }
        if (marginText.isNotEmpty()) Text(marginText, style = Ucs.type.bodySmall, color = c.onHeroMuted)
        val provisional = result.hasErrors || result.overall.temporaryCreditsExcluded > 0
        if (gpa != null && provisional) {
            Text(
                if (result.hasErrors) "Provisional: some courses are missing information and are left out."
                else "Provisional: courses with a temporary grade are left out until resolved.",
                style = Ucs.type.caption,
                color = c.accent,
                modifier = Modifier.testTag("provisional"),
            )
        }
        result.nearBoundary?.let {
            Text(
                "Within 0.005 of ${it.band.name}. UNZA does not publish how GPAs are rounded at a boundary.",
                style = Ucs.type.caption,
                color = c.accent,
            )
        }
        ClassScale(gpa, rules, onDark = true, projected = ctx.derived.projection?.gpa)
        Hairline(color = c.heroLine)
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Stat("Credits earned", "$earned", sub = "/ $programmeCredits", onDark = true, modifier = Modifier.weight(1f), description = "$earned of $programmeCredits credits earned")
            Stat("Courses passed", "$completedCourses", onDark = true, modifier = Modifier.weight(1f))
            Stat("Year of study", "${profile.currentYearOfStudy}", sub = "of ${profile.programmeYears}", onDark = true, modifier = Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProgressTrack(
                fraction = if (programmeCredits > 0) earned.toFloat() / programmeCredits else 0f,
                color = c.brand,
                track = c.heroRaised,
                height = 5.dp,
            )
            Text(
                "${if (programmeCredits > 0) earned * 100 / programmeCredits else 0}% of a typical ${programmeCredits}-credit degree",
                style = Ucs.type.caption,
                color = c.onHeroMuted,
            )
        }
    }
}

@Composable
private fun TrendCard(ctx: ScreenContext) {
    val result = ctx.derived.result
    val points = result.years.mapIndexed { i, y ->
        TrendPoint(
            label = "Year ${y.year.yearOfStudy}",
            yearGpa = y.gpa,
            cumulative = if (y.hasGrades) result.cumulativeAfterYear[i] else null,
        )
    }
    UcsCard {
        SectionHeader("Performance by year")
        if (points.count { it.yearGpa != null } == 0) {
            Text("Your trend appears once a year has grades.", style = Ucs.type.bodySmall, color = Ucs.colors.ink2, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            Spacer(Modifier.height(8.dp))
            TrendChart(points, ctx.rules)
            Spacer(Modifier.height(6.dp))
            ChartLegend()
        }
    }
}

@Composable
private fun YearsCard(ctx: ScreenContext) {
    val years = ctx.derived.result.years
    UcsCard(padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Column(Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = 4.dp)) {
            SectionHeader("Year by year", action = "Calculator", onAction = { ctx.nav.selectTab(Route.Calculator) })
        }
        if (years.isEmpty()) {
            Text("No years yet. Add one in the calculator.", style = Ucs.type.bodySmall, color = Ucs.colors.ink2, modifier = Modifier.padding(Spacing.md))
        }
        years.forEachIndexed { i, y ->
            if (i > 0) Hairline(Modifier.padding(horizontal = Spacing.md))
            YearRow(y, ctx.record.profile.currentYearOfStudy, onClick = { ctx.nav.openYear(y.year.id) })
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun YearRow(y: YearResult, currentYear: Int, onClick: () -> Unit) {
    val c = Ucs.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = Spacing.md, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${y.year.label}, ${y.gpa?.let { "GPA ${Format.gpa(it)}" } ?: "no GPA yet"}, ${yearStatus(y)}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(y.year.label + (y.year.session?.let { "  ·  $it" } ?: ""), style = Ucs.type.label, color = c.ink)
            Text(yearStatus(y), style = Ucs.type.caption, color = c.ink3)
        }
        if (y.gpa != null) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Format.gpa(y.gpa), style = Ucs.type.mono.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Dot(bandColor(y.band, c), 6.dp)
                    Text(y.band?.name ?: "", style = Ucs.type.caption, color = c.ink3)
                }
            }
        } else {
            Text(
                when {
                    y.courses.isEmpty() -> "Empty"
                    y.year.yearOfStudy > currentYear -> "Planned"
                    else -> "In progress"
                },
                style = Ucs.type.caption,
                color = c.ink3,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.ink3, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TargetSummary(ctx: ScreenContext) {
    val c = Ucs.colors
    val a = ctx.derived.analysis
    val target = a.target.label
    val text = buildAnnotatedString {
        when (a.feasibility) {
            Feasibility.ACHIEVABLE, Feasibility.SECURED_IF_PASSED -> {
                append("To graduate with $target, average ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.ink)) { append(Format.gpa(a.requiredAverage)) }
                append(" or better over your remaining ${a.remainingCredits} credits.")
            }
            Feasibility.SECURED -> append("$target is secured on GPA alone, whatever your remaining grades.")
            Feasibility.NOT_ACHIEVABLE -> append("$target is out of reach: A+ in every remaining course would give ${Format.gpa(a.bestPossible)}.")
            Feasibility.MET -> append("You have reached $target and no courses remain.")
            Feasibility.MISSED -> append("No courses remain, and your final GPA is below $target.")
            Feasibility.NEEDS_DATA -> append("Add your courses to see what you need for $target.")
        }
    }
    val style = feasibilityStyle(a.feasibility, c)
    UcsCard(onClick = { ctx.nav.selectTab(Route.Planner) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Your target", modifier = Modifier.weight(1f))
            ucs.gpa.ui.components.Pill(style.label, style.container, style.content)
        }
        Spacer(Modifier.height(8.dp))
        Text(text, style = Ucs.type.body, color = c.ink2)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Open the planner", style = Ucs.type.labelSmall, color = c.primary)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.primary, modifier = Modifier.size(16.dp))
        }
    }
}
