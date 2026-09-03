package br.com.qor.androidApp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.NightlifeGvTokens
import domain.enum.City

/** design-system.md §4.2's per-hub badge treatment: active/inactive colors + the pt-BR hub label. */
data class CityFilterStyle(
    val activeColor: Color,
    val activeTextColor: Color,
    val label: String,
)

private const val InactiveTintAlpha = 0.15f

/** A2 — the 4-hub color table from design-system.md §4.2, keyed by [City]. */
object CityFilterColors {
    fun styleFor(city: City): CityFilterStyle = when (city) {
        City.Vitoria -> CityFilterStyle(Color(NightlifeGvTokens.AccentPink), Color(NightlifeGvTokens.ColorBgDeep), "Vitória")
        City.VilaVelha -> CityFilterStyle(Color(NightlifeGvTokens.AccentBlue), Color(NightlifeGvTokens.ColorBgDeep), "Vila Velha")
        City.Serra -> CityFilterStyle(Color(NightlifeGvTokens.AccentOrange), Color(NightlifeGvTokens.ColorBgDeep), "Serra")
        City.Cariacica -> CityFilterStyle(Color(NightlifeGvTokens.AccentPurple), Color.White, "Cariacica")
    }
}

/** A2 — Horizontal City Filter Bar (design-system.md §4.2). */
@Composable
fun CityFilterBar(selected: City?, onSelect: (City) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NightlifeGvTokens.Space2Dp.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = NightlifeGvTokens.Space3Dp.dp, vertical = NightlifeGvTokens.Space2Dp.dp),
    ) {
        City.entries.forEach { city ->
            CityFilterPill(city = city, isActive = city == selected, onClick = { onSelect(city) })
        }
    }
}

@Composable
private fun CityFilterPill(city: City, isActive: Boolean, onClick: () -> Unit) {
    val style = CityFilterColors.styleFor(city)
    val backgroundColor = if (isActive) style.activeColor else style.activeColor.copy(alpha = InactiveTintAlpha)
    val textColor = if (isActive) style.activeTextColor else style.activeColor

    Text(
        text = style.label.uppercase(),
        color = textColor,
        fontWeight = FontWeight.SemiBold,
        fontSize = NightlifeGvTokens.TextBadge.SizeSp.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusPillDp.dp))
            .background(backgroundColor)
            .selectable(selected = isActive, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = NightlifeGvTokens.Space3Dp.dp, vertical = NightlifeGvTokens.Space2Dp.dp),
    )
}
