package com.qualorock.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    composable(FEED_ROUTE) {
                        val holder: EventFeedViewModelHolder = viewModel()
                        val state by holder.eventFeedViewModel.state.collectAsState()

                        EventFeedScreen(
                            state = state,
                            onLoadNextPage = holder.eventFeedViewModel::loadNextPage,
                            onRetry = holder.eventFeedViewModel::retry,
                            onEventClick = { event -> navController.navigate("detail/${event.id}") },
                            onFavoriteClick = { /* favorites is a separate feature; no-op placeholder */ },
                            onShareClick = { /* sharing is a separate feature; no-op placeholder */ },
                            modifier = Modifier.statusBarsPadding(),
                        )
                    }
                    composable(
                        DETAIL_ROUTE,
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
                    ) {
                        val holder: EventDetailViewModelHolder = viewModel(factory = EventDetailViewModelHolder.Factory)
                        val state by holder.eventDetailViewModel.state.collectAsState()

                        EventDetailScreen(
                            state = state,
                            onRetry = holder.eventDetailViewModel::retry,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
