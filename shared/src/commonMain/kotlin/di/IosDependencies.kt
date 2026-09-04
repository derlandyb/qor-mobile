package di

import data.SessionStore
import domain.event.PollingCoordinator
import domain.event.usecase.GetEventDetails
import domain.event.usecase.ListUpcomingEvents
import domain.user.usecase.AuthenticateFan
import domain.user.usecase.ExerciseDataRight
import domain.user.usecase.RegisterFan
import domain.user.usecase.ResetPassword
import domain.user.usecase.UpdateProfile
import domain.user.usecase.VerifyEmail
import org.koin.mp.KoinPlatform

/**
 * I7 — typed Swift-callable accessors into [sharedModule]'s Koin graph. Generic `Koin.get<T>()`
 * calls don't bridge to Swift (no reified generics over Objective-C interop), so every use case
 * an iOS screen needs gets its own concrete accessor here — this is iOS's equivalent of
 * Android's own `viewModelModule` resolving the same [sharedModule] singletons, just exposed as
 * plain functions instead of a second DI graph, since Koin has no first-class Swift injection.
 */
object IosDependencies {
    private val koin get() = KoinPlatform.getKoin()

    fun authenticateFan(): AuthenticateFan = koin.get()
    fun registerFan(): RegisterFan = koin.get()
    fun verifyEmail(): VerifyEmail = koin.get()
    fun resetPassword(): ResetPassword = koin.get()
    fun updateProfile(): UpdateProfile = koin.get()
    fun exerciseDataRight(): ExerciseDataRight = koin.get()
    fun listUpcomingEvents(): ListUpcomingEvents = koin.get()
    fun getEventDetails(): GetEventDetails = koin.get()
    fun pollingCoordinator(): PollingCoordinator = koin.get()
    fun sessionStore(): SessionStore = koin.get()
}
