package br.com.qor.androidApp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.NightlifeGvTokens

private const val DefaultMessage = "Nenhum evento encontrado"

/** A4 — design-system-consistent empty state for event-discovery edge cases (empty list). */
@Composable
fun EmptyState(message: String = DefaultMessage, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NightlifeGvTokens.Space2Dp.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(NightlifeGvTokens.Space6Dp.dp),
    ) {
        Text(
            text = message,
            color = Color(NightlifeGvTokens.ColorTextSecondary),
            fontSize = NightlifeGvTokens.TextBody.SizeSp.sp,
            textAlign = TextAlign.Center,
        )
    }
}
