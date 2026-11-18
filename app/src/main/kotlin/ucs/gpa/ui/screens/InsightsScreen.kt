package ucs.gpa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ucs.gpa.core.format.Format
import ucs.gpa.core.insight.Insight
import ucs.gpa.core.plan.Feasibility
import ucs.gpa.ui.ScreenContext
import ucs.gpa.ui.ScreenList
import ucs.gpa.ui.components.Dot
import ucs.gpa.ui.components.EmptyState
import ucs.gpa.ui.components.Hairline
import ucs.gpa.ui.components.Overline
import ucs.gpa.ui.components.ProgressTrack
import ucs.gpa.ui.components.TopBar
import ucs.gpa.ui.components.UcsCard
import ucs.gpa.ui.components.ValueBar
import ucs.gpa.ui.components.bandColor
import ucs.gpa.ui.nav.Route
import ucs.gpa.ui.theme.Ucs

@Composable
fun InsightsScreen(ctx: ScreenContext) {
    val insights = ctx.derived.insights
    ScreenList(topBar = { TopBar("Insights", subtitle = "Facts from the results you've entered") }) {
        if (ctx.state.isDemo) item { DemoBanner(onStartOwn = { ctx.nav.navigate(Route.Setup) }) }
        if (insights.isEmpty()) {
            item {
                UcsCard {
                    EmptyState(
                        Icons.Outlined.Insights,
                        "No insights yet",
                        "Insights appear once you've entered grades. They only describe your results; they never guess at reasons.",
                        primary = "Open calculator" to { ctx.nav.selectTab(Route.Calculator) },
                    )
                }
            }
            return@ScreenList
        }
        insights.forEach { insight ->
            item(key = insight.id) {
                when (insight) {
                    is Insight.TargetProgress -> TargetProgressCard(insight)
                    is Insight.StrongestYear -> StrongestYearCard(insight)
                    is Insight.Trend -> TrendCard(insight)
                    is Insight.TopContributors -> ContributorsCard(insight)
                    is Insight.BelowAverage -> BelowAverageCard(insight, ctx)
                    is Insight.GradeDistribution -> DistributionCard(insight)
                    is Insight.CourseWeightInfluence -> InfluenceCard(insight)
                }
            }
        }
        item {
            Text(
                "Insights use only the results in this record. They show what the numbers say, not why.",
                style = Ucs.type.caption,
                color = Ucs.colors.ink3,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun InsightCard(label: String, headline: String, tag: String, content: @Composable () -> Unit = {}) {
    UcsCard(modifier = Modifier.testTag("insight-$tag")) {
        Overline(label)
        Spacer(Modifier.height(6.dp))
        Text(headline, style = Ucs.type.title, color = Ucs.colors.ink)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun TargetProgressCard(i: Insight.TargetProgress) {
    val a = i.analysis
    val c = Ucs.colors
    val cur = a.currentGpa ?: return
    val gap = a.target.gpa - cur
    val headline = if (gap <= 0) "${Format.decimals(-gap, 2)} above ${a.target.label}" else "${Format.decimals(gap, 2)} below ${a.target.label}"
    InsightCard("Progress towards your target", headline, "target") {
        val max = 5f
        ProgressTrack(
            (cur / max).toFloat(),
            marker = (a.target.gpa / max).toFloat(),
            markerColor = c.accent,
            height = 8.dp,
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text("Now ${Format.gpa(cur)}", style = Ucs.type.caption, color = c.ink2, modifier = Modifier.weight(1f))
            Text("Target ${Format.decimals(a.target.gpa, 2)}", style = Ucs.type.caption, color = c.ink2)
        }
        if (a.requiredAverage != null && (a.feasibility == Feasibility.ACHIEVABLE || a.feasibility == Feasibility.SECURED_IF_PASSED)) {
            Spacer(Modifier.height(8.dp))
            Text("Needs an average of ${Format.gpa(a.requiredAverage)} over the remaining ${a.remainingCredits} credits.", style = Ucs.type.bodySmall, color = c.ink2)
        }
    }
}

@Composable
private fun StrongestYearCard(i: Insight.StrongestYear) {
    val c = Ucs.colors
    val headline = if (i.tiedWith.isEmpty()) "${i.best.year.label}, at ${Format.gpa(i.best.gpa)}"
    else "${(listOf(i.best) + i.tiedWith).joinToString(" and ") { it.year.label }}, tied at ${Format.gpa(i.best.gpa)}"
    InsightCard("Strongest year", headline, "strongest") {
        Text("Compared across ${i.compared} graded years.", style = Ucs.type.caption, color = c.ink3)
    }
}

@Composable
private fun TrendCard(i: Insight.Trend) {
    val c = Ucs.colors
    val change = i.change
    val headline = when {
        kotlin.math.abs(change) < 0.005 -> "Steady since ${i.first.first.year.label}"
        change > 0 -> "Up ${Format.decimals(change, 2)} since ${i.first.first.year.label}"
        else -> "Down ${Format.decimals(-change, 2)} since ${i.first.first.year.label}"
    }
    InsightCard("Year GPA trend", headline, "trend") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            i.points.forEachIndexed { idx, (y, gpa) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(y.year.label, style = Ucs.type.bodySmall, color = c.ink2, modifier = Modifier.width(56.dp))
                    ValueBar((gpa / 5.0).toFloat(), modifier = Modifier.weight(1f), color = bandColor(y.band, c))
                    Text(Format.gpa(gpa), style = Ucs.type.mono, color = c.ink, modifier = Modifier.width(40.dp))
                    val delta = if (idx > 0) gpa - i.points[idx - 1].second else null
                    Text(delta?.let { Format.signed(it) } ?: "", style = Ucs.type.monoSmall, color = c.ink3, modifier = Modifier.width(46.dp))
                }
            }
        }
    }
}

@Composable
private fun ContributorsCard(i: Insight.TopContributors) {
    val c = Ucs.colors
    InsightCard("Largest contributions", "Your biggest grade-point earners", "contributors") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            i.courses.forEachIndexed { idx, r ->
                if (idx > 0) Hairline()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(r.course.shortTitle, style = Ucs.type.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.credits} credits × ${r.grade!!.code} (${Format.decimals(r.grade!!.points, 2)})", style = Ucs.type.caption, color = c.ink3)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Format.points(r.weightedHundredths), style = Ucs.type.mono.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                        Text("${r.weightedHundredths * 100 / i.totalWeightedHundredths.coerceAtLeast(1)}% of all points", style = Ucs.type.caption, color = c.ink3)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Full courses earn twice the points of a half course with the same grade.", style = Ucs.type.caption, color = c.ink3)
    }
}

@Composable
private fun BelowAverageCard(i: Insight.BelowAverage, ctx: ScreenContext) {
    val c = Ucs.colors
    val total = ctx.derived.result.overall.countedCredits.coerceAtLeast(1)
    InsightCard("Below your average", "Grades under your ${Format.gpa(i.gpa)}", "below") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            i.courses.forEachIndexed { idx, (r, drag) ->
                if (idx > 0) Hairline()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(r.course.shortTitle, style = Ucs.type.label, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.grade!!.code} · ${r.credits} credits", style = Ucs.type.caption, color = c.ink3)
                    }
                    Text(Format.signed(-drag / total, 3), style = Ucs.type.mono, color = c.ink2)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("How much lower your GPA is than if each of these had matched your average.", style = Ucs.type.caption, color = c.ink3)
    }
}

@Composable
private fun DistributionCard(i: Insight.GradeDistribution) {
    val c = Ucs.colors
    val max = i.counts.maxOfOrNull { it.second } ?: 1
    val top = i.counts.maxByOrNull { it.second }
    InsightCard("Grades", top?.let { "Most common: ${it.first} (${plural(it.second, "course")})" } ?: "", "distribution") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            i.counts.forEach { (grade, count) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(grade, style = Ucs.type.titleSmall, color = c.ink, modifier = Modifier.width(30.dp))
                    ValueBar(count.toFloat() / max, modifier = Modifier.weight(1f), color = c.brand)
                    Text("$count", style = Ucs.type.mono, color = c.ink2, modifier = Modifier.width(24.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${plural(i.total, "graded course")} counted towards your GPA.", style = Ucs.type.caption, color = c.ink3)
    }
}

@Composable
private fun InfluenceCard(i: Insight.CourseWeightInfluence) {
    val c = Ucs.colors
    InsightCard("Full vs half courses", "A full course moves your GPA twice as far", "influence") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfluenceRow("Full course", "${i.fullCredits} credits", i.fullStep, c.primary)
            InfluenceRow("Half course", "${i.halfCredits} credits", i.halfStep, c.bandMerit)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Effect of one grade step (for example B+ to A, +0.50) on your final GPA, out of ${i.totalCredits} credits" +
                if (i.usesRemainingCourses) " including your remaining courses." else ".",
            style = Ucs.type.caption,
            color = c.ink3,
        )
    }
}

@Composable
private fun InfluenceRow(label: String, sub: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    val c = Ucs.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Dot(color, 8.dp)
        Column(Modifier.weight(1f)) {
            Text(label, style = Ucs.type.label, color = c.ink)
            Text(sub, style = Ucs.type.caption, color = c.ink3)
        }
        Text("+${Format.decimals(value, 3)}", style = Ucs.type.mono.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
    }
}
