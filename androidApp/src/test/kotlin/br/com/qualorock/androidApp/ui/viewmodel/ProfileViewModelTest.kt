package br.com.qualorock.androidApp.ui.viewmodel

import data.SecureTokenStorage
import data.SessionStore
import domain.enum.ConsentType
import domain.user.ConfirmResetResult
import domain.user.DataRightResult
import domain.user.LoginResult
import domain.user.ProfileUpdateFields
import domain.user.RegisterResult
import domain.user.User
import domain.user.UserRepository
import domain.user.usecase.UpdateProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-file [UserRepository] fake, same shape as the other A-task ViewModel tests' fakes —
 * `androidApp` only depends on `shared`'s main artifact, not `commonTest`.
 */
private class FakeProfileUserRepository(private val updateResult: Result<User>) : UserRepository {
    var lastUpdateFields: ProfileUpdateFields? = null

    override suspend fun register(
        email: String,
        password: String,
        birthdate: String,
        name: String,
        consentAccepted: Boolean,
    ): RegisterResult = error("not used by ProfileViewModelTest")

    override suspend fun login(email: String, password: String): LoginResult = error("not used by ProfileViewModelTest")
    override suspend fun loginWithGoogle(googleIdToken: String): LoginResult = error("not used by ProfileViewModelTest")
    override suspend fun logout() = Unit
    override suspend fun requestPasswordReset(email: String) = Unit
    override suspend fun verifyResetCode(email: String, code: String): domain.user.VerifyResetCodeResult =
        error("not used by ProfileViewModelTest")
    override suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): ConfirmResetResult =
        error("not used by ProfileViewModelTest")

    override suspend fun resendVerification(email: String) = Unit
    override suspend fun verifyEmailCode(email: String, code: String): domain.user.VerifyEmailResult =
        error("not used by ProfileViewModelTest")

    override suspend fun getProfile(): User = error("not used by ProfileViewModelTest")

    override suspend fun updateProfile(fields: ProfileUpdateFields): User {
        lastUpdateFields = fields
        return updateResult.getOrThrow()
    }

    override suspend fun accessData(): DataRightResult = error("not used by ProfileViewModelTest")
    override suspend fun exportData(): DataRightResult = error("not used by ProfileViewModelTest")
    override suspend fun deleteAccount(): DataRightResult = error("not used by ProfileViewModelTest")
    override suspend fun revokeConsent(consentType: ConsentType): DataRightResult = error("not used by ProfileViewModelTest")
}

/** In-file [SecureTokenStorage] fake — `shared`'s own `FakeSecureTokenStorage` is `commonTest`-only. */
private class FakeSecureTokenStorage(initialToken: String? = null) : SecureTokenStorage {
    private var token: String? = initialToken
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

private fun sampleUser() = User(
    id = 1,
    name = "Ana",
    email = "ana@example.com",
    emailVerifiedAt = "2026-01-01T00:00:00Z",
    phone = "27999999999",
    profilePictureUrl = null,
    birthdate = "1990-01-01",
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun setUpViewModel(
        updateResult: Result<User> = Result.success(sampleUser()),
    ): Triple<ProfileViewModel, SessionStore, FakeProfileUserRepository> {
        val repository = FakeProfileUserRepository(updateResult)
        val sessionStore = SessionStore(repository, FakeSecureTokenStorage())
        sessionStore.set(sampleUser(), "tok-1")
        val vm = ProfileViewModel(sessionStore, UpdateProfile(repository))
        return Triple(vm, sessionStore, repository)
    }

    @Test
    fun `GIVEN a logged-in session WHEN constructed THEN the form fields are populated from the session user`() = runTest {
        val (vm, _, _) = setUpViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(sampleUser(), vm.uiState.value.user)
        assertEquals("Ana", vm.uiState.value.nameInput)
        assertEquals("27999999999", vm.uiState.value.phoneInput)
        assertEquals("ana@example.com", vm.uiState.value.emailInput)
    }

    @Test
    fun `GIVEN the name input changed WHEN onSaveName succeeds THEN the session user is updated with the new name`() = runTest {
        val (vm, sessionStore, repository) = setUpViewModel(Result.success(sampleUser().copy(name = "Ana Renomeada")))
        dispatcher.scheduler.advanceUntilIdle()

        vm.onNameChange("Ana Renomeada")
        vm.onSaveName()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProfileUpdateFields(name = "Ana Renomeada"), repository.lastUpdateFields)
        assertEquals("Ana Renomeada", sessionStore.currentUser.value?.name)
        assertEquals("Ana Renomeada", vm.uiState.value.user?.name)
        assertFalse(vm.uiState.value.isSavingName)
        assertFalse(vm.uiState.value.nameError)
    }

    @Test
    fun `GIVEN the phone input changed WHEN onSavePhone succeeds THEN the session user is updated with the new phone`() = runTest {
        val (vm, sessionStore, repository) = setUpViewModel(Result.success(sampleUser().copy(phone = "27988888888")))
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPhoneChange("27988888888")
        vm.onSavePhone()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ProfileUpdateFields(phone = "27988888888"), repository.lastUpdateFields)
        assertEquals("27988888888", sessionStore.currentUser.value?.phone)
        assertFalse(vm.uiState.value.isSavingPhone)
    }

    @Test
    fun `GIVEN onSaveName fails WHEN the repository throws THEN nameError is set and the session user is unchanged`() = runTest {
        val (vm, sessionStore, _) = setUpViewModel(Result.failure(RuntimeException("network error")))
        dispatcher.scheduler.advanceUntilIdle()

        vm.onNameChange("Ana Renomeada")
        vm.onSaveName()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.nameError)
        assertFalse(vm.uiState.value.isSavingName)
        assertEquals("Ana", sessionStore.currentUser.value?.name)
    }

    @Test
    fun `GIVEN the email input changed WHEN onSaveEmail succeeds THEN an EmailChangePending event fires and the displayed email is unchanged`() =
        runTest {
            val (vm, sessionStore, repository) = setUpViewModel(Result.success(sampleUser()))
            dispatcher.scheduler.advanceUntilIdle()

            vm.onEmailChange("nova@example.com")
            vm.onSaveEmail()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(ProfileUpdateFields(email = "nova@example.com"), repository.lastUpdateFields)
            assertEquals(ProfileEvent.EmailChangePending("nova@example.com"), vm.events.first())
            assertEquals("ana@example.com", vm.uiState.value.emailInput)
            assertEquals("ana@example.com", sessionStore.currentUser.value?.email)
            assertFalse(vm.uiState.value.isSavingEmail)
        }

    @Test
    fun `GIVEN onSaveEmail fails WHEN the repository throws THEN emailError is set and no event fires`() = runTest {
        val (vm, _, _) = setUpViewModel(Result.failure(RuntimeException("network error")))
        dispatcher.scheduler.advanceUntilIdle()

        vm.onEmailChange("nova@example.com")
        vm.onSaveEmail()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.emailError)
        assertFalse(vm.uiState.value.isSavingEmail)
    }

    @Test
    fun `GIVEN a field was in error WHEN it is edited again THEN its error is cleared`() = runTest {
        val (vm, _, _) = setUpViewModel(Result.failure(RuntimeException("network error")))
        dispatcher.scheduler.advanceUntilIdle()
        vm.onSaveName()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.nameError)

        vm.onNameChange("Outro nome")

        assertFalse(vm.uiState.value.nameError)
    }
}
