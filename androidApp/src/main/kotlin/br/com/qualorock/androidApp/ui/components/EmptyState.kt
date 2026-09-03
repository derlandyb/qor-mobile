package br.com.qualorock.androidApp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import design.QualORockThemeTokens

/** A4 — design-system-consistent empty state for event-discovery edge cases (empty list). */
@Composable
fun EmptyState(message: String = stringResource(R.string.empty_state_no_events), modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(QualORockThemeTokens.Space6Dp.dp),
    ) {
        Text(
            text = message,
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            textAlign = TextAlign.Center,
        )
    }
}
