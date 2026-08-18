package com.qualorock.android.filters

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qualorock.android.R
import com.qualorock.shared.filters.ArtistOption
import com.qualorock.shared.filters.OptionsUiState

@Composable
fun GenreArtistPanel(
    genreOptions: OptionsUiState<String>,
    artistOptions: OptionsUiState<ArtistOption>,
    selectedGenres: Set<String>,
    selectedArtist: ArtistOption?,
    onToggleGenre: (String) -> Unit,
    onSelectArtist: (ArtistOption?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(id = R.string.filters_advanced_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        OptionSection(title = stringResource(id = R.string.filters_genre_title), options = genreOptions) {
            when (genreOptions) {
                is OptionsUiState.Loaded ->
                    OptionPillRow(genreOptions.options) { genre ->
                        FilterChip(
                            selected = genre in selectedGenres,
                            onClick = { onToggleGenre(genre) },
                            label = { Text(genre) },
                        )
                    }
                else -> Unit
            }
        }
        OptionSection(title = stringResource(id = R.string.filters_artist_title), options = artistOptions) {
            when (artistOptions) {
                is OptionsUiState.Loaded ->
                    OptionPillRow(artistOptions.options) { artist ->
                        FilterChip(
                            selected = selectedArtist?.id == artist.id,
                            onClick = { onSelectArtist(if (selectedArtist?.id == artist.id) null else artist) },
                            label = { Text(artist.name) },
                        )
                    }
                else -> Unit
            }
        }
    }
}

@Composable
private fun <T> OptionSection(
    title: String,
    options: OptionsUiState<T>,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp))
        when (options) {
            is OptionsUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            is OptionsUiState.Error ->
                Text(
                    text = stringResource(id = R.string.filters_options_error),
                    modifier = Modifier.padding(16.dp),
                )
            is OptionsUiState.Loaded ->
                if (options.options.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.filters_no_options),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    content()
                }
        }
    }
}

@Composable
private fun <T> OptionPillRow(
    options: List<T>,
    pill: @Composable (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option -> pill(option) }
    }
}
