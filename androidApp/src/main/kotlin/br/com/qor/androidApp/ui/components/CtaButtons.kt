package br.com.qor.androidApp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.NightlifeGvTokens

/** Test hook for locating this CTA when it's a sibling of other clickable regions (e.g. `EventCard`'s own card-tap area). */
const val MapaCtaTestTag = "mapaCta"

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
        animationSpec = tween(NightlifeGvTokens.DurationBaseMs),
        label = "mapaCtaPress",
    )
    val blue = Color(NightlifeGvTokens.AccentBlue)
    val dark = Color(NightlifeGvTokens.ColorBgDeep)
    val backgroundColor = lerp(blue.copy(alpha = CtaButtonMotion.MapaRestingAlpha), blue, progress)
    val contentColor = lerp(blue, dark, progress)

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .testTag(MapaCtaTestTag)
            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusMdDp.dp))
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = NightlifeGvTokens.Space3Dp.dp, vertical = NightlifeGvTokens.Space2Dp.dp),
    ) {
        androidx.compose.material3.Text(
            text = "Ver no Mapa",
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = NightlifeGvTokens.TextButton.SizeSp.sp,
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
        animationSpec = tween(NightlifeGvTokens.DurationBaseMs),
        label = "instagramCtaPress",
    )
    val pink = Color(NightlifeGvTokens.AccentPink)
    val purple = Color(NightlifeGvTokens.AccentPurple)
    val shift = InstagramGradientSpanPx * progress
    val brush = Brush.horizontalGradient(
        colors = listOf(pink, purple),
        startX = shift - InstagramGradientSpanPx,
        endX = shift,
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusMdDp.dp))
            .background(brush)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = NightlifeGvTokens.Space3Dp.dp, vertical = NightlifeGvTokens.Space2Dp.dp),
    ) {
        androidx.compose.material3.Text(
            text = "Ver Instagram",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = NightlifeGvTokens.TextButton.SizeSp.sp,
        )
    }
}
