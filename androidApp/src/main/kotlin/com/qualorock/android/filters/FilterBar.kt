package com.qualorock.android.filters

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qualorock.android.R
import com.qualorock.shared.filters.DateBucket
import com.qualorock.shared.filters.FilterState

private val CITY_OPTIONS = listOf("Vitória", "Vila Velha", "Serra", "Cariacica")

@Composable
fun FilterBar(
    state: FilterState,
    onDateSelect: (DateBucket?) -> Unit,
    onCitySelect: (String?) -> Unit,
    onOpenPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateBucket.entries.forEach { bucket ->
            FilterChip(
                selected = state.dateBucket == bucket,
                onClick = { onDateSelect(if (state.dateBucket == bucket) null else bucket) },
                label = { Text(bucket.label) },
            )
        }
        CITY_OPTIONS.forEach { city ->
            FilterChip(
                selected = state.city == city,
                onClick = { onCitySelect(if (state.city == city) null else city) },
                label = { Text(city) },
            )
        }
        AssistChip(
            onClick = onOpenPanel,
            label = { Text(stringResource(id = R.string.filters_button)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Tune, contentDescription = null) },
        )
    }
}
