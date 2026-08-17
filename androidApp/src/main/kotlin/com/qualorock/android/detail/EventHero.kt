package com.qualorock.android.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qualorock.android.R
import com.qualorock.shared.detail.PriceLineFormatter
import com.qualorock.shared.domain.Event
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Renders the ticket link near the date/price row, as a small secondary [TextButton]
 * — per event-details/design.md's Gap 1 correction (Save/Share stay the two prominent
 * actions in [ActionRow]; ticket is demoted here, not in the action row).
 */
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

            if (event.ticketUrl != null) {
                val context = LocalContext.current
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.ticketUrl))) },
                    modifier = Modifier.testTag("ticket_link"),
                ) {
                    Text(text = stringResource(id = R.string.detail_ticket_cta), style = MaterialTheme.typography.labelMedium)
                }
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
