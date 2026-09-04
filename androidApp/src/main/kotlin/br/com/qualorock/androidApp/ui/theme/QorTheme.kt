package br.com.qualorock.androidApp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import design.QualORockThemeTokens

/**
 * A14 — thin `MaterialTheme` wrapper mapping NIGHTLIFE-GV's [QualORockThemeTokens] color tokens
 * (already used ad-hoc throughout `ui/components/`, e.g. [br.com.qualorock.androidApp.ui.components.BottomNav])
 * onto Compose's `ColorScheme`, so `MainActivity`/the nav graph get one themed root instead of the
 * bare `MaterialTheme { Surface { ... } }` A1 placeholder used.
 *
 * **No custom `Typography`.** NIGHTLIFE-GV specifies Space Grotesk (display) / Inter (body) per
 * `design-system.md` §2.2 and [QualORockThemeTokens.FontFamilyDisplay]/[FontFamilyBody], but no
 * `res/font/` resources or `FontFamily` wiring exist anywhere in this module yet (confirmed while
 * building this task) — every screen built so far (A7-A13) sets its own `fontSize`/`fontWeight`
 * per-`Text` from the same tokens rather than relying on `MaterialTheme.typography`. Inventing font
 * resources here would be scope creep for a "wire the nav graph" task; default Compose typography
 * is used and only the color tokens are applied.
 *
 * NIGHTLIFE-GV is a dark-only design system (`design-system.md` §1 — "near-black nightlife base");
 * [colorScheme] always resolves to the dark palette regardless of the device's system theme, same
 * as every screen built in A7-A13 hardcoding [QualORockThemeTokens] dark values directly.
 */
@Composable
fun QorTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(QualORockThemeTokens.AccentPink),
        secondary = Color(QualORockThemeTokens.AccentBlue),
        tertiary = Color(QualORockThemeTokens.AccentPurple),
        background = Color(QualORockThemeTokens.ColorBgDeep),
        surface = Color(QualORockThemeTokens.ColorSurfaceCard),
        surfaceVariant = Color(QualORockThemeTokens.ColorBgBase),
        onPrimary = Color(QualORockThemeTokens.ColorTextPrimary),
        onSecondary = Color(QualORockThemeTokens.ColorTextPrimary),
        onBackground = Color(QualORockThemeTokens.ColorTextPrimary),
        onSurface = Color(QualORockThemeTokens.ColorTextPrimary),
        onSurfaceVariant = Color(QualORockThemeTokens.ColorTextSecondary),
        outline = Color(QualORockThemeTokens.ColorBorderSubtle),
        error = Color(QualORockThemeTokens.ColorDanger),
    )

    MaterialTheme(colorScheme = colorScheme, content = content)
}
