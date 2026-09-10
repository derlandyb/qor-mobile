package br.com.qualorock.androidApp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import design.QualORockThemeTokens

/**
 * A3 — MVP Core's bottom-nav destinations. `Favoritos` was a disabled stub per mobile.md's A3
 * scope note (the favoriting action itself was Milestone 2 work); T31 (nightlife-gv-stitch-refresh,
 * FAVUI-05) wires it to a real [br.com.qualorock.androidApp.ui.screen.FavoritesScreen] route, so
 * it's enabled here like every other tab.
 */
enum class BottomNavDestination(@param:StringRes val labelRes: Int, val icon: ImageVector, val enabled: Boolean) {
    Inicio(R.string.nav_inicio, Icons.Outlined.Home, enabled = true),
    Explorar(R.string.nav_explorar, Icons.Outlined.Explore, enabled = true),
    Favoritos(R.string.nav_favoritos, Icons.Outlined.FavoriteBorder, enabled = true),
    Perfil(R.string.nav_perfil, Icons.Outlined.Person, enabled = true),
}

private const val UnderlineHeightDp = 2

/** A3 — fixed bottom navigation, design-system.md active-state icon + accent-color underline. */
@Composable
fun BottomNav(current: BottomNavDestination, onSelect: (BottomNavDestination) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(QualORockThemeTokens.ColorSurfaceCard))
            .padding(bottom = QualORockThemeTokens.Space6Dp.dp),
    ) {
        BottomNavDestination.entries.forEach { destination ->
            BottomNavItem(destination = destination, icon = destination.icon, isSelected = destination == current, onSelect = onSelect)
        }
    }
}

/**
 * An equal [RowScope.weight] slice per item — without it, the first item's `fillMaxWidth()`
 * underline claims the Row's entire width, leaving every later sibling zero width (a classic
 * Compose Row-measurement pitfall, caught by `BottomNavTest`'s click-on-a-later-item case).
 */
@Composable
private fun RowScope.BottomNavItem(
    destination: BottomNavDestination,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: (BottomNavDestination) -> Unit,
) {
    val accent = Color(QualORockThemeTokens.AccentPink)
    val textColor = when {
        !destination.enabled -> Color(QualORockThemeTokens.ColorTextTertiary)
        isSelected -> accent
        else -> Color(QualORockThemeTokens.ColorTextSecondary)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = QualORockThemeTokens.Space2Dp.dp)
            .selectable(
                selected = isSelected,
                enabled = destination.enabled,
                onClick = { onSelect(destination) },
            )
            .semantics { if (!destination.enabled) disabled() },
    ) {
        // contentDescription null: decorative, the label is already carried by the Text
        // below — a non-null description here would make TalkBack announce it twice.
        Icon(icon, contentDescription = null, tint = if (isSelected) accent else textColor)
        Text(
            text = stringResource(destination.labelRes),
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(UnderlineHeightDp.dp)
                .background(if (isSelected) accent else Color.Transparent),
        )
    }
}
