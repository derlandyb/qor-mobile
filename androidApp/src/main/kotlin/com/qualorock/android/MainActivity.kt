package com.qualorock.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qualorock.android.detail.EventDetailScreen
import com.qualorock.android.detail.EventDetailViewModelHolder
import com.qualorock.android.feed.EventFeedScreen
import com.qualorock.android.feed.EventFeedViewModelHolder
import com.qualorock.android.map.MapScreen
import com.qualorock.android.map.MapViewModelHolder
import com.qualorock.android.ui.theme.QualORockTheme
import com.qualorock.shared.filters.FilterViewModel

private const val FEED_ROUTE = "feed"
private const val MAP_ROUTE = "map"
private const val DETAIL_ROUTE = "detail/{eventId}"
private val TAB_ROUTES = listOf(FEED_ROUTE, MAP_ROUTE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QualORockTheme {
                // Activity-scoped (obtained outside the NavHost's per-route ViewModelStore) so the same
                // FilterViewModel instance backs both tabs — filters survive switching Feed <-> Map (MAP-003 AC2).
                val filterHolder: SharedFilterViewModelHolder = viewModel()
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = { MainTabBar(navController) },
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = FEED_ROUTE,
                        modifier = Modifier.fillMaxSize().padding(padding),
                    ) {
                        composable(FEED_ROUTE) { FeedRoute(navController, filterHolder.filterViewModel) }
                        composable(MAP_ROUTE) { MapRoute(navController, filterHolder.filterViewModel) }
                        composable(
                            DETAIL_ROUTE,
                            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
                        ) { DetailRoute(navController) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabBar(navController: NavController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == FEED_ROUTE } == true,
            onClick = { navigateToTab(navController, FEED_ROUTE) },
            icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.tab_feed)) },
            label = { Text(stringResource(R.string.tab_feed)) },
        )
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.route == MAP_ROUTE } == true,
            onClick = { navigateToTab(navController, MAP_ROUTE) },
            icon = { Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.tab_map)) },
            label = { Text(stringResource(R.string.tab_map)) },
        )
    }
}

private fun navigateToTab(
    navController: NavController,
    route: String,
) {
    if (route !in TAB_ROUTES) return
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FeedRoute(
    navController: NavController,
    filterViewModel: FilterViewModel,
) {
    val holder: EventFeedViewModelHolder = viewModel(factory = EventFeedViewModelHolder.factory(filterViewModel))
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
private fun MapRoute(
    navController: NavController,
    filterViewModel: FilterViewModel,
) {
    val holder: MapViewModelHolder = viewModel(factory = MapViewModelHolder.factory(filterViewModel))
    val markersState by holder.mapQueryViewModel.markersState.collectAsState()

    MapScreen(
        markersState = markersState,
        onOpenEventDetails = { eventId -> navController.navigate("detail/$eventId") },
        onRetry = holder.mapQueryViewModel::retry,
        onClearFilters = filterViewModel::clearAll,
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
