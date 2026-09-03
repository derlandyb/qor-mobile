package br.com.qualorock.androidApp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.NightlifeGvTokens
import domain.event.Event

/** design-system.md §3's card-hover spec (`hover:scale-[1.03] hover:-translate-y-1`), as pure functions of press state. */
object EventCardMotion {
    // design-system.md §4.1's cover-image aspect ratio (4:5, portrait poster crop).
    const val ImageAspectRatioWidth = 4f
    const val ImageAspectRatioHeight = 5f

    private const val RestingScale = 1f
    private const val PressedScale = 1.03f
    private const val PressedRiseDp = -4f

    fun pressScale(pressed: Boolean): Float = if (pressed) PressedScale else RestingScale
    fun pressRiseDp(pressed: Boolean): Float = if (pressed) PressedRiseDp else 0f
}

/**
 * A2 — Event Card (design-system.md §4.1). `onMapClick` opens a maps deep link built from
 * [Event.address] (no dedicated `mapsUrl` field on the API contract); an Instagram CTA is
 * omitted entirely — `Event` has no `instagramUrl` field, the same client-side gap already
 * documented for `qor-website`'s `EventCard` (AD-015/STATE.md).
 */
@Composable
fun EventCard(event: Event, onClick: () -> Unit, onMapClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = EventCardMotion.pressScale(pressed),
        animationSpec = tween(NightlifeGvTokens.DurationBaseMs),
        label = "eventCardScale",
    )
    val riseDp by animateDpAsState(
        targetValue = EventCardMotion.pressRiseDp(pressed).dp,
        animationSpec = tween(NightlifeGvTokens.DurationBaseMs),
        label = "eventCardRise",
    )
    val dateLabel = formatDateBadge(event.startsAt)
    val timeLabel = formatEventTime(event.startsAt)
    val cityStyle = CityFilterColors.styleFor(event.city)

    Column(
        modifier = modifier
            .scale(scale)
            .offset(y = riseDp)
            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusLgDp.dp))
            .background(Color(NightlifeGvTokens.ColorSurfaceCard))
            .border(
                width = NightlifeGvTokens.BorderWidthHairlineDp.dp,
                color = Color(NightlifeGvTokens.ColorBorderSubtle),
                shape = RoundedCornerShape(NightlifeGvTokens.RadiusLgDp.dp),
            ),
    ) {
        // The card-tap region (image + title/venue block) is a separate clickable from the CTA
        // row below — Compose's semantics-merge chains a descendant's `OnClick` action into an
        // ancestor's own `clickable` when both apply directly to the same subtree, so
        // `MapaCta`'s independent action must be a *sibling*, not nested inside this one.
        Column(
            modifier = Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(EventCardMotion.ImageAspectRatioWidth / EventCardMotion.ImageAspectRatioHeight)
                    .background(Color(NightlifeGvTokens.ColorBgBase)),
            ) {
                Box(
                    modifier = Modifier
                        .padding(NightlifeGvTokens.Space3Dp.dp)
                        .clip(RoundedCornerShape(NightlifeGvTokens.RadiusSmDp.dp))
                        .background(Color(NightlifeGvTokens.ColorBgDeep).copy(alpha = 0.8f))
                        .padding(horizontal = NightlifeGvTokens.Space2Dp.dp, vertical = NightlifeGvTokens.Space1Dp.dp),
                ) {
                    Column {
                        Text(dateLabel.month, color = Color(NightlifeGvTokens.ColorTextSecondary), fontSize = 10.sp)
                        Text(
                            dateLabel.day,
                            color = Color(NightlifeGvTokens.ColorTextPrimary),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                    }
                }
                GenreTag(genre = event.genre, modifier = Modifier.padding(NightlifeGvTokens.Space3Dp.dp))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(NightlifeGvTokens.Space2Dp.dp),
                modifier = Modifier.padding(NightlifeGvTokens.Space3Dp.dp),
            ) {
                Text(
                    text = event.title,
                    color = Color(NightlifeGvTokens.ColorTextPrimary),
                    fontWeight = FontWeight.Bold,
                    fontSize = NightlifeGvTokens.TextEventTitle.SizeSp.sp,
                    maxLines = 2,
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            event.address,
                            color = Color(NightlifeGvTokens.ColorTextPrimary),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(timeLabel, color = Color(NightlifeGvTokens.ColorTextSecondary), fontSize = 13.sp)
                    }
                    Text(
                        text = stringResource(cityStyle.labelRes).uppercase(),
                        color = cityStyle.activeColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = NightlifeGvTokens.TextBadge.SizeSp.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusPillDp.dp))
                            .background(cityStyle.activeColor.copy(alpha = 0.15f))
                            .padding(horizontal = NightlifeGvTokens.Space2Dp.dp, vertical = NightlifeGvTokens.Space1Dp.dp),
                    )
                }
            }
        }

        MapaCta(
            onClick = onMapClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = NightlifeGvTokens.Space3Dp.dp, end = NightlifeGvTokens.Space3Dp.dp, bottom = NightlifeGvTokens.Space3Dp.dp),
        )
    }
}
