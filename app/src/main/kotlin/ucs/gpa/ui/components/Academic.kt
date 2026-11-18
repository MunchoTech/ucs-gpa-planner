package ucs.gpa.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ucs.gpa.core.calc.CourseResult
import ucs.gpa.core.calc.CourseStatus
import ucs.gpa.core.format.Format
import ucs.gpa.core.model.CourseLoad
import ucs.gpa.core.rules.AcademicRules
import ucs.gpa.core.rules.ClassBand
import ucs.gpa.core.rules.GradeDefinition
import ucs.gpa.core.rules.GradeKind
import ucs.gpa.ui.theme.Radii
import ucs.gpa.ui.theme.Ucs
import ucs.gpa.ui.theme.UcsColors

fun bandColor(band: ClassBand?, c: UcsColors): Color = when (band?.id) {
    "distinction" -> c.bandDistinction
    "merit" -> c.bandMerit
    "credit" -> c.bandCredit
    else -> c.bandPass
}

/** The class name as a quiet chip with a coloured dot. */
@Composable
fun BandChip(band: ClassBand?, modifier: Modifier = Modifier, onDark: Boolean = false, prefix: String? = null) {
    val c = Ucs.colors
    val label = (prefix?.let { "$it " } ?: "") + (band?.name ?: "Not classified yet")
    Row(
        modifier
            .clip(Radii.pill)
            .background(if (onDark) c.heroRaised else c.surfaceSubtle)
            .padding(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Dot(if (band == null) c.ink3 else if (onDark && band.id == "pass") c.onHeroMuted else bandColor(band, c), 8.dp)
        Text(label, style = Ucs.type.labelSmall, color = if (onDark) c.onHero else c.ink, maxLines = 1)
    }
}

/** The grade as a compact badge. Ungraded courses get a dashed outline instead of a fake value. */
@Composable
fun GradeBadge(result: CourseResult, modifier: Modifier = Modifier, width: Dp = 44.dp) {
    val c = Ucs.colors
    val grade = result.grade
    val (bg, fg, text) = when (result.status) {
        CourseStatus.COUNTED -> when {
            grade!!.kind == GradeKind.FAIL -> Triple(c.dangerSoft, c.danger, grade.code)
            grade.pointsHundredths >= 400 -> Triple(c.primarySoft, c.primary, grade.code)
            else -> Triple(c.surfaceSubtle, c.ink, grade.code)
        }
        CourseStatus.TEMPORARY -> Triple(c.warningSoft, c.warning, grade?.code ?: "—")
        CourseStatus.INVALID -> Triple(c.dangerSoft, c.danger, "!")
        CourseStatus.EXCLUDED, CourseStatus.REPEAT_EXCLUDED -> Triple(c.surfaceSubtle, c.ink3, grade?.code ?: "—")
        CourseStatus.NOT_GRADED -> Triple(Color.Transparent, c.ink3, "—")
    }
    val dashed = result.status == CourseStatus.NOT_GRADED
    val line = c.lineStrong
    Box(
        modifier
            .width(width)
            .height(34.dp)
            .clip(Radii.small)
            .background(bg)
            .then(
                if (dashed) Modifier.drawBehind {
                    drawRoundRect(
                        color = line,
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))),
                    )
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Ucs.type.titleSmall, color = fg, maxLines = 1)
    }
}

/** "Full · 30 credits", "Half · 15 credits", "Non-credit", "20 credits". */
fun loadLabel(load: CourseLoad, credits: Int?, rules: AcademicRules): String = when {
    credits == null -> "Credits missing"
    credits == 0 -> "Non-credit"
    load == CourseLoad.FULL -> "Full · ${rules.fullCourseCredits} credits"
    load == CourseLoad.HALF -> "Half · ${rules.halfCourseCredits} credits"
    else -> "$credits credits"
}

/** One course in a list: name, weight, grade and its contribution in grade points. */
@Composable
fun CourseRow(result: CourseResult, rules: AcademicRules, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ucs.colors
    val course = result.course
    val statusLine = when (result.status) {
        CourseStatus.COUNTED -> "${Format.points(result.weightedHundredths)} pts"
        CourseStatus.NOT_GRADED -> "Not graded yet"
        CourseStatus.TEMPORARY -> if (result.includedInGpa) "Counted as 0.00" else "Not counted until resolved"
        CourseStatus.EXCLUDED -> "Not counted"
        CourseStatus.REPEAT_EXCLUDED -> "Repeat · not counted"
        CourseStatus.INVALID -> "Needs fixing"
    }
    val statusColor = when (result.status) {
        CourseStatus.COUNTED -> c.ink2
        CourseStatus.TEMPORARY -> c.warning
        CourseStatus.INVALID -> c.danger
        else -> c.ink3
    }
    val description = buildString {
        append(course.displayName).append(", ")
        append(loadLabel(course.load, result.credits, rules)).append(", ")
        append(result.grade?.let { "grade ${it.code}" } ?: "no grade").append(", ").append(statusLine)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (course.code.isNotBlank()) Text(course.code.trim(), style = Ucs.type.monoSmall, color = c.ink3, maxLines = 1)
            }
            Text(
                course.title.ifBlank { course.code.ifBlank { "Untitled course" } },
                style = Ucs.type.label,
                color = if (result.status == CourseStatus.EXCLUDED || result.status == CourseStatus.REPEAT_EXCLUDED) c.ink2 else c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${loadLabel(course.load, result.credits, rules)}  ·  $statusLine",
                style = Ucs.type.caption,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GradeBadge(result)
    }
}

/**
 * The four classes as equal segments, with the student's GPA (orange) and, optionally, a projected
 * GPA (hollow ring). Segment widths are equal so each class is readable; the boundaries are labelled.
 */
@Composable
fun ClassScale(
    gpa: Double?,
    rules: AcademicRules,
    modifier: Modifier = Modifier,
    projected: Double? = null,
    onDark: Boolean = false,
) {
    val c = Ucs.colors
    val bands = rules.bands.reversed() // Pass … Distinction
    val maxGpa = rules.maxPointsHundredths / 100.0
    fun position(value: Double): Float {
        val v = value.coerceIn(0.0, maxGpa)
        val n = bands.size
        for (i in bands.indices) {
            val lo = bands[i].minGpa
            val hi = if (i + 1 < n) bands[i + 1].minGpa else maxGpa
            if (v < hi || i == n - 1) return ((i + (v - lo) / (hi - lo)) / n).toFloat().coerceIn(0f, 1f)
        }
        return 1f
    }
    val currentIndex = gpa?.let { g -> bands.indexOfLast { g * 100 + 1e-9 >= it.minHundredths } } ?: -1
    val marker by animateFloatAsState(gpa?.let { position(it) } ?: 0f, tween(700), label = "marker")
    val ring by animateFloatAsState(projected?.let { position(it) } ?: 0f, tween(700), label = "ring")
    val segInactive = if (onDark) c.heroLine else c.surfaceSubtle
    val segActive = if (onDark) c.brand else c.primarySoft
    val textMuted = if (onDark) c.onHeroMuted else c.ink3
    val textStrong = if (onDark) c.onHero else c.ink
    val accent = c.accent
    val ringFill = if (onDark) c.hero else c.surface

    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = "Class scale" + (gpa?.let { ", your GPA ${Format.gpa(it)}" } ?: "") + (projected?.let { ", projected ${Format.gpa(it)}" } ?: "")
    }) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(14.dp)) {
            val w = maxWidth
            for (i in 1 until bands.size) {
                Text(
                    Format.decimals(bands[i].minGpa, 2),
                    style = Ucs.type.monoSmall.copy(fontSize = Ucs.type.caption.fontSize * 0.92f),
                    color = textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp).offset(x = w * (i.toFloat() / bands.size) - 20.dp),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.CenterStart) {
            val w = maxWidth
            Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                bands.forEachIndexed { i, _ ->
                    Box(
                        Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (i == currentIndex) segActive else segInactive),
                    )
                }
            }
            if (projected != null) {
                Box(
                    Modifier.offset(x = (w * ring) - 7.dp).size(14.dp).clip(CircleShape).background(ringFill)
                        .border(3.dp, accent, CircleShape),
                )
            }
            if (gpa != null) {
                Box(Modifier.offset(x = (w * marker) - 2.5.dp).width(5.dp).height(22.dp).clip(RoundedCornerShape(3.dp)).background(accent))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            bands.forEachIndexed { i, b ->
                Text(
                    b.name,
                    style = Ucs.type.caption,
                    color = if (i == currentIndex) textStrong else textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
    }
}

data class TrendPoint(val label: String, val yearGpa: Double?, val cumulative: Double?)

/** Year GPA (solid) and cumulative GPA (dashed) on a fixed 0–5 scale window, with class boundaries. */
@Composable
fun TrendChart(points: List<TrendPoint>, rules: AcademicRules, modifier: Modifier = Modifier, height: Dp = 176.dp) {
    val c = Ucs.colors
    val measurer = rememberTextMeasurer()
    val axisStyle = Ucs.type.monoSmall.copy(color = c.ink3, fontSize = Ucs.type.caption.fontSize * 0.9f)
    val valueStyle = Ucs.type.monoSmall.copy(color = c.ink, fontSize = Ucs.type.caption.fontSize * 0.95f)
    val labelStyle = Ucs.type.caption.copy(color = c.ink3)
    val values = points.flatMap { listOfNotNull(it.yearGpa, it.cumulative) }
    val maxY = rules.maxPointsHundredths / 100.0
    val minY = (values.minOrNull() ?: 2.0).let { m -> if (m < 2.0) kotlin.math.floor(m * 2) / 2 else 2.0 }
    val thresholds = rules.targetBands.map { it.minGpa }
    val top = rules.targetBands.first()
    val animated by animateFloatAsState(1f, tween(600), label = "draw")
    val summary = points.filter { it.yearGpa != null }.joinToString(", ") { "${it.label} ${Format.gpa(it.yearGpa)}" }

    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = "GPA by year: $summary" }) {
        val left = 38.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val topY = 10.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        fun y(v: Double) = (bottom - (v - minY) / (maxY - minY) * (bottom - topY)).toFloat()
        val n = points.size
        fun x(i: Int) = if (n <= 1) (left + right) / 2 else left + 14.dp.toPx() + i * (right - left - 28.dp.toPx()) / (n - 1)

        // top-class zone
        drawRect(c.primarySoft.copy(alpha = if (c.isDark) 0.55f else 0.6f), topLeft = Offset(left, y(maxY)), size = androidx.compose.ui.geometry.Size(right - left, y(top.minGpa) - y(maxY)))
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        (thresholds + listOf(minY, maxY)).distinct().forEach { t ->
            val isEdge = t == minY || t == maxY
            drawLine(if (isEdge) c.line else c.lineStrong, Offset(left, y(t)), Offset(right, y(t)), strokeWidth = 1.dp.toPx(), pathEffect = if (isEdge) null else dash)
            val layout = measurer.measure(Format.decimals(t, 2), axisStyle)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y(t) - layout.size.height / 2f))
        }
        val zoneLabel = measurer.measure(top.name.uppercase(), labelStyle.copy(color = c.primary, fontSize = labelStyle.fontSize * 0.82f, letterSpacing = labelStyle.letterSpacing))
        drawText(zoneLabel, topLeft = Offset(right - zoneLabel.size.width - 6.dp.toPx(), y(maxY) + 4.dp.toPx()))

        // cumulative (dashed)
        val cumPts = points.mapIndexedNotNull { i, p -> p.cumulative?.let { Offset(x(i), y(it)) } }
        if (cumPts.size >= 2) {
            val path = Path().apply { moveTo(cumPts[0].x, cumPts[0].y); cumPts.drop(1).forEach { lineTo(it.x, it.y) } }
            drawPath(path, c.ink2.copy(alpha = animated), style = Stroke(width = 1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)), cap = StrokeCap.Round))
        }
        cumPts.forEach { p ->
            drawCircle(c.surface, 3.6.dp.toPx(), p)
            drawCircle(c.ink2, 3.6.dp.toPx(), p, style = Stroke(1.6.dp.toPx()))
        }

        // year GPA (solid)
        val yearPts = points.mapIndexedNotNull { i, p -> p.yearGpa?.let { Triple(i, Offset(x(i), y(it)), it) } }
        if (yearPts.size >= 2) {
            val path = Path().apply { moveTo(yearPts[0].second.x, yearPts[0].second.y); yearPts.drop(1).forEach { lineTo(it.second.x, it.second.y) } }
            drawPath(path, c.primary.copy(alpha = animated), style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
        }
        yearPts.forEach { (_, p, v) ->
            drawCircle(c.surface, 5.5.dp.toPx(), p)
            drawCircle(c.primary, 4.2.dp.toPx(), p)
            val layout = measurer.measure(Format.gpa(v), valueStyle)
            val above = p.y - layout.size.height - 7.dp.toPx()
            val ly = if (above < topY) p.y + 8.dp.toPx() else above
            drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, ly))
        }

        points.forEachIndexed { i, p ->
            val layout = measurer.measure(p.label, labelStyle)
            drawText(layout, topLeft = Offset(x(i) - layout.size.width / 2f, size.height - layout.size.height - 2.dp.toPx()))
        }
    }
}

@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    val c = Ucs.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.primary))
            Text("Year GPA", style = Ucs.type.caption, color = c.ink2)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(Modifier.width(16.dp).height(3.dp)) {
                drawLine(c.ink2, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = size.height * 0.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 3f)))
            }
            Text("Cumulative", style = Ucs.type.caption, color = c.ink2)
        }
    }
}

/** Equal-width segmented control. */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ucs.colors
    Row(
        modifier.fillMaxWidth().clip(Radii.control).background(c.surfaceSubtle).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .clip(Radii.small)
                    .background(if (selected) c.surface else Color.Transparent)
                    .then(if (selected) Modifier.border(1.dp, c.line, Radii.small) else Modifier)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Ucs.type.labelSmall, color = if (selected) c.ink else c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A selectable pill. Selected state is charcoal (Linear-style), not colourful. */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, sub: String? = null, style: TextStyle = Ucs.type.labelSmall) {
    val c = Ucs.colors
    Row(
        modifier
            .clip(Radii.pill)
            .background(if (selected) c.ink else c.surface)
            .border(1.dp, if (selected) c.ink else c.line, Radii.pill)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = style, color = if (selected) c.canvas else c.ink, maxLines = 1)
        if (sub != null) Text(sub, style = Ucs.type.monoSmall, color = if (selected) c.canvas.copy(alpha = 0.7f) else c.ink3, maxLines = 1)
    }
}

/** Grade selection for the course editor, grouped as on the official table. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradePicker(rules: AcademicRules, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    val groups = listOf(
        "Pass" to rules.grades.filter { it.kind == GradeKind.PASS },
        "Fail" to rules.grades.filter { it.kind == GradeKind.FAIL },
        "Temporary" to rules.grades.filter { it.kind == GradeKind.TEMPORARY },
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip("No grade yet", selected = selected.isNullOrBlank(), onClick = { onSelect(null) })
        }
        groups.forEach { (label, grades) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Overline(label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    grades.forEach { g ->
                        GradeChip(g, selected = rules.grade(selected)?.code == g.code, onClick = { onSelect(g.code) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeChip(grade: GradeDefinition, selected: Boolean, onClick: () -> Unit) {
    val c = Ucs.colors
    Column(
        Modifier
            .widthIn(min = 58.dp)
            .clip(Radii.control)
            .background(if (selected) c.ink else c.surface)
            .border(1.dp, if (selected) c.ink else c.line, Radii.control)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${grade.code}, ${Format.decimals(grade.points, 2)} points, ${grade.description}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(grade.code, style = Ucs.type.titleSmall, color = if (selected) c.canvas else c.ink)
        Text(
            if (grade.kind == GradeKind.TEMPORARY) "—" else Format.decimals(grade.points, 2),
            style = Ucs.type.monoSmall,
            color = if (selected) c.canvas.copy(alpha = 0.7f) else c.ink3,
        )
    }
}

/** Compact hypothetical-grade selector: tap a grade, tap it again to clear. */
@Composable
fun GradeSegments(options: List<String>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    val c = Ucs.colors
    Row(modifier.fillMaxWidth().clip(Radii.small).background(c.surfaceSubtle).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { g ->
            val isSel = g == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSel) c.accent else Color.Transparent)
                    .selectable(selected = isSel, role = Role.RadioButton, onClick = { onSelect(if (isSel) null else g) })
                    .semantics { contentDescription = if (isSel) "$g selected" else g },
                contentAlignment = Alignment.Center,
            ) {
                Text(g, style = Ucs.type.labelSmall, color = if (isSel) Color(0xFF1B0F03) else c.ink2, maxLines = 1)
            }
        }
    }
}

/** A horizontal bar used in insights. */
@Composable
fun ValueBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Ucs.colors.primary) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Ucs.colors.surfaceSubtle)) {
        Box(Modifier.fillMaxWidth(animated).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Ucs.colors.ink, emphasize: Boolean = false) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (emphasize) Ucs.type.titleSmall else Ucs.type.bodySmall, color = if (emphasize) Ucs.colors.ink else Ucs.colors.ink2, modifier = Modifier.weight(1f))
        Text(value, style = if (emphasize) Ucs.type.mono.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else Ucs.type.mono, color = valueColor)
    }
}

@Composable
fun ClickableText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Ucs.colors.primary, style: TextStyle = Ucs.type.labelSmall) {
    Text(text, style = style, color = color, modifier = modifier.clip(Radii.small).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp))
}
