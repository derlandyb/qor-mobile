package com.qualorock.android.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qualorock.shared.map.MapDisplayState

/**
 * Non-blocking banner overlaid on the map for states that aren't "here are some markers" — loading, a load
 * failure (MAP-007/012, retryable), an empty viewport (MAP-006/011), or zero filter matches (MAP-003, offers
 * "Limpar filtros"). None of these force-move the camera.
 */
@Composable
fun MapLoadingBanner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier.testTag("map_loading_state"))
}

@Composable
fun MapErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp).testTag("map_error_state")) {
        Text(text = message)
        Button(onClick = onRetry, modifier = Modifier.testTag("map_retry_button")) {
            Text("Tentar novamente")
        }
    }
}

@Composable
fun MapDisplayStateBanner(
    displayState: MapDisplayState,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (displayState) {
        is MapDisplayState.Markers -> Unit
        is MapDisplayState.EmptyViewport ->
            Column(modifier = modifier.padding(16.dp).testTag("empty_viewport_state")) {
                Text("Nenhum evento nesta área.")
            }
        is MapDisplayState.NoFilterResults ->
            Column(modifier = modifier.padding(16.dp).testTag("no_filter_results_state")) {
                Text("Nenhum evento corresponde aos filtros ativos.")
                if (displayState.canClear) {
                    Button(onClick = onClearFilters, modifier = Modifier.testTag("clear_filters_button")) {
                        Text("Limpar filtros")
                    }
                }
            }
    }
}
