package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.CityFilterColors
import br.com.qualorock.androidApp.ui.components.EmptyState
import br.com.qualorock.androidApp.ui.components.EventCard
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.viewmodel.HubUiState
import br.com.qualorock.androidApp.ui.viewmodel.HubViewModel
import design.QualORockThemeTokens
import domain.enum.City
import org.koin.androidx.compose.koinViewModel

/**
 * T33 — one city's Hub (HUB-01..04), styled per Stitch mobile screen
 * `9d9e74eb48c943cabd714e195599f001`, "Hubs da Grande Vitória (Mobile)": a per-city header (city
 * name, in that city's [CityFilterColors] accent) over the same [EventCard] list used everywhere
 * else. [city] comes only from the nav-graph route arg the caller passes in — see
 * [HubViewModel]'s KDoc for why no stored city preference is ever consulted (the spec's own Edge
 * Case). The mock's "usar minha localização" CTA and per-city hero imagery have no backing data
 * source (`City` carries no image/geo field) and are out of scope, the same REFRESH-04 rule the
 * existing-screen refresh tasks already followed.
 */
@Composable
fun HubScreen(
    city: City,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HubViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(city) {
        viewModel.load(city)
    }

    when (val state = uiState) {
        HubUiState.Loading -> LoadingIndicator(modifier)

        HubUiState.Empty -> Column(modifier = modifier.fillMaxSize()) {
            HubHeader(city = city, modifier = Modifier.padding(QualORockThemeTokens.Space4Dp.dp))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState()
            }
        }

        HubUiState.Error -> ErrorState(onRetry = { viewModel.load(city) }, modifier = modifier)

        is HubUiState.Content -> Column(modifier = modifier.fillMaxSize()) {
            HubHeader(city = city, modifier = Modifier.padding(QualORockThemeTokens.Space4Dp.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space4Dp.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = QualORockThemeTokens.Space4Dp.dp),
            ) {
                items(state.events, key = { it.id }) { event ->
                    EventCard(event = event, onClick = { onEventClick(event.id) }, onMapClick = {})
                }
            }
        }
    }
}

@Composable
private fun HubHeader(city: City, modifier: Modifier = Modifier) {
    val style = CityFilterColors.styleFor(city)
    Text(
        text = stringResource(style.labelRes),
        color = style.activeColor,
        fontWeight = FontWeight.Bold,
        fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
        modifier = modifier,
    )
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
            text = stringResource(R.string.hub_error_message),
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        PrimaryButton(
            text = stringResource(R.string.hub_cta_tentar_novamente),
            onClick = onRetry,
        )
    }
}
