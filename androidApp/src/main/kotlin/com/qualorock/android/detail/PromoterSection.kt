package com.qualorock.android.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qualorock.shared.domain.Promoter
import com.qualorock.shared.domain.VerificationStatus

@Composable
fun PromoterSection(
    promoter: Promoter?,
    modifier: Modifier = Modifier,
) {
    if (promoter == null) return

    val context = LocalContext.current

    Row(modifier = modifier.padding(16.dp).testTag("promoter_section"), verticalAlignment = Alignment.CenterVertically) {
        Text(text = promoter.name, style = MaterialTheme.typography.titleMedium)
        if (promoter.verificationStatus == VerificationStatus.VERIFIED) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Verificado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        promoter.socialLinks?.get("instagram")?.let { instagramUrl ->
            IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(instagramUrl))) }) {
                Text(text = "IG")
            }
        }
        promoter.socialLinks?.get("whatsapp")?.let { whatsappUrl ->
            IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))) }) {
                Text(text = "WA")
            }
        }
    }
}
