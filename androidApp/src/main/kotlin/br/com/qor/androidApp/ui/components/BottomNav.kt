package br.com.qor.androidApp.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.NightlifeGvTokens

/**
 * A3 — MVP Core's bottom-nav destinations. `Favoritos` renders as a disabled stub per
 * mobile.md's A3 scope note: the favoriting action itself is Milestone 2 (Social &
 * Notifications, A20) — the tab exists here only for nav-shell completeness.
 */
enum class BottomNavDestination(val label: String, val enabled: Boolean) {
    Inicio("Início", enabled = true),
    Explorar("Explorar", enabled = true),
    Favoritos("Favoritos", enabled = false),
    Perfil("Perfil", enabled = true),
}

private const val UnderlineHeightDp = 2

/** A3 — fixed bottom navigation, design-system.md active-state icon + accent-color underline. */
@Composable
fun BottomNav(current: BottomNavDestination, onSelect: (BottomNavDestination) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(NightlifeGvTokens.ColorSurfaceCard)),
    ) {
        BottomNavDestination.entries.forEach { destination ->
            BottomNavItem(destination = destination, isSelected = destination == current, onSelect = onSelect)
        }
    }
}

/**
 * An equal [RowScope.weight] slice per item — without it, the first item's `fillMaxWidth()`
 * underline claims the Row's entire width, leaving every later sibling zero width (a classic
 * Compose Row-measurement pitfall, caught by `BottomNavTest`'s click-on-a-later-item case).
 */
@Composable
private fun RowScope.BottomNavItem(destination: BottomNavDestination, isSelected: Boolean, onSelect: (BottomNavDestination) -> Unit) {
    val accent = Color(NightlifeGvTokens.AccentPink)
    val textColor = when {
        !destination.enabled -> Color(NightlifeGvTokens.ColorTextTertiary)
        isSelected -> accent
        else -> Color(NightlifeGvTokens.ColorTextSecondary)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = NightlifeGvTokens.Space2Dp.dp)
            .selectable(
                selected = isSelected,
                enabled = destination.enabled,
                onClick = { onSelect(destination) },
            )
            .semantics { if (!destination.enabled) disabled() },
    ) {
        Text(
            text = destination.label,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = NightlifeGvTokens.TextMetadata.SizeSp.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(UnderlineHeightDp.dp)
                .background(if (isSelected) accent else Color.Transparent),
        )
    }
}
