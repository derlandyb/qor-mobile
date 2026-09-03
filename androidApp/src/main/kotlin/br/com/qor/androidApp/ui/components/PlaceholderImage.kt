package br.com.qor.androidApp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import design.NightlifeGvTokens

/** A4 — design-system-consistent placeholder for an event with no `coverImageUrl` (`Event.coverImageUrl == null`). */
@Composable
fun PlaceholderImage(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(NightlifeGvTokens.RadiusImageDp.dp))
            .background(Color(NightlifeGvTokens.ColorBgBase))
            .semantics { contentDescription = "Sem imagem de divulgação" },
    )
}
