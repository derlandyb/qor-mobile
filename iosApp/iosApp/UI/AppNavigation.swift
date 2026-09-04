import SwiftUI
import shared

/// I14 — the full MVP Core route table, mirroring Android's `QorNavGraph` (A14): an
/// unauthenticated stack (Login <-> Signup <-> PasswordRecovery, both pushing
/// EmailVerification on success), and an authenticated area (Home/Explore/Profile behind
/// `BottomNav`, sharing one `NavigationStack` whose path handles pushed EventDetail/
/// EmailVerification-from-Profile destinations — SwiftUI has no direct `NavHost` equivalent,
/// so tab switching is a plain `@State` root swap under one stack rather than three nested
/// stacks, keeping one shared back-stack the same way Android's single `NavHost` does).
///
/// **Startup session restore (AUTH-12).** [SessionStore.restore] runs once, before deciding
/// the initial screen, so the fan never flashes Login before landing on Home — see
/// [RestoreState].
struct AppNavigation: View {
    private let sessionStore = IosDependencies.shared.sessionStore()

    @State private var restoreState: RestoreState = .loading
    @State private var authPath = NavigationPath()
    @State private var mainPath = NavigationPath()

    var body: some View {
        Group {
            switch restoreState {
            case .loading:
                StartupLoadingView()
            case .unauthenticated:
                unauthenticatedStack
            case .authenticated:
                authenticatedStack
            }
        }
        .task {
            try? await sessionStore.restore()
            restoreState = sessionStore.currentUser.value != nil ? .authenticated : .unauthenticated
        }
        .onOpenURL { url in
            guard let eventId = Self.eventId(from: url) else { return }
            if case .authenticated = restoreState {
                mainPath.append(AuthenticatedDestination.eventDetail(eventId))
            }
        }
    }

    /// Parses `qualorock://evento/{eventId}` — same custom-scheme deep link as Android's A14
    /// (`navDeepLink { uriPattern = "qualorock://evento/{eventId}" }`); no canonical `https`
    /// domain exists anywhere in this project yet to register as a Universal Link.
    static func eventId(from url: URL) -> String? {
        guard url.scheme == "qualorock", url.host == "evento" else { return nil }
        let id = url.path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return id.isEmpty ? nil : id
    }

    private var unauthenticatedStack: some View {
        NavigationStack(path: $authPath) {
            LoginView(
                onLoginSuccess: {
                    restoreState = .authenticated
                    authPath = NavigationPath()
                },
                onNavigateToVerifyEmail: { email in
                    authPath.append(AuthDestination.emailVerification(email: email))
                },
                onNavigateToSignup: { authPath.append(AuthDestination.signup) },
                onNavigateToPasswordRecovery: { authPath.append(AuthDestination.passwordRecovery) }
            )
            .navigationDestination(for: AuthDestination.self) { destination in
                switch destination {
                case .signup:
                    SignupView(
                        onSignupSuccess: { email in
                            authPath.append(AuthDestination.emailVerification(email: email))
                        },
                        onNavigateToLogin: { authPath = NavigationPath() }
                    )
                case .passwordRecovery:
                    PasswordRecoveryView(
                        onResetSuccess: { authPath = NavigationPath() },
                        onNavigateToLogin: { authPath = NavigationPath() }
                    )
                case let .emailVerification(email):
                    EmailVerificationView(
                        email: email,
                        context: .signup,
                        onVerifiedForSignup: { authPath = NavigationPath() },
                        onVerifiedForEmailChange: { authPath = NavigationPath() }
                    )
                }
            }
        }
    }

    private var authenticatedStack: some View {
        NavigationStack(path: $mainPath) {
            MainTabRootView(
                onEventClick: { eventId in mainPath.append(AuthenticatedDestination.eventDetail(eventId)) },
                onEmailChangePending: { email in
                    mainPath.append(AuthenticatedDestination.emailVerificationFromProfile(email: email))
                },
                onLogout: {
                    restoreState = .unauthenticated
                    mainPath = NavigationPath()
                }
            )
            .navigationDestination(for: AuthenticatedDestination.self) { destination in
                switch destination {
                case let .eventDetail(eventId):
                    EventDetailView(eventId: eventId, onBack: { mainPath.removeLast() })
                case let .emailVerificationFromProfile(email):
                    EmailVerificationView(
                        email: email,
                        context: .emailChange,
                        onVerifiedForSignup: { mainPath.removeLast() },
                        onVerifiedForEmailChange: { mainPath.removeLast() }
                    )
                }
            }
        }
    }
}

private enum RestoreState {
    case loading
    case unauthenticated
    case authenticated
}

private enum AuthDestination: Hashable {
    case signup
    case passwordRecovery
    case emailVerification(email: String)
}

private enum AuthenticatedDestination: Hashable {
    case eventDetail(String)
    case emailVerificationFromProfile(email: String)
}

private struct StartupLoadingView: View {
    var body: some View {
        VStack(spacing: QorSpace.space2) {
            ProgressView().tint(QorColor.accentPink)
            Text(String(localized: "nav_startup_loading"))
                .font(.system(size: CGFloat(QualORockThemeTokens.TextMetadata.shared.SizeSp)))
                .foregroundStyle(QorColor.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(QorColor.bgBase)
    }
}

/// Shared `BottomNav` chrome for the three tab-root destinations, mirroring Android's
/// `BottomNavScaffold` — `Favoritos` stays disabled (I3 stub, Milestone 2 scope).
struct MainTabRootView: View {
    let onEventClick: (String) -> Void
    let onEmailChangePending: (String) -> Void
    let onLogout: () -> Void

    @State private var current: BottomNavDestination

    init(
        initialTab: BottomNavDestination = .inicio,
        onEventClick: @escaping (String) -> Void,
        onEmailChangePending: @escaping (String) -> Void,
        onLogout: @escaping () -> Void
    ) {
        _current = State(initialValue: initialTab)
        self.onEventClick = onEventClick
        self.onEmailChangePending = onEmailChangePending
        self.onLogout = onLogout
    }

    var body: some View {
        VStack(spacing: 0) {
            Group {
                switch current {
                case .inicio:
                    HomeFeedView(onEventClick: onEventClick)
                case .explorar:
                    ExploreView(onEventClick: { event in onEventClick(event.id) })
                case .perfil:
                    ProfileView(onEmailChangePending: onEmailChangePending)
                case .favoritos:
                    EmptyView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            BottomNav(current: current, onSelect: { destination in
                if destination.enabled { current = destination }
            })
        }
    }
}
