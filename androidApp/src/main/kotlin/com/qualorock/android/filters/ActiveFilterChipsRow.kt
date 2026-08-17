package com.qualorock.android.filters

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qualorock.android.R
import com.qualorock.shared.filters.FilterChip

private fun FilterChip.label(): String =
    when (this) {
        is FilterChip.DateChip -> bucket.label
        is FilterChip.CityChip -> city
        is FilterChip.GenreChip -> genres.joinToString(", ")
        is FilterChip.ArtistChip -> artist.name
    }

@Composable
fun ActiveFilterChipsRow(
    chips: List<FilterChip>,
    onRemove: (FilterChip) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            InputChip(
                selected = true,
                onClick = { onRemove(chip) },
                label = { Text(chip.label()) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remover filtro ${chip.label()}",
                    )
                },
            )
        }
        TextButton(onClick = onClearAll) {
            Text(stringResource(id = R.string.filters_clear_all))
        }
    }
}
