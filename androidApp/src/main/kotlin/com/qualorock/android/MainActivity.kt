package com.qualorock.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qualorock.android.feed.EventFeedScreen
import com.qualorock.android.feed.EventFeedViewModelHolder
import com.qualorock.android.ui.theme.QualORockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QualORockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    val holder: EventFeedViewModelHolder = viewModel()
                    val state by holder.eventFeedViewModel.state.collectAsState()

                    EventFeedScreen(
                        state = state,
                        onLoadNextPage = holder.eventFeedViewModel::loadNextPage,
                        onRetry = holder.eventFeedViewModel::retry,
                        onEventClick = { /* event-details is a separate feature; no-op placeholder */ },
                        onFavoriteClick = { /* favorites is a separate feature; no-op placeholder */ },
                        onShareClick = { /* sharing is a separate feature; no-op placeholder */ },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}
