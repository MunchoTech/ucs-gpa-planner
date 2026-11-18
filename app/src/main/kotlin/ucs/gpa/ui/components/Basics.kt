package ucs.gpa.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ucs.gpa.ui.theme.Radii
import ucs.gpa.ui.theme.Spacing
import ucs.gpa.ui.theme.Ucs

/** The standard container: white surface, 1dp hairline, 12dp corners, no shadow. */
@Composable
fun UcsCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Spacing.md),
    onClick: (() -> Unit)? = null,
    color: Color = Ucs.colors.surface,
    borderColor: Color = Ucs.colors.line,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(Radii.card)
            .background(color)
            .border(1.dp, borderColor, Radii.card)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .animateContentSize(tween(220))
            .padding(padding),
        content = content,
    )
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = Ucs.colors.ink3) {
    Text(text.uppercase(), modifier, style = Ucs.type.overline, color = color)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 32.dp).semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Ucs.type.titleSmall, color = Ucs.colors.ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = Ucs.type.labelSmall,
                color = Ucs.colors.primary,
                modifier = Modifier.clip(Radii.small).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: TextStyle = Ucs.type.labelSmall,
) {
    Row(
        modifier.clip(Radii.pill).background(container).padding(horizontal = 10.dp, vertical = 4.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, style = style, color = content, maxLines = 1)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    val mark = Ucs.brand.mark
    if (mark != null) {
        Image(mark, contentDescription = "UCS", modifier = modifier.size(width = size * 0.77f, height = size))
    } else {
        Box(modifier.size(size).clip(Radii.small).background(Ucs.colors.brand), contentAlignment = Alignment.Center) {
            Text("U", color = Color.White, style = Ucs.type.titleSmall)
        }
    }
}

/** Top bar for tab screens (large title) and pushed screens (back arrow, smaller title). */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = if (onBack != null) 4.dp else Spacing.screen, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Ucs.colors.ink)
            }
        }
        Column(Modifier.weight(1f).padding(vertical = 8.dp).semantics { heading() }) {
            Text(
                title,
                style = if (onBack != null) Ucs.type.title else Ucs.type.screenTitle,
                color = Ucs.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Text(subtitle, style = Ucs.type.caption, color = Ucs.colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = Radii.control,
        colors = ButtonDefaults.buttonColors(
            containerColor = Ucs.colors.primary,
            contentColor = Ucs.colors.onPrimary,
            disabledContainerColor = Ucs.colors.surfaceSubtle,
            disabledContentColor = Ucs.colors.ink3,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Ucs.type.label)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, destructive: Boolean = false) {
    val content = if (destructive) Ucs.colors.danger else Ucs.colors.ink
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = Radii.control,
        border = androidx.compose.foundation.BorderStroke(1.dp, Ucs.colors.lineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = content, containerColor = Ucs.colors.surface),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Ucs.type.label)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Ucs.colors.primary) {
    TextButton(onClick = onClick, modifier = modifier, shape = Radii.small) {
        Text(text, style = Ucs.type.labelSmall, color = color)
    }
}

enum class BannerKind { INFO, WARNING, ERROR }

@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    kind: BannerKind = BannerKind.INFO,
    title: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = Ucs.colors
    val (bg, fg, icon) = when (kind) {
        BannerKind.INFO -> Triple(c.surfaceSubtle, c.ink2, Icons.Outlined.Info)
        BannerKind.WARNING -> Triple(c.warningSoft, c.warning, Icons.Outlined.WarningAmber)
        BannerKind.ERROR -> Triple(c.dangerSoft, c.danger, Icons.Outlined.ErrorOutline)
    }
    Row(
        modifier.fillMaxWidth().clip(Radii.control).background(bg).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title != null) Text(title, style = Ucs.type.titleSmall, color = if (kind == BannerKind.INFO) c.ink else fg)
            Text(text, style = Ucs.type.bodySmall, color = if (kind == BannerKind.INFO) c.ink2 else c.ink)
            if (action != null && onAction != null) {
                Text(
                    action,
                    style = Ucs.type.labelSmall,
                    color = if (kind == BannerKind.INFO) c.primary else fg,
                    modifier = Modifier.padding(top = 4.dp).clip(Radii.small).clickable(role = Role.Button, onClick = onAction).padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Ucs.colors.primarySoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Ucs.colors.primary, modifier = Modifier.size(24.dp))
        }
        Text(title, style = Ucs.type.title, color = Ucs.colors.ink)
        Text(
            body,
            style = Ucs.type.bodySmall,
            color = Ucs.colors.ink2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (primary != null || secondary != null) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                primary?.let { PrimaryButton(it.first, it.second) }
                secondary?.let { SecondaryButton(it.first, it.second) }
            }
        }
    }
}

/** A tappable row for settings-style lists. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    titleColor: Color = Ucs.colors.ink,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (titleColor == Ucs.colors.ink) Ucs.colors.ink2 else titleColor, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Ucs.type.label, color = titleColor)
            if (subtitle != null) Text(subtitle, style = Ucs.type.bodySmall, color = Ucs.colors.ink3)
        }
        trailing?.invoke()
        if (onClick != null && showChevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Ucs.colors.ink3, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Ucs.colors.line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Ucs.colors.primary,
    track: Color = Ucs.colors.surfaceSubtle,
    height: Dp = 6.dp,
    marker: Float? = null,
    markerColor: Color = Ucs.colors.ink,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(500), label = "progress")
    Box(modifier.fillMaxWidth().height(height + 6.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track))
        Box(Modifier.fillMaxWidth(animated).height(height).clip(RoundedCornerShape(50)).background(color))
        if (marker != null) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(height + 6.dp)) {
                val x = maxWidth * marker.coerceIn(0f, 1f)
                Box(Modifier.padding(start = (x - 1.dp).coerceAtLeast(0.dp)).width(2.dp).height(height + 6.dp).background(markerColor))
            }
        }
    }
}

/** A labelled figure: label above, value below. */
@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, onDark: Boolean = false, description: String? = null) {
    val c = Ucs.colors
    Column(
        modifier.semantics(mergeDescendants = true) { if (description != null) contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = Ucs.type.caption, color = if (onDark) c.onHeroMuted else c.ink3, maxLines = 2)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = Ucs.type.number.copy(fontSize = Ucs.type.number.fontSize * 0.9f), color = if (onDark) c.onHero else c.ink, maxLines = 1)
            if (sub != null) Text(sub, style = Ucs.type.caption, color = if (onDark) c.onHeroMuted else c.ink3, modifier = Modifier.padding(bottom = 3.dp), maxLines = 1)
        }
    }
}
