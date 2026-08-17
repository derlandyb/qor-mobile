package com.qualorock.android.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qualorock.android.R

@Composable
fun DescriptionSection(
    description: String?,
    modifier: Modifier = Modifier,
) {
    if (description.isNullOrBlank()) return

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = stringResource(id = R.string.detail_about_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
