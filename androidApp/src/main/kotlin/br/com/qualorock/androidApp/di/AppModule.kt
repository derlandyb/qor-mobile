package br.com.qualorock.androidApp.di

import br.com.qualorock.androidApp.ui.viewmodel.EmailVerificationViewModel
import br.com.qualorock.androidApp.ui.viewmodel.LoginViewModel
import br.com.qualorock.androidApp.ui.viewmodel.SignupViewModel
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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * A1 — wires `shared`'s repositories/use cases into Koin so Android UI code (screens/ViewModels
 * built in later A-tasks) can `get()`/`koinInject()` them instead of constructing this graph by
 * hand per screen. A7 adds the first ViewModel registration (`LoginViewModel`).
 */
val appModule = module {
    single { createSecureTokenStorage() }
    single { createAuthenticatedHttpClient(createQorHttpClient(), get()) }

    single<EventRepository> { EventRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }

    single { SessionStore(get(), get()) } bind SessionWriter::class

    single { ListUpcomingEvents(get()) }
    single { GetEventDetails(get()) }
    single { PollingCoordinator(get()) }

    single { AuthenticateFan(get(), get()) }
    single { RegisterFan(get()) }
    single { ResetPassword(get()) }
    single { VerifyEmail(get()) }
    single { UpdateProfile(get()) }
    single { ExerciseDataRight(get()) }

    viewModel { LoginViewModel(get()) }
    viewModel { SignupViewModel(get()) }
    viewModel { EmailVerificationViewModel(get()) }
}
