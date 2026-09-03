package br.com.qualorock.androidApp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/** design-system.md §4.3 tint/text color for one genre chip; `solidBackground` = Sertanejo's solid treatment. */
data class GenreTagStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val solidBackground: Boolean,
)

private const val TintAlpha = 0.15f

/**
 * A2 — the 5-genre color map from design-system.md §4.3. `Event.genre` is a raw API string
 * (no genre-list endpoint yet — the same gap `qor-website`'s `GenreTagSet` documents, per
 * STATE.md's open Todo), so an unknown genre falls back to a neutral tint rather than crashing.
 */
object GenreTagColors {
    fun styleFor(genre: String): GenreTagStyle = when (genre.lowercase()) {
        "rock" -> tinted(NightlifeGvTokens.AccentOrange)
        "samba" -> tinted(NightlifeGvTokens.AccentPink)
        "sertanejo" -> GenreTagStyle(
            backgroundColor = Color(NightlifeGvTokens.AccentPink),
            textColor = Color(NightlifeGvTokens.ColorBgBase),
            solidBackground = true,
        )
        "eletrônico", "eletronico" -> tinted(NightlifeGvTokens.AccentPurple)
        "reggae" -> tinted(NightlifeGvTokens.AccentBlue)
        else -> GenreTagStyle(
            backgroundColor = Color(NightlifeGvTokens.ColorSurfaceCardHover),
            textColor = Color(NightlifeGvTokens.ColorTextSecondary),
            solidBackground = false,
        )
    }

    private fun tinted(accent: Long): GenreTagStyle =
        GenreTagStyle(backgroundColor = Color(accent).copy(alpha = TintAlpha), textColor = Color(accent), solidBackground = false)
}

@Composable
fun GenreTag(genre: String, modifier: Modifier = Modifier) {
    val style = GenreTagColors.styleFor(genre)
    val description = stringResource(R.string.content_description_genre, genre)
    Text(
        text = genre.uppercase(),
        color = style.textColor,
        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = NightlifeGvTokens.TextBadge.SizeSp.sp,
        modifier = modifier
            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusSmDp.dp))
            .background(style.backgroundColor)
            .padding(horizontal = NightlifeGvTokens.Space2Dp.dp, vertical = NightlifeGvTokens.Space1Dp.dp)
            .semantics { contentDescription = description },
    )
}
