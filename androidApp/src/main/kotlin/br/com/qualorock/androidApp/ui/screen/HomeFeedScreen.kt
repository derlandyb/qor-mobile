package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.CityFilterColors
import br.com.qualorock.androidApp.ui.components.EmptyState
import br.com.qualorock.androidApp.ui.components.EventCard
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.viewmodel.HomeFeedUiState
import br.com.qualorock.androidApp.ui.viewmodel.HomeFeedViewModel
import design.QualORockThemeTokens
import domain.enum.City
import domain.event.Event
import org.koin.androidx.compose.koinViewModel

/** How many items from the end of the list trigger [HomeFeedViewModel.onLoadMore] (DISC-02). */
private const val LoadMoreThresholdItems = 3

/**
 * A11 — the public event feed (DISC-01–DISC-06), no login required. Owns only rendering + scroll
 * pagination + pull-to-refresh wiring; navigation on card tap is A14's job, not this screen's —
 * [onEventClick] and [onMapClick] stay plain callbacks, same navigation-agnostic design as
 * A7-A10's screens.
 *
 * [onMapClick] defaults to a no-op: `EventCard` requires a map-CTA callback (design-system.md
 * §4.1's "Ver no Mapa"), but building the actual maps deep link from [Event.address] is left to
 * whoever wires this screen into the nav graph (A14), not this screen's own concern.
 *
 * nightlife-gv-stitch-refresh T27 restyled per Stitch mobile screen
 * `921f7cd5342f444ea0f27def28ef6b33`, "Mobile App Homepage" — adds the mock's "Eventos em
 * Destaque" section heading above the card list. The mock's city-filter chips and its Mapa/
 * Salvos bottom-nav tabs belong to the not-yet-built Explore/Map/Favoritos screens (separate
 * tasks), not this task's refresh scope.
 *
 * T33 (HUB-02) adds [onHubClick]: a "Hubs da Grande Vitória" entry-point row of the 4 [City]
 * chips above the card list, reusing [CityFilterColors] for the same per-city styling
 * `CityFilterBar` already uses. This is additive alongside `ExploreScreen`'s existing
 * `CityFilterBar`→filtered-list link, not a replacement of it (design.md's "Hubs vs. Explore"
 * tech decision — Hubs is a distinct curated surface, `/eventos`-equivalent stays untouched).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    onEventClick: (String) -> Unit,
    onMapClick: (Event) -> Unit = {},
    onHubClick: (City) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeFeedViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
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

    when (val state = uiState) {
        HomeFeedUiState.Loading -> LoadingIndicator(modifier)

        HomeFeedUiState.Empty -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState()
        }

        HomeFeedUiState.Error -> ErrorState(onRetry = viewModel::onRefresh, modifier = modifier)

        is HomeFeedUiState.Content -> PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space4Dp.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(QualORockThemeTokens.Space4Dp.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.hub_section_title),
                        color = Color(QualORockThemeTokens.ColorTextPrimary),
                        fontWeight = FontWeight.Bold,
                        fontSize = QualORockThemeTokens.TextEventTitle.SizeSp.sp,
                    )
                }

                item {
                    HubEntryPointRow(onHubClick = onHubClick)
                }

                item {
                    Text(
                        text = stringResource(R.string.home_feed_section_title),
                        color = Color(QualORockThemeTokens.ColorTextPrimary),
                        fontWeight = FontWeight.Bold,
                        fontSize = QualORockThemeTokens.TextEventTitle.SizeSp.sp,
                    )
                }

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

/**
 * T33/HUB-02 — one pill per [City], reusing [CityFilterColors]'s per-city palette (same table
 * [br.com.qualorock.androidApp.ui.components.CityFilterBar] uses).
 */
@Composable
private fun HubEntryPointRow(onHubClick: (City) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        City.entries.forEach { city ->
            val style = CityFilterColors.styleFor(city)
            Text(
                text = stringResource(style.labelRes),
                color = style.activeTextColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = QualORockThemeTokens.TextBadge.SizeSp.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(QualORockThemeTokens.RadiusPillDp.dp))
                    .background(style.activeColor)
                    .selectable(selected = false, onClick = { onHubClick(city) })
                    .padding(horizontal = QualORockThemeTokens.Space3Dp.dp, vertical = QualORockThemeTokens.Space2Dp.dp),
            )
        }
    }
}

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
