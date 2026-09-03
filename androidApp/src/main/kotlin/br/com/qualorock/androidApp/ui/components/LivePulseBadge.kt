package br.com.qualorock.androidApp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import design.NightlifeGvTokens

private const val PulseDurationMs = 1800
private const val PulseMinAlpha = 0.4f
private const val DotSizeDp = 8

/** A2 — "ao vivo agora" live-pulse dot: the one continuous-loop animation in the system (design-system.md §3). */
@Composable
fun LivePulseBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = PulseMinAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(PulseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "livePulseAlpha",
    )
    val pink = Color(NightlifeGvTokens.AccentPink)
    val description = stringResource(R.string.content_description_live_now)

    Row(
        horizontalArrangement = Arrangement.spacedBy(NightlifeGvTokens.Space1Dp.dp),
        modifier = modifier
            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusPillDp.dp))
            .background(pink.copy(alpha = 0.15f))
            .padding(horizontal = NightlifeGvTokens.Space2Dp.dp, vertical = NightlifeGvTokens.Space1Dp.dp)
            .semantics { contentDescription = description },
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(DotSizeDp.dp)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(pink),
        )
        Text(
            text = stringResource(R.string.live_pulse_label),
            color = pink,
            fontWeight = FontWeight.SemiBold,
            fontSize = NightlifeGvTokens.TextBadge.SizeSp.sp,
        )
    }
}
