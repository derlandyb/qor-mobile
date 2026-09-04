package br.com.qualorock.androidApp.ui.nav

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.BottomNav
import br.com.qualorock.androidApp.ui.components.BottomNavDestination
import br.com.qualorock.androidApp.ui.screen.EmailVerificationScreen
import br.com.qualorock.androidApp.ui.screen.EventDetailScreen
import br.com.qualorock.androidApp.ui.screen.ExploreScreen
import br.com.qualorock.androidApp.ui.screen.HomeFeedScreen
import br.com.qualorock.androidApp.ui.screen.LoginScreen
import br.com.qualorock.androidApp.ui.screen.PasswordRecoveryScreen
import br.com.qualorock.androidApp.ui.screen.ProfileScreen
import br.com.qualorock.androidApp.ui.screen.SignupScreen
import data.SessionStore
import design.QualORockThemeTokens

/**
 * A14 — the full MVP Core route table, wiring together every screen built in A7-A13 behind one
 * `NavHost`. Two sub-graphs:
 *
 * - **Unauthenticated**: [Routes.Login] &lt;-&gt; [Routes.Signup] &lt;-&gt; [Routes.PasswordRecovery],
 *   with Login/Signup pushing [Routes.EmailVerification] on their success/unverified paths.
 * - **Authenticated**: [Routes.Home]/[Routes.Explore]/[Routes.Profile] behind [BottomNav], each
 *   `Scaffold`-wrapped individually (no nested `NavHost`) rather than sharing one — the three tab
 *   roots are peer destinations of the same top-level graph, popping/restoring state via
 *   `popUpTo`/`saveState` the same way Navigation's own bottom-nav sample does.
 *
 * [Routes.EmailVerification] is reachable from both sub-graphs (AUTH-10's "unverified account,
 * resend" path from Login, A8's post-signup path, and A13's "change e-mail" path from Profile) —
 * see [EmailVerificationReturnTo] for how the single screen/route disambiguates where `onVerified`
 * sends the fan back to, without needing two near-duplicate routes.
 *
 * **Startup session restore (AUTH-12).** [SessionStore.restore] is invoked once, before the
 * `NavHost`'s `startDestination` is decided, so the fan never flashes Login before landing on Home
 * — see [RestoreState].
 */
@Composable
fun QorNavGraph(sessionStore: SessionStore, navController: NavHostController = rememberNavController()) {
    var restoreState by remember { mutableStateOf<RestoreState>(RestoreState.Loading) }

    LaunchedEffect(sessionStore) {
        sessionStore.restore()
        restoreState = if (sessionStore.currentUser.value != null) {
            RestoreState.Authenticated
        } else {
            RestoreState.Unauthenticated
        }
    }

    when (val state = restoreState) {
        RestoreState.Loading -> StartupLoading()
        is RestoreState.Resolved -> NavHost(navController = navController, startDestination = state.startRoute) {
            unauthenticatedGraph(navController)
            authenticatedGraph(navController)
            emailVerificationDestination(navController)
            eventDetailDestination()
        }
    }
}

/** [RestoreState.Authenticated]/[RestoreState.Unauthenticated] share this so both resolve a [Resolved.startRoute]. */
private sealed class RestoreState {
    data object Loading : RestoreState()

    sealed class Resolved(val startRoute: String) : RestoreState()

    data object Authenticated : Resolved(Routes.Home)

    data object Unauthenticated : Resolved(Routes.Login)
}

@Composable
private fun StartupLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(QualORockThemeTokens.AccentPink))
        Text(
            text = stringResource(R.string.nav_startup_loading),
            color = Color(QualORockThemeTokens.ColorTextSecondary),
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
        )
    }
}

/** Route names + typed nav-arg helpers — one source of truth so no route string is hand-typed twice. */
private object Routes {
    const val Login = "login"
    const val Signup = "signup"
    const val PasswordRecovery = "password_recovery"
    const val Home = "home"
    const val Explore = "explore"
    const val Profile = "profile"

    const val EmailArg = "email"
    const val ReturnToArg = "returnTo"
    const val EmailVerification = "email_verification/{$EmailArg}/{$ReturnToArg}"

    fun emailVerification(email: String, returnTo: EmailVerificationReturnTo) =
        "email_verification/${Uri.encode(email)}/${returnTo.name}"

    const val EventIdArg = "eventId"
    const val EventDetail = "event_detail/{$EventIdArg}"

    fun eventDetail(eventId: String) = "event_detail/${Uri.encode(eventId)}"
}

/**
 * Disambiguates [EmailVerificationScreen]'s single `onVerified` return target: signup (A8/S12b's
 * documented "verifying does not log the fan in" -> Login) vs. a profile e-mail change (A13's
 * `ProfileEvent.EmailChangePending` -> back to Profile, already-authenticated). Passed as a
 * nav-graph string arg rather than two near-duplicate routes/composables — the screen and its args
 * are identical either way, only the "where next" differs.
 */
private enum class EmailVerificationReturnTo { Login, Profile }

private fun NavGraphBuilder.unauthenticatedGraph(navController: NavHostController) {
    composable(Routes.Login) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Routes.Home) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            },
            onNavigateToVerifyEmail = { email ->
                navController.navigate(Routes.emailVerification(email, EmailVerificationReturnTo.Login))
            },
            onNavigateToSignup = { navController.navigate(Routes.Signup) },
            onNavigateToPasswordRecovery = { navController.navigate(Routes.PasswordRecovery) },
        )
    }

    composable(Routes.Signup) {
        SignupScreen(
            onSignupSuccess = { email ->
                navController.navigate(Routes.emailVerification(email, EmailVerificationReturnTo.Login))
            },
            onNavigateToLogin = { navController.navigate(Routes.Login) },
        )
    }

    composable(Routes.PasswordRecovery) {
        PasswordRecoveryScreen(
            onResetSuccess = {
                navController.navigate(Routes.Login) {
                    popUpTo(Routes.PasswordRecovery) { inclusive = true }
                }
            },
            onNavigateToLogin = { navController.navigate(Routes.Login) },
        )
    }
}

private fun NavGraphBuilder.authenticatedGraph(navController: NavHostController) {
    composable(Routes.Home) {
        BottomNavScaffold(current = BottomNavDestination.Inicio, navController = navController) { padding ->
            HomeFeedScreen(
                onEventClick = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    composable(Routes.Explore) {
        BottomNavScaffold(current = BottomNavDestination.Explorar, navController = navController) { padding ->
            ExploreScreen(
                onEventClick = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    composable(Routes.Profile) {
        BottomNavScaffold(current = BottomNavDestination.Perfil, navController = navController) { padding ->
            ProfileScreen(
                onEmailChangePending = { newEmail ->
                    navController.navigate(Routes.emailVerification(newEmail, EmailVerificationReturnTo.Profile))
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

private fun NavGraphBuilder.emailVerificationDestination(navController: NavHostController) {
    composable(
        route = Routes.EmailVerification,
        arguments = listOf(
            navArgument(Routes.EmailArg) { type = NavType.StringType },
            navArgument(Routes.ReturnToArg) { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val email = backStackEntry.arguments?.getString(Routes.EmailArg).orEmpty()
        val returnTo = EmailVerificationReturnTo.valueOf(
            backStackEntry.arguments?.getString(Routes.ReturnToArg) ?: EmailVerificationReturnTo.Login.name,
        )

        EmailVerificationScreen(
            email = email,
            onVerified = {
                when (returnTo) {
                    EmailVerificationReturnTo.Profile -> navController.navigate(Routes.Profile) {
                        popUpTo(Routes.Profile) { inclusive = true }
                    }

                    EmailVerificationReturnTo.Login -> navController.navigate(Routes.Login) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            },
        )
    }
}

/**
 * A14 — DISC edge cases ("reached via a stale/direct link") require a shared event URL to open
 * straight into [EventDetailScreen], which already renders the correct Cancelled/Ended banner
 * (A13) once it loads. No canonical `https` domain exists yet anywhere in this project (`qor-api`,
 * `qor-website`, `.specs/`) to mirror as a verified Android App Link, so this registers the
 * custom-scheme deep link `qualorock://evento/{eventId}` instead of fabricating a domain — this
 * mechanically satisfies A14's "deep-link handling for shared event URLs" requirement without
 * claiming ownership of a real host. When a canonical `https` domain is chosen, add a second
 * [navDeepLink] here mirroring `qor-website`'s `/eventos/{id}` route, with `autoVerify="true"` and
 * a matching `assetlinks.json`.
 */
private fun NavGraphBuilder.eventDetailDestination() {
    composable(
        route = Routes.EventDetail,
        arguments = listOf(navArgument(Routes.EventIdArg) { type = NavType.StringType }),
        deepLinks = listOf(navDeepLink { uriPattern = "qualorock://evento/{${Routes.EventIdArg}}" }),
    ) { backStackEntry ->
        val eventId = backStackEntry.arguments?.getString(Routes.EventIdArg).orEmpty()
        EventDetailScreen(eventId = eventId)
    }
}

/**
 * Shared `Scaffold` + [BottomNav] chrome for the three tab-root destinations (safe default per
 * A14's brief: `EventDetail` and every auth screen render full-screen, without the bar).
 * `onSelect` reuses `popUpTo(Routes.Home) { saveState = true }` + `launchSingleTop`/`restoreState`
 * — the standard Navigation-Compose bottom-bar pattern — so switching tabs doesn't pile up
 * duplicate back-stack entries. `Favoritos` never reaches `onSelect` at all: [BottomNav] itself
 * renders it `enabled = false` (Milestone-2 stub, per A3), so no route is registered for it here.
 */
@Composable
private fun BottomNavScaffold(
    current: BottomNavDestination,
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = {
            BottomNav(
                current = current,
                onSelect = { destination ->
                    val route = when (destination) {
                        BottomNavDestination.Inicio -> Routes.Home
                        BottomNavDestination.Explorar -> Routes.Explore
                        BottomNavDestination.Perfil -> Routes.Profile
                        BottomNavDestination.Favoritos -> null
                    }
                    if (route != null && destination != current) {
                        navController.navigate(route) {
                            popUpTo(Routes.Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { padding -> content(padding) }
}
