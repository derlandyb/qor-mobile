package di

import data.EventRepositoryImpl
import data.SessionStore
import data.UserRepositoryImpl
import data.createAuthenticatedHttpClient
import data.createQorHttpClient
import data.createSecureTokenStorage
import domain.event.EventRepository
import domain.event.PollingCoordinator
import domain.event.usecase.GetEventDetails
import domain.event.usecase.ListUpcomingEvents
import domain.user.UserRepository
import domain.user.usecase.AuthenticateFan
import domain.user.usecase.ExerciseDataRight
import domain.user.usecase.RegisterFan
import domain.user.usecase.ResetPassword
import domain.user.usecase.SessionWriter
import domain.user.usecase.UpdateProfile
import domain.user.usecase.VerifyEmail
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * I1 — the platform-agnostic half of the app's Koin graph (repositories, use cases, session
 * state), moved here from `androidApp`'s original `AppModule` so both Android and iOS resolve
 * the exact same instances via the shared KMP module instead of each platform redeclaring the
 * same bindings. Each platform's own module (Android's `viewModelModule`, iOS's future
 * equivalent) only adds its UI-layer bindings (ViewModels) on top of this one.
 */
val sharedModule = module {
    single { createSecureTokenStorage() }
    single { createAuthenticatedHttpClient(createQorHttpClient(), get()) }

    single<EventRepository> { EventRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }

    single { SessionStore(get(), get()) } bind SessionWriter::class

    single { ListUpcomingEvents(get()) }
    single { GetEventDetails(get()) }

    // `factory`, not `single`: Home and Explore are separate BottomNav destinations that can
    // both be alive at once, each calling `PollingCoordinator.start` with its own city/genre
    // pair — a shared singleton would have them fight over the same poll state.
    factory { PollingCoordinator(get()) }

    single { AuthenticateFan(get(), get()) }
    single { RegisterFan(get()) }
    single { ResetPassword(get()) }
    single { VerifyEmail(get()) }
    single { UpdateProfile(get()) }
    single { ExerciseDataRight(get()) }
}
