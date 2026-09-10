package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.EmptyState
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.viewmodel.MapUiState
import br.com.qualorock.androidApp.ui.viewmodel.MapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import design.QualORockThemeTokens
import domain.enum.City
import domain.event.MapBounds
import org.koin.androidx.compose.koinViewModel

// T32 (MAPUI-01/03) — Grande Vitória's approximate centroid, the initial camera before any pan/zoom re-query.
private const val GrandeVitoriaCentroidLat = -20.294
private const val GrandeVitoriaCentroidLng = -40.317
private const val InitialMapZoom = 11f

private val InitialMapCamera = CameraPosition.fromLatLngZoom(
    LatLng(GrandeVitoriaCentroidLat, GrandeVitoriaCentroidLng),
    InitialMapZoom,
)

/**
 * T32 — Map screen (MAPUI-01..04), extending [EventMapState.kt][toMapPins]'s single-pin pattern
 * (used by [EventDetailScreen]'s embedded per-event map) to a multi-pin `maps-compose` `GoogleMap`
 * over [MapViewModel]'s [MapUiState], styled per Stitch's "Mapa Interativo" mobile mock.
 *
 * Initial load defaults to [City.Vitoria] (city mode) since this app has no location-permission
 * flow yet to seed a real viewport bounding box; MAPUI-03's pan/zoom re-query then takes over via
 * [MapViewModel.loadByBounds] once the map reports a real viewport (`onMapLoaded`/camera-idle).
 *
 * **[toMapPins]'s [MapPin.event]->[onEventClick] wiring, not the `GoogleMap`/`Marker` render
 * itself, is this screen's unit-testable surface** — mirrors [EventDetailScreen]'s own
 * established boundary: that screen's `Located`/`GoogleMap` branch (real Play-Services map tiles)
 * is never exercised by its Robolectric render test either (only the pure
 * `geocodeAddress`->[EventMapState] mapping is), because `maps-compose`'s `GoogleMap` needs Play
 * Services internals Robolectric doesn't provide. [toMapPins] (pure) and [MapViewModel] (fake
 * repository) carry this screen's real test coverage instead.
 */
@Composable
fun MapScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadByCity(City.Vitoria)
    }

    when (val state = uiState) {
        MapUiState.Loading -> LoadingIndicator(modifier)

        MapUiState.Error -> ErrorState(onRetry = { viewModel.loadByCity(City.Vitoria) }, modifier = modifier)

        is MapUiState.Content -> Box(modifier = modifier.fillMaxSize()) {
            val cameraPositionState = rememberCameraPositionState { position = InitialMapCamera }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    cameraPositionState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                        viewModel.loadByBounds(bounds.toDomain())
                    }
                },
            ) {
                state.pins.forEach { pin ->
                    val position = LatLng(pin.point.latitude, pin.point.longitude)
                    Marker(
                        state = MarkerState(position = position),
                        title = pin.event.title,
                        snippet = pin.event.address,
                        onClick = {
                            onEventClick(pin.event.id)
                            true
                        },
                    )
                }
            }

            if (state.pins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(QualORockThemeTokens.Space4Dp.dp),
                ) {
                    EmptyState(message = stringResource(R.string.map_empty_state))
                }
            }
        }
    }
}

/** [LatLngBounds] (Google Maps SDK) -> [MapBounds] (`shared` domain value type, MAPUI-03). */
private fun LatLngBounds.toDomain(): MapBounds = MapBounds(
    north = northeast.latitude,
    south = southwest.latitude,
    east = northeast.longitude,
    west = southwest.longitude,
)

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(QualORockThemeTokens.AccentPink))
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space3Dp.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(QualORockThemeTokens.Space6Dp.dp),
    ) {
        Text(
            text = stringResource(R.string.map_error_message),
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        PrimaryButton(
            text = stringResource(R.string.map_cta_tentar_novamente),
            onClick = onRetry,
        )
    }
}
