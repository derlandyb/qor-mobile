package com.qualorock.android.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qualorock.android.R
import com.qualorock.shared.domain.BannerStatus

@Composable
fun StatusBanner(
    bannerStatus: BannerStatus?,
    modifier: Modifier = Modifier,
) {
    when (bannerStatus) {
        BannerStatus.CANCELLED ->
            Row(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(16.dp)
                        .testTag("status_banner_cancelled"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                Text(
                    text = stringResource(id = R.string.detail_cancelled_banner),
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        BannerStatus.FINISHED ->
            Row(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(16.dp)
                        .testTag("status_banner_finished"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(id = R.string.detail_finished_banner),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        null -> Unit
    }
}
