package com.qualorock.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qualorock.android.detail.EventDetailScreen
import com.qualorock.android.detail.EventDetailViewModelHolder
import com.qualorock.android.feed.EventFeedScreen
import com.qualorock.android.feed.EventFeedViewModelHolder
import com.qualorock.android.ui.theme.QualORockTheme

private const val FEED_ROUTE = "feed"
private const val DETAIL_ROUTE = "detail/{eventId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QualORockTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = FEED_ROUTE,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(FEED_ROUTE) { FeedRoute(navController) }
                    composable(
                        DETAIL_ROUTE,
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
                    ) { DetailRoute(navController) }
                }
            }
        }
    }
}

@Composable
private fun FeedRoute(navController: NavController) {
    val holder: EventFeedViewModelHolder = viewModel()
    val state by holder.eventFeedViewModel.state.collectAsState()
    val query by holder.searchViewModel.query.collectAsState()
    val filterState by holder.filterViewModel.state.collectAsState()
    val genreOptions by holder.filterViewModel.genreOptions.collectAsState()
    val artistOptions by holder.filterViewModel.artistOptions.collectAsState()
    val resultsState by holder.feedQueryViewModel.resultsState.collectAsState()

    EventFeedScreen(
        state = state,
        onLoadNextPage = holder.eventFeedViewModel::loadNextPage,
        onRetry = holder.eventFeedViewModel::retry,
        onEventClick = { event -> navController.navigate("detail/${event.id}") },
        onFavoriteClick = { /* favorites is a separate feature; no-op placeholder */ },
        onShareClick = { /* sharing is a separate feature; no-op placeholder */ },
        modifier = Modifier.statusBarsPadding(),
        query = query,
        onQueryChange = { holder.searchViewModel.query.value = it },
        onClearQuery = { holder.searchViewModel.query.value = "" },
        filterState = filterState,
        genreOptions = genreOptions,
        artistOptions = artistOptions,
        onDateSelect = holder.filterViewModel::selectDateBucket,
        onCitySelect = holder.filterViewModel::selectCity,
        onToggleGenre = holder.filterViewModel::toggleGenre,
        onSelectArtist = holder.filterViewModel::selectArtist,
        onRemoveChip = holder.filterViewModel::removeChip,
        onClearAllFilters = {
            holder.filterViewModel.clearAll()
            holder.searchViewModel.query.value = ""
        },
        resultsState = resultsState,
        onRetryResults = holder.feedQueryViewModel::retry,
    )
}

@Composable
private fun DetailRoute(navController: NavController) {
    val holder: EventDetailViewModelHolder = viewModel(factory = EventDetailViewModelHolder.Factory)
    val state by holder.eventDetailViewModel.state.collectAsState()

    EventDetailScreen(
        state = state,
        onRetry = holder.eventDetailViewModel::retry,
        onBack = { navController.popBackStack() },
    )
}
