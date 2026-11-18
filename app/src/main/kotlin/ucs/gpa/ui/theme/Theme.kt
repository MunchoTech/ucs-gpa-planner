package ucs.gpa.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UCS design tokens. Colours are derived from the UCS mark: teal (#2A7F8A) for the brand and
 * primary actions, orange (#FE8D2D) only for "you are here" markers and small highlights, on a
 * charcoal-navy and warm-neutral base. Every colour in the UI comes from here.
 */
@Immutable
data class UcsColors(
    val canvas: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val brand: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val hero: Color,
    val heroRaised: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroLine: Color,
    val positive: Color,
    val positiveSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val bandDistinction: Color,
    val bandMerit: Color,
    val bandCredit: Color,
    val bandPass: Color,
    val isDark: Boolean,
)

val LightColors = UcsColors(
    canvas = Color(0xFFF5F5F2),
    surface = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFEFEFEA),
    line = Color(0xFFE3E3DD),
    lineStrong = Color(0xFFCFCFC7),
    ink = Color(0xFF0F1724),
    ink2 = Color(0xFF515A69),
    ink3 = Color(0xFF858C97),
    brand = Color(0xFF2A7F8A),
    primary = Color(0xFF1D6B76),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFE2EFF0),
    accent = Color(0xFFF2892C),
    accentSoft = Color(0xFFFDEEDF),
    onAccentSoft = Color(0xFF7A3D06),
    hero = Color(0xFF0F1B2D),
    heroRaised = Color(0xFF192841),
    onHero = Color(0xFFF2F5F8),
    onHeroMuted = Color(0xFF94A2B5),
    heroLine = Color(0xFF263751),
    positive = Color(0xFF1C7653),
    positiveSoft = Color(0xFFE1F1E9),
    warning = Color(0xFF8F5B00),
    warningSoft = Color(0xFFFAF0DA),
    danger = Color(0xFFB42318),
    dangerSoft = Color(0xFFFBE8E6),
    bandDistinction = Color(0xFF2A7F8A),
    bandMerit = Color(0xFF6BA7AF),
    bandCredit = Color(0xFFADCBCF),
    bandPass = Color(0xFFD9DFE0),
    isDark = false,
)

val DarkColors = UcsColors(
    canvas = Color(0xFF0A0F17),
    surface = Color(0xFF111824),
    surfaceSubtle = Color(0xFF18212E),
    line = Color(0xFF222C3A),
    lineStrong = Color(0xFF2F3B4D),
    ink = Color(0xFFE8ECF1),
    ink2 = Color(0xFFA3ADBA),
    ink3 = Color(0xFF6E7987),
    brand = Color(0xFF3C9CA8),
    primary = Color(0xFF62BCC7),
    onPrimary = Color(0xFF062126),
    primarySoft = Color(0xFF14313A),
    accent = Color(0xFFFF9C47),
    accentSoft = Color(0xFF35230F),
    onAccentSoft = Color(0xFFFFC796),
    hero = Color(0xFF13223A),
    heroRaised = Color(0xFF1B2D4A),
    onHero = Color(0xFFF2F5F8),
    onHeroMuted = Color(0xFF94A3B8),
    heroLine = Color(0xFF2A3D5C),
    positive = Color(0xFF52C48E),
    positiveSoft = Color(0xFF12301F),
    warning = Color(0xFFE6B04E),
    warningSoft = Color(0xFF33270F),
    danger = Color(0xFFF2877B),
    dangerSoft = Color(0xFF3A1A17),
    bandDistinction = Color(0xFF4FB2BE),
    bandMerit = Color(0xFF34808A),
    bandCredit = Color(0xFF285860),
    bandPass = Color(0xFF263640),
    isDark = true,
)

data class UcsFonts(val sans: FontFamily, val mono: FontFamily) {
    companion object {
        val Fallback = UcsFonts(FontFamily.SansSerif, FontFamily.Monospace)
    }
}

/** Named text styles. Screen titles stay at 24sp: no oversized headings. */
@Immutable
data class UcsType(
    val heroNumber: TextStyle,
    val bigNumber: TextStyle,
    val number: TextStyle,
    val screenTitle: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
    val overline: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
)

fun ucsType(fonts: UcsFonts): UcsType {
    val sans = fonts.sans
    val tnum = "tnum"
    return UcsType(
        heroNumber = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 60.sp, lineHeight = 64.sp, letterSpacing = (-2).sp, fontFeatureSettings = tnum),
        bigNumber = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-1).sp, fontFeatureSettings = tnum),
        number = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp, fontFeatureSettings = tnum),
        screenTitle = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp),
        title = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp),
        titleSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp),
        body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
        caption = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        label = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
        labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
        overline = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp),
        mono = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = tnum),
        monoSmall = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = tnum),
    )
}

object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val screen = 20.dp
}

object Radii {
    val card = RoundedCornerShape(12.dp)
    val control = RoundedCornerShape(10.dp)
    val small = RoundedCornerShape(8.dp)
    val pill = RoundedCornerShape(50)
}

/** Brand imagery supplied by the platform (Android resources or files on desktop). */
data class BrandAssets(val mark: Painter?)

val LocalUcsColors = staticCompositionLocalOf { LightColors }
val LocalUcsType = staticCompositionLocalOf { ucsType(UcsFonts.Fallback) }
val LocalBrandAssets = staticCompositionLocalOf { BrandAssets(null) }

object Ucs {
    val colors: UcsColors @Composable get() = LocalUcsColors.current
    val type: UcsType @Composable get() = LocalUcsType.current
    val brand: BrandAssets @Composable get() = LocalBrandAssets.current
}

private fun materialScheme(c: UcsColors): ColorScheme {
    val base = if (c.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primarySoft,
        onPrimaryContainer = c.ink,
        secondary = c.ink2,
        onSecondary = c.surface,
        secondaryContainer = c.primarySoft,
        onSecondaryContainer = c.ink,
        tertiary = c.accent,
        background = c.canvas,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        surfaceVariant = c.surfaceSubtle,
        onSurfaceVariant = c.ink2,
        surfaceContainerLowest = c.surface,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surfaceSubtle,
        inverseSurface = c.ink,
        inverseOnSurface = c.surface,
        outline = c.lineStrong,
        outlineVariant = c.line,
        error = c.danger,
        onError = c.surface,
        errorContainer = c.dangerSoft,
        onErrorContainer = c.danger,
        scrim = Color.Black.copy(alpha = 0.4f),
    )
}

private fun materialTypography(t: UcsType) = Typography(
    headlineSmall = t.screenTitle,
    titleLarge = t.screenTitle,
    titleMedium = t.title,
    titleSmall = t.titleSmall,
    bodyLarge = t.body,
    bodyMedium = t.bodySmall,
    bodySmall = t.caption,
    labelLarge = t.label,
    labelMedium = t.labelSmall,
    labelSmall = t.caption,
)

@Composable
fun UcsTheme(
    dark: Boolean,
    fonts: UcsFonts = UcsFonts.Fallback,
    brand: BrandAssets = BrandAssets(null),
    content: @Composable () -> Unit,
) {
    val colors = if (dark) DarkColors else LightColors
    val type = androidx.compose.runtime.remember(fonts) { ucsType(fonts) }
    CompositionLocalProvider(
        LocalUcsColors provides colors,
        LocalUcsType provides type,
        LocalBrandAssets provides brand,
    ) {
        MaterialTheme(
            colorScheme = materialScheme(colors),
            typography = materialTypography(type),
            shapes = Shapes(
                extraSmall = Radii.small,
                small = Radii.small,
                medium = Radii.control,
                large = Radii.card,
                extraLarge = RoundedCornerShape(20.dp),
            ),
            content = content,
        )
    }
}
