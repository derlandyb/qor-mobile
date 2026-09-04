package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.CityFilterBar
import br.com.qualorock.androidApp.ui.components.EmptyState
import br.com.qualorock.androidApp.ui.components.EventCard
import br.com.qualorock.androidApp.ui.components.GenreTag
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.components.SecondaryButton
import br.com.qualorock.androidApp.ui.viewmodel.ExploreViewModel
import br.com.qualorock.androidApp.ui.viewmodel.HomeFeedUiState
import design.QualORockThemeTokens
import domain.event.Event
import org.koin.androidx.compose.koinViewModel

/** How many items from the end of the list trigger [ExploreViewModel.onLoadMore] (DISC-18), same threshold as A11. */
private const val LoadMoreThresholdItems = 3

/**
 * A12 — placeholder genre catalog (DISC-15). `Event.genre` is a raw API string with no
 * genre-lookup endpoint yet (ARCHITECTURE §14's DB-backed lookup table isn't exposed to clients
 * yet — same gap [br.com.qualorock.androidApp.ui.components.GenreTagColors]'s KDoc documents), so
 * this mirrors that object's own known-genre set rather than inventing a fetch call.
 */
private val PlaceholderGenres = listOf("Rock", "Samba", "Sertanejo", "Eletrônico", "Reggae")

/**
 * A12 — Explore tab (DISC-14–DISC-18): [HomeFeedScreen]'s list surface with [CityFilterBar]/a
 * genre filter row added, per the task breakdown's own note that this is the same list surface as
 * the public feed, just as a separate BottomNav destination. Delegates all list rendering to the
 * same [HomeFeedUiState] shape [HomeFeedScreen] renders — see [ExploreViewModel]'s KDoc for why no
 * separate UI-state type was introduced. Navigation on card tap is A14's job, same as A11.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onEventClick: (String) -> Unit,
    onMapClick: (Event) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val hasActiveFilters = selectedCity != null || selectedGenre != null
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val state = uiState
                if (state is HomeFeedUiState.Content &&
                    lastVisibleIndex != null &&
                    lastVisibleIndex >= state.events.size - LoadMoreThresholdItems
                ) {
                    viewModel.onLoadMore()
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        CityFilterBar(selected = selectedCity, onSelect = viewModel::onCitySelected)
        GenreFilterRow(selected = selectedGenre, onSelect = viewModel::onGenreSelected)

        when (val state = uiState) {
            HomeFeedUiState.Loading -> LoadingIndicator(Modifier.fillMaxSize())

            HomeFeedUiState.Empty -> ExploreEmptyState(
                hasActiveFilters = hasActiveFilters,
                onClearFilters = viewModel::onClearFilters,
                modifier = Modifier.fillMaxSize(),
            )

            HomeFeedUiState.Error -> ErrorState(onRetry = viewModel::onRefresh, modifier = Modifier.fillMaxSize())

            is HomeFeedUiState.Content -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space4Dp.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(QualORockThemeTokens.Space4Dp.dp),
                ) {
                    items(state.events, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event.id) },
                            onMapClick = { onMapClick(event) },
                        )
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(QualORockThemeTokens.AccentPink))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** DISC-15 — a horizontally-scrolling, single-select row of [GenreTag] chips over [PlaceholderGenres]. */
@Composable
private fun GenreFilterRow(selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
    ) {
        PlaceholderGenres.forEach { genre ->
            val isActive = genre == selected
            val selectionBorder = if (isActive) {
                Modifier.border(
                    width = QualORockThemeTokens.BorderWidthHairlineDp.dp,
                    color = Color(QualORockThemeTokens.AccentPink),
                    shape = RoundedCornerShape(QualORockThemeTokens.RadiusSmDp.dp),
                )
            } else {
                Modifier
            }

            GenreTag(
                genre = genre,
                modifier = Modifier
                    .selectable(selected = isActive, onClick = { onSelect(genre) }, role = Role.RadioButton)
                    .then(selectionBorder),
            )
        }
    }
}

@Composable
private fun ExploreEmptyState(hasActiveFilters: Boolean, onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space3Dp.dp),
        ) {
            EmptyState(
                message = if (hasActiveFilters) {
                    stringResource(R.string.explore_empty_state_no_matches)
                } else {
                    stringResource(R.string.empty_state_no_events)
                },
            )

            if (hasActiveFilters) {
                SecondaryButton(text = stringResource(R.string.explore_cta_limpar_filtros), onClick = onClearFilters)
            }
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(QualORockThemeTokens.AccentPink))
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space3Dp.dp),
        modifier = modifier.padding(QualORockThemeTokens.Space6Dp.dp),
    ) {
        Text(
            text = stringResource(R.string.home_feed_error_message),
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        PrimaryButton(
            text = stringResource(R.string.home_feed_cta_tentar_novamente),
            onClick = onRetry,
        )
    }
}
