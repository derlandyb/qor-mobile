package com.qualorock.android.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qualorock.shared.detail.PriceLineFormatter
import com.qualorock.shared.domain.Event
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun EventHero(
    event: Event,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (event.coverImageUrl != null) {
            AsyncImage(
                model = event.coverImageUrl,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        } else {
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.secondary),
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${event.venue.name} • ${event.city}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(text = formatEventDateTime(event), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = " · ${PriceLineFormatter.format(event.price)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun formatEventDateTime(event: Event): String {
    val localDateTime = event.startDateTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = localDateTime.date.dayOfMonth.toString().padStart(2, '0')
    val month = localDateTime.date.monthNumber.toString().padStart(2, '0')
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    return "$day/$month · $hour:$minute"
}
