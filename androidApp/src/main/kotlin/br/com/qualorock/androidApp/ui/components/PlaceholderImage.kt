package br.com.qualorock.androidApp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.qualorock.androidApp.R
import design.QualORockThemeTokens

/** A4 — design-system-consistent placeholder for an event with no `coverImageUrl` (`Event.coverImageUrl == null`). */
@Composable
fun PlaceholderImage(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.content_description_no_flyer)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(QualORockThemeTokens.RadiusImageDp.dp))
            .background(Color(QualORockThemeTokens.ColorBgBase))
            .semantics { contentDescription = description },
    )
}
