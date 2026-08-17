package com.qualorock.android.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.qualorock.android.R
import com.qualorock.shared.detail.LocationSectionVariant
import com.qualorock.shared.domain.Venue

@Composable
fun LocationSection(
    venue: Venue,
    modifier: Modifier = Modifier,
) {
    val variant = LocationSectionVariant.from(venue)
    if (variant is LocationSectionVariant.Omitted) return

    val context = LocalContext.current

    Column(modifier = modifier.padding(16.dp).testTag("location_section")) {
        Text(text = stringResource(id = R.string.detail_location_title), style = MaterialTheme.typography.titleMedium)

        val addressText =
            when (variant) {
                is LocationSectionVariant.Map -> {
                    AsyncImage(
                        model = variant.url,
                        contentDescription = stringResource(id = R.string.detail_location_title),
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 7f)
                                .clip(RoundedCornerShape(16.dp))
                                .padding(top = 8.dp)
                                .testTag("location_map"),
                    )
                    variant.address
                }
                is LocationSectionVariant.AddressOnly -> variant.address
                LocationSectionVariant.Omitted -> null
            }

        if (addressText != null) {
            Text(
                text = addressText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp).testTag("location_address"),
            )
        }

        if (venue.latitude != null && venue.longitude != null) {
            TextButton(
                onClick = {
                    val uri = Uri.parse("https://maps.google.com/?q=${venue.latitude},${venue.longitude}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.testTag("view_on_map"),
            ) {
                Text(text = stringResource(id = R.string.detail_view_on_map))
            }
        }
    }
}
