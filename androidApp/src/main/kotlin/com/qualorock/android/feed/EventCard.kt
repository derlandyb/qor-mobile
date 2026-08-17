package com.qualorock.android.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qualorock.android.R
import com.qualorock.shared.domain.Event
import com.qualorock.shared.domain.EventStatus
import com.qualorock.shared.domain.Price
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EventCard(
    event: Event,
    onClick: (Event) -> Unit,
    onFavoriteClick: (Event) -> Unit,
    onShareClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onClick(event) },
        shape = RoundedCornerShape(16.dp),
    ) {
        Box {
            if (event.coverImageUrl != null) {
                AsyncImage(
                    model = event.coverImageUrl,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.secondary),
                )
            }

            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Column {
                    IconButton(onClick = { onFavoriteClick(event) }) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = "Favoritar",
                        )
                    }
                    IconButton(onClick = { onShareClick(event) }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Compartilhar",
                        )
                    }
                }
            }

            if (event.status == EventStatus.CANCELLED) {
                Text(
                    text = stringResourceCancelled(),
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .semantics { contentDescription = "Evento cancelado" },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.titleMedium)
            Text(text = event.venue.name, style = MaterialTheme.typography.bodyMedium)
            priceLabel(event.price)?.let { label ->
                Text(text = label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun priceLabel(price: Price?): String? =
    when {
        price == null -> null
        price.isFree -> "Grátis"
        price.min == null -> null
        else -> "A partir de ${NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(price.min)}"
    }

@Composable
private fun stringResourceCancelled(): String = androidx.compose.ui.res.stringResource(id = R.string.feed_cancelled)
