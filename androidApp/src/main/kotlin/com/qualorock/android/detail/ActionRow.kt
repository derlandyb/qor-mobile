package com.qualorock.android.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.qualorock.android.AppConfig
import com.qualorock.android.R
import com.qualorock.shared.domain.Event

@Composable
fun ActionRow(
    event: Event,
    onShared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var saved by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FilledIconButton(onClick = { saved = !saved }, modifier = Modifier.testTag("action_save")) {
            Icon(
                imageVector = if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(id = R.string.detail_save),
            )
        }
        FilledIconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString("${AppConfig.API_BASE_URL}/compartilhar/eventos/${event.id}"))
                onShared()
            },
            modifier = Modifier.testTag("action_share"),
        ) {
            Icon(imageVector = Icons.Filled.Share, contentDescription = stringResource(id = R.string.detail_share))
        }

        if (event.ticketUrl != null) {
            val context = LocalContext.current
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.ticketUrl)))
                },
                modifier = Modifier.testTag("ticket_link"),
            ) {
                Text(text = stringResource(id = R.string.detail_ticket_cta), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
