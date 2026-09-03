package br.com.qualorock.androidApp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import design.QualORockThemeTokens
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val EntranceStartScale = 0.96f
private const val EntranceStartOffsetDp = 16f

private val EntranceEasing = CubicBezierEasing(
    QualORockThemeTokens.EaseBeat.x1,
    QualORockThemeTokens.EaseBeat.y1,
    QualORockThemeTokens.EaseBeat.x2,
    QualORockThemeTokens.EaseBeat.y2,
)

/** `card-enter`'s (design-system.md §3) per-index stagger delay — pure, so it's unit-testable without Compose. */
fun entranceStaggerDelayMillis(index: Int): Long = QualORockThemeTokens.DurationStaggerMs.toLong() * index

/**
 * A2 — `card-enter`: fade + rise + scale, staggered by [entranceStaggerDelayMillis] per [index].
 * Apply to each `LazyColumn`/`LazyVerticalGrid` item's `Modifier` (consumed by A11/A12's screens).
 */
@Composable
fun Modifier.entranceStagger(index: Int): Modifier {
    val alpha = remember(index) { Animatable(0f) }
    val offsetY = remember(index) { Animatable(EntranceStartOffsetDp) }
    val scale = remember(index) { Animatable(EntranceStartScale) }

    LaunchedEffect(index) {
        delay(entranceStaggerDelayMillis(index))
        coroutineScope {
            launch { alpha.animateTo(1f, tween(QualORockThemeTokens.DurationSlowMs, easing = EntranceEasing)) }
            launch { offsetY.animateTo(0f, tween(QualORockThemeTokens.DurationSlowMs, easing = EntranceEasing)) }
            launch { scale.animateTo(1f, tween(QualORockThemeTokens.DurationSlowMs, easing = EntranceEasing)) }
        }
    }

    return this
        .alpha(alpha.value)
        .offset(y = offsetY.value.dp)
        .scale(scale.value)
}
