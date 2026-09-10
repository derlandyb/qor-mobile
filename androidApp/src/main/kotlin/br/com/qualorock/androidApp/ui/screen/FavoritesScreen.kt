package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import br.com.qualorock.androidApp.ui.components.EmptyState
import br.com.qualorock.androidApp.ui.components.EventCard
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.viewmodel.FavoritesUiState
import br.com.qualorock.androidApp.ui.viewmodel.FavoritesViewModel
import design.QualORockThemeTokens
import org.koin.androidx.compose.koinViewModel

/**
 * T31 — Favoritos tab (FAVUI-01/02/04/05), styled per Stitch mobile screen
 * `2d9c61f8cc944edf9885957eac7dab44`, "Meus Favoritos (Mobile)": a title header over the same
 * [EventCard] used everywhere else, with a filled-heart affordance overlaid on each card
 * (mirroring the mock's top-right "remover dos favoritos" button) rather than modifying
 * [EventCard] itself — this task's `Reuses` note calls out [EventCard]/[EmptyState] as reused
 * as-is. The mock's genre-filter chip row and date-grouped section headings have no backing
 * data source here ([FavoritesViewModel] surfaces a flat list) and are left out of scope, the
 * same REFRESH-04 "don't invent a field the API doesn't provide" rule the existing-screen
 * refresh tasks (T24-T29) already followed.
 */
@Composable
fun FavoritesScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        FavoritesUiState.Loading -> LoadingIndicator(modifier)

        FavoritesUiState.Empty -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(message = stringResource(R.string.favorites_empty_state))
        }

        FavoritesUiState.Error -> ErrorState(onRetry = viewModel::load, modifier = modifier)

        is FavoritesUiState.Content -> Column(modifier = modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.favorites_title),
                color = Color(QualORockThemeTokens.ColorTextPrimary),
                fontWeight = FontWeight.Bold,
                fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
                modifier = Modifier.padding(QualORockThemeTokens.Space4Dp.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space4Dp.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = QualORockThemeTokens.Space4Dp.dp),
            ) {
                items(state.events, key = { it.id }) { event ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event.id) },
                            onMapClick = {},
                        )
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = stringResource(R.string.favorites_cta_remove, event.title),
                            tint = Color(QualORockThemeTokens.AccentPink),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(QualORockThemeTokens.Space3Dp.dp)
                                .clip(CircleShape)
                                .background(Color(QualORockThemeTokens.ColorBgDeep).copy(alpha = 0.8f))
                                .clickable(onClick = { viewModel.onUnfavorite(event.id) })
                                .padding(QualORockThemeTokens.Space2Dp.dp),
                        )
                    }
                }
            }
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
            text = stringResource(R.string.favorites_error_message),
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        PrimaryButton(
            text = stringResource(R.string.favorites_cta_tentar_novamente),
            onClick = onRetry,
        )
    }
}
