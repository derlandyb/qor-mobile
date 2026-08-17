package com.qualorock.android.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.qualorock.android.R
import com.qualorock.shared.detail.EventDetailUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val shareCopiedMessage = stringResource(id = R.string.detail_share_copied)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.detail_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        when (state) {
            is EventDetailUiState.Loading ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is EventDetailUiState.NotFound ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).testTag("not_found_state"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(id = R.string.detail_not_found), style = MaterialTheme.typography.titleMedium)
                }

            is EventDetailUiState.LoadError ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = stringResource(id = R.string.detail_error_title), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onRetry, modifier = Modifier.testTag("retry_button")) {
                        Text(text = stringResource(id = R.string.detail_retry))
                    }
                }

            is EventDetailUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                    StatusBanner(bannerStatus = state.event.bannerStatus)
                    EventHero(event = state.event)
                    ActionRow(
                        event = state.event,
                        onShared = {
                            coroutineScope.launch { snackbarHostState.showSnackbar(shareCopiedMessage) }
                        },
                    )
                    DescriptionSection(description = state.event.description)
                    LocationSection(venue = state.event.venue)
                    PromoterSection(promoter = state.event.promoter)
                }
        }
    }
}
