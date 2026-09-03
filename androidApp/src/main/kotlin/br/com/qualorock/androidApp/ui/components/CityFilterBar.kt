package br.com.qualorock.androidApp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import design.QualORockThemeTokens
import domain.enum.City

/** design-system.md §4.2's per-hub badge treatment: active/inactive colors + the pt-BR hub label. */
data class CityFilterStyle(
    val activeColor: Color,
    val activeTextColor: Color,
    @param:StringRes val labelRes: Int,
)

private const val InactiveTintAlpha = 0.15f

/** A2 — the 4-hub color table from design-system.md §4.2, keyed by [City]. */
object CityFilterColors {
    fun styleFor(city: City): CityFilterStyle = when (city) {
        City.Vitoria -> CityFilterStyle(Color(QualORockThemeTokens.AccentPink), Color(QualORockThemeTokens.ColorBgDeep), R.string.city_vitoria)
        City.VilaVelha -> CityFilterStyle(Color(QualORockThemeTokens.AccentBlue), Color(QualORockThemeTokens.ColorBgDeep), R.string.city_vila_velha)
        City.Serra -> CityFilterStyle(Color(QualORockThemeTokens.AccentOrange), Color(QualORockThemeTokens.ColorBgDeep), R.string.city_serra)
        City.Cariacica -> CityFilterStyle(Color(QualORockThemeTokens.AccentPurple), Color.White, R.string.city_cariacica)
    }
}

/** A2 — Horizontal City Filter Bar (design-system.md §4.2). */
@Composable
fun CityFilterBar(selected: City?, onSelect: (City) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
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
        text = stringResource(style.labelRes).uppercase(),
        color = textColor,
        fontWeight = FontWeight.SemiBold,
        fontSize = QualORockThemeTokens.TextBadge.SizeSp.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(QualORockThemeTokens.RadiusPillDp.dp))
            .background(backgroundColor)
            .selectable(selected = isActive, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
    )
}
