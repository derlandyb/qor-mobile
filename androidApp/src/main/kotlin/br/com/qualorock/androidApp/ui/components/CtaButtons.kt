package br.com.qualorock.androidApp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import design.QualORockThemeTokens

/**
 * design-system.md §4.4's two CTA treatments, as pure functions of press state so the target
 * animation values are unit-testable without Compose (see `CtaButtonMotionTest`).
 */
object CtaButtonMotion {
    const val MapaRestingAlpha = 0.10f

    /** "Ver no Mapa": 10%-tint outline at rest, fully solid blue on press. */
    fun mapaBackgroundAlpha(pressed: Boolean): Float = if (pressed) 1f else MapaRestingAlpha

    /** "Ver Instagram": `background-position` 0% (left) at rest, 100% (right) on press. */
    fun instagramGradientOffset(pressed: Boolean): Float = if (pressed) 1f else 0f
}

private const val InstagramGradientSpanPx = 400f

/** A2 — "Ver no Mapa" (blue outline → solid on press), design-system.md §4.4. */
@Composable
fun MapaCta(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(QualORockThemeTokens.DurationBaseMs),
        label = "mapaCtaPress",
    )
    val blue = Color(QualORockThemeTokens.AccentBlue)
    val dark = Color(QualORockThemeTokens.ColorBgDeep)
    val backgroundColor = lerp(blue.copy(alpha = CtaButtonMotion.MapaRestingAlpha), blue, progress)
    val contentColor = lerp(blue, dark, progress)

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .testTag(stringResource(R.string.test_tag_mapa_cta))
            .clip(RoundedCornerShape(QualORockThemeTokens.RadiusMdDp.dp))
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
    ) {
        androidx.compose.material3.Text(
            text = stringResource(R.string.cta_ver_no_mapa),
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = QualORockThemeTokens.TextButton.SizeSp.sp,
        )
    }
}

/** A2 — "Ver Instagram" (pink→purple animated gradient, `background-position` shift), design-system.md §4.4. */
@Composable
fun InstagramCta(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = CtaButtonMotion.instagramGradientOffset(pressed),
        animationSpec = tween(QualORockThemeTokens.DurationBaseMs),
        label = "instagramCtaPress",
    )
    val pink = Color(QualORockThemeTokens.AccentPink)
    val purple = Color(QualORockThemeTokens.AccentPurple)
    val shift = InstagramGradientSpanPx * progress
    val brush = Brush.horizontalGradient(
        colors = listOf(pink, purple),
        startX = shift - InstagramGradientSpanPx,
        endX = shift,
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(QualORockThemeTokens.RadiusMdDp.dp))
            .background(brush)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
    ) {
        androidx.compose.material3.Text(
            text = stringResource(R.string.cta_ver_instagram),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = QualORockThemeTokens.TextButton.SizeSp.sp,
        )
    }
}

/** Diameter of [PrimaryButton]'s loading spinner — reuses `--space-4` so no new token is invented. */
private val PrimaryButtonSpinnerSizeDp = QualORockThemeTokens.Space4Dp.dp

/**
 * A7 — generic solid CTA for form submits (login, and future signup/reset screens), design-system.md
 * §4.4's filled-button treatment applied with an arbitrary [text]/[onClick] instead of [MapaCta]'s
 * hardcoded copy. Shows a spinner in place of the label while [isLoading], and is inert (no click,
 * dimmed) while [enabled] is false.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val backgroundColor = if (enabled) {
        Color(QualORockThemeTokens.AccentPink)
    } else {
        Color(QualORockThemeTokens.ColorBorderSubtle)
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(QualORockThemeTokens.RadiusMdDp.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(PrimaryButtonSpinnerSizeDp),
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = QualORockThemeTokens.TextButton.SizeSp.sp,
            )
        }
    }
}

/**
 * A7 — generic outline CTA, [MapaCta]'s resting treatment generalized to an arbitrary [text]/[onClick]
 * (e.g. "Entrar com Google"). Inert (no click, dimmed border/text) while [enabled] is false — used to
 * render a disabled stub where the underlying integration doesn't exist yet.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) {
        Color(QualORockThemeTokens.ColorTextPrimary)
    } else {
        Color(QualORockThemeTokens.ColorTextTertiary)
    }

    val shape = RoundedCornerShape(QualORockThemeTokens.RadiusMdDp.dp)

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(shape)
            .border(width = QualORockThemeTokens.BorderWidthHairlineDp.dp, color = contentColor, shape = shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = QualORockThemeTokens.TextButton.SizeSp.sp,
        )
    }
}
