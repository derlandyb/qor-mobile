package br.com.qualorock.androidApp.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.EventCardMotion
import br.com.qualorock.androidApp.ui.components.GenreTag
import br.com.qualorock.androidApp.ui.components.InstagramCta
import br.com.qualorock.androidApp.ui.components.PlaceholderImage
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.components.SecondaryButton
import br.com.qualorock.androidApp.ui.components.formatDateBadge
import br.com.qualorock.androidApp.ui.components.formatEventTime
import br.com.qualorock.androidApp.ui.viewmodel.EventDetailUiState
import br.com.qualorock.androidApp.ui.viewmodel.EventDetailViewModel
import design.QualORockThemeTokens
import domain.event.Event
import domain.event.EventDetail
import domain.event.EventPromoterContact
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * A13 — event detail (DISC-07–DISC-13). No login required. Branches over [EventDetail]'s three
 * subtypes: [EventDetail.Cancelled]/[EventDetail.Ended] render a status banner only (DISC-07),
 * [EventDetail.Active] renders the full content — description, date/time, address, genre,
 * free/paid indicator, a ticket-link button (paid only, DISC-08/09), an "abrir no mapa" button,
 * a per-promoter contact list (DISC-11), and a native share action (DISC-12).
 *
 * **Map handling (DISC-10):** no Maps SDK dependency exists anywhere in this app yet
 * (`androidApp/build.gradle.kts` has none), and adding one is out of this task's scope. Rather
 * than embedding a real map view, "abrir no mapa" opens [Event.address] in the device's default
 * maps app via an implicit `geo:` [Intent] — no new dependency, and the fan still gets turn-by-turn
 * navigation, which is arguably more useful than a static embedded map for this use case.
 *
 * **DISC-13 (accessibility info/event rules/notes) is skipped.** Neither [Event] nor
 * [EventDetail] exposes any such field (see `shared/src/commonMain/kotlin/domain/event/Event.kt`)
 * — inventing one client-side would mean rendering a field the API contract doesn't provide.
 *
 * **[launchIntent] is an injectable seam, not just a convenience default.** Every platform action
 * this screen performs (ticket link, map, share, promoter contact links) ultimately reduces to
 * "hand a [Context] and an [Intent] to `startActivity`" — factoring that one call out behind a
 * parameter (default: the real `context.startActivity(intent)`) lets tests exercise the
 * ticket-link failure path (DISC-08's "don't crash" requirement) deterministically, without
 * relying on Robolectric's shadow `ActivityManager` to reproduce a real `ActivityNotFoundException`.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
@Composable
fun EventDetailScreen(
    eventId: String,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = koinViewModel(),
    launchIntent: (Context, Intent) -> Unit = { context, intent -> context.startActivity(intent) },
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val ticketLinkErrorMessage = stringResource(R.string.event_detail_ticket_link_error)

    LaunchedEffect(eventId) {
        viewModel.load(eventId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(QualORockThemeTokens.ColorBgDeep),
        modifier = modifier,
    ) { paddingValues ->
        when (val state = uiState) {
            EventDetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(QualORockThemeTokens.AccentPink))
            }

            EventDetailUiState.Error -> ErrorState(
                onRetry = { viewModel.load(eventId) },
                modifier = Modifier.padding(paddingValues),
            )

            is EventDetailUiState.Content -> EventDetailContent(
                detail = state.detail,
                onOpenTicket = { url ->
                    try {
                        launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(ticketLinkErrorMessage) }
                    }
                },
                onOpenMap = { address ->
                    runSilently {
                        val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
                        launchIntent(context, Intent(Intent.ACTION_VIEW, geoUri))
                    }
                },
                onShare = { text ->
                    runSilently {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        launchIntent(context, Intent.createChooser(sendIntent, null))
                    }
                },
                onContactIntent = { intent -> runSilently { launchIntent(context, intent) } },
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Suppress("TooGenericExceptionCaught", "SwallowedException")
private fun runSilently(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        // Defensive only — DISC-10/DISC-12 carry no "don't crash" requirement of their own (unlike
        // the ticket link, DISC-08/09), but a missing maps/share-target app on the device must
        // still not crash this screen.
    }
}

@Composable
private fun EventDetailContent(
    detail: EventDetail,
    onOpenTicket: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onShare: (String) -> Unit,
    onContactIntent: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (detail) {
        is EventDetail.Cancelled -> StatusBanner(stringResource(R.string.event_detail_status_cancelled), modifier)
        is EventDetail.Ended -> StatusBanner(stringResource(R.string.event_detail_status_ended), modifier)
        is EventDetail.Active -> ActiveEventContent(detail, onOpenTicket, onOpenMap, onShare, onContactIntent, modifier)
    }
}

@Composable
private fun StatusBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(QualORockThemeTokens.Space6Dp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontWeight = FontWeight.SemiBold,
            fontSize = QualORockThemeTokens.TextEventTitle.SizeSp.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActiveEventContent(
    detail: EventDetail.Active,
    onOpenTicket: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onShare: (String) -> Unit,
    onContactIntent: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = detail.event
    val dateLabel = formatDateBadge(event.startsAt)
    val timeLabel = formatEventTime(event.startsAt)
    val shareText = stringResource(R.string.event_detail_share_text, event.title, event.address)

    Column(
        verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space4Dp.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(QualORockThemeTokens.Space4Dp.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(EventCardMotion.ImageAspectRatioWidth / EventCardMotion.ImageAspectRatioHeight)) {
            PlaceholderImage()
        }

        Text(
            text = event.title,
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.Bold,
            fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp)) {
            GenreTag(genre = event.genre)
            Text(
                text = stringResource(if (event.isFree) R.string.event_detail_label_free else R.string.event_detail_label_paid),
                color = Color(QualORockThemeTokens.ColorTextSecondary),
                fontWeight = FontWeight.SemiBold,
                fontSize = QualORockThemeTokens.TextBadge.SizeSp.sp,
            )
        }

        Text(
            text = "${dateLabel.day} ${dateLabel.month} · $timeLabel",
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
        )

        Text(
            text = event.address,
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.SemiBold,
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
        )

        SecondaryButton(
            text = stringResource(R.string.cta_abrir_no_mapa),
            onClick = { onOpenMap(event.address) },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = event.description,
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
        )

        if (!event.isFree) {
            val ticketUrl = event.ticketUrl
            if (ticketUrl != null) {
                PrimaryButton(
                    text = stringResource(R.string.cta_comprar_ingresso),
                    onClick = { onOpenTicket(ticketUrl) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (detail.promoters.isNotEmpty()) {
            Text(
                text = stringResource(R.string.event_detail_contacts_title),
                color = Color(QualORockThemeTokens.ColorTextPrimary),
                fontWeight = FontWeight.Bold,
                fontSize = QualORockThemeTokens.TextEventTitle.SizeSp.sp,
            )
            detail.promoters.forEach { contact ->
                PromoterContactRow(contact = contact, onContactIntent = onContactIntent)
            }
        }

        SecondaryButton(
            text = stringResource(R.string.cta_compartilhar),
            onClick = { onShare(shareText) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * DISC-11 — one tagged promoter's contact row. Each button is rendered only if its field is
 * present on [contact] (omitted, never shown blank) — [EventPromoterContact]'s own KDoc calls
 * this the "omit only the missing field" rule.
 */
@Composable
private fun PromoterContactRow(contact: EventPromoterContact, onContactIntent: (Intent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp)) {
        Text(
            text = contact.name,
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.SemiBold,
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp)) {
            contact.phone?.let { phone ->
                SecondaryButton(
                    text = stringResource(R.string.cta_ligar),
                    onClick = { onContactIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) },
                )
            }
            contact.email?.let { email ->
                SecondaryButton(
                    text = stringResource(R.string.cta_enviar_email),
                    onClick = { onContactIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) },
                )
            }
            contact.instagram?.let { handle ->
                InstagramCta(onClick = { onContactIntent(instagramIntent(handle)) })
            }
            contact.tiktok?.let { handle ->
                SecondaryButton(
                    text = stringResource(R.string.cta_tiktok),
                    onClick = { onContactIntent(tiktokIntent(handle)) },
                )
            }
        }
    }
}

/**
 * `EventPromoterContact.instagram`/`.tiktok` are raw handles per `api.md` T24 (no dedicated
 * profile-URL field), so the web-profile URL is built client-side — a leading "@" is stripped
 * defensively in case the API ever sends one.
 */
private fun instagramIntent(handle: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/${handle.removePrefix("@")}"))

private fun tiktokIntent(handle: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@${handle.removePrefix("@")}"))

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
            text = stringResource(R.string.event_detail_error_message),
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        PrimaryButton(
            text = stringResource(R.string.event_detail_cta_tentar_novamente),
            onClick = onRetry,
        )
    }
}
