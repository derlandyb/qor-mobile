package br.com.qualorock.androidApp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.SessionStore
import domain.user.ProfileUpdateFields
import domain.user.User
import domain.user.usecase.UpdateProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-shot outcome [ProfileScreen][br.com.qualorock.androidApp.ui.screen.ProfileScreen] consumes; A14 wires navigation. */
sealed class ProfileEvent {
    /** AUTH-19 — [newEmail] still awaits re-verification; A14 routes this to `EmailVerificationScreen`. */
    data class EmailChangePending(val newEmail: String) : ProfileEvent()
}

/**
 * Form + save state for [ProfileScreen][br.com.qualorock.androidApp.ui.screen.ProfileScreen].
 * `*Error` fields are booleans, not messages — [UpdateProfile]/[domain.user.UserRepository] carry
 * no server-message contract for this call (unlike `LoginResult`/`VerifyEmailResult`), the same
 * gap [HomeFeedUiState.Error] already documents; the screen resolves generic pt-BR copy from a
 * string resource, keeping this class Android-resource-agnostic.
 */
data class ProfileUiState(
    val user: User? = null,
    val nameInput: String = "",
    val phoneInput: String = "",
    val emailInput: String = "",
    val isSavingName: Boolean = false,
    val isSavingPhone: Boolean = false,
    val isSavingEmail: Boolean = false,
    val nameError: Boolean = false,
    val phoneError: Boolean = false,
    val emailError: Boolean = false,
)

/**
 * A13 — basic profile display + inline edit (AUTH-17–AUTH-19). Reads the currently-loaded fan
 * from [SessionStore.currentUser] (already populated at login/restore, S9/S10) rather than
 * issuing a fresh `getProfile()` fetch — this screen only ever needs to show the session's own
 * user, and [SessionStore] is the Koin singleton every screen would observe if it needed to.
 *
 * [UpdateProfile] backs both the inline name/phone saves (AUTH-18, applied immediately — success
 * writes the returned [User] straight back into [SessionStore] via [SessionStore.updateCurrentUser]
 * so [uiState] and every other screen observing the session see it at once) and the email-change
 * submit (AUTH-19). **A successful email change never touches [uiState]'s displayed email or the
 * shared session** — the server holds the new address pending re-verification, so this class
 * discards [UpdateProfile]'s return value for that call and instead fires
 * [ProfileEvent.EmailChangePending] for A14 to route to `EmailVerificationScreen`'s existing OTP
 * UX, rather than duplicating that UI here.
 *
 * Profile-picture editing has no client-side upload path yet ([ProfileUpdateFields.profilePictureUrl]
 * exists on the contract, but no image-picker/upload wiring exists anywhere in this app) — like
 * A7/A8's Google sign-in stub, [ProfileScreen][br.com.qualorock.androidApp.ui.screen.ProfileScreen]
 * renders a visible, inert "Alterar foto" action with no backing ViewModel method, documented in
 * its own KDoc rather than invented here.
 *
 * P2 concerns (address/location, favorite genres/radius, notification prefs, LGPD data-rights UI)
 * are explicitly out of scope for this task — see mobile.md A13.
 */
class ProfileViewModel(
    private val sessionStore: SessionStore,
    private val updateProfile: UpdateProfile,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionStore.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            user = user,
                            nameInput = user.name,
                            phoneInput = user.phone.orEmpty(),
                            emailInput = user.email,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(nameInput = value, nameError = false) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phoneInput = value, phoneError = false) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(emailInput = value, emailError = false) }
    }

    /** AUTH-18 — inline-save, reflects immediately (via [SessionStore.updateCurrentUser]) on success. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun onSaveName() {
        val name = _uiState.value.nameInput
        _uiState.update { it.copy(isSavingName = true, nameError = false) }
        viewModelScope.launch {
            try {
                val updated = updateProfile.execute(ProfileUpdateFields(name = name))
                sessionStore.updateCurrentUser(updated)
                _uiState.update { it.copy(isSavingName = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingName = false, nameError = true) }
            }
        }
    }

    /** AUTH-18 — inline-save, reflects immediately (via [SessionStore.updateCurrentUser]) on success. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun onSavePhone() {
        val phone = _uiState.value.phoneInput
        _uiState.update { it.copy(isSavingPhone = true, phoneError = false) }
        viewModelScope.launch {
            try {
                val updated = updateProfile.execute(ProfileUpdateFields(phone = phone))
                sessionStore.updateCurrentUser(updated)
                _uiState.update { it.copy(isSavingPhone = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingPhone = false, phoneError = true) }
            }
        }
    }

    /**
     * AUTH-19 — unlike [onSaveName]/[onSavePhone], a successful call here never applies the
     * change locally or to [SessionStore]; it only fires [ProfileEvent.EmailChangePending]. See
     * this class's own KDoc for why.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun onSaveEmail() {
        val newEmail = _uiState.value.emailInput
        _uiState.update { it.copy(isSavingEmail = true, emailError = false) }
        viewModelScope.launch {
            try {
                updateProfile.execute(ProfileUpdateFields(email = newEmail))
                _uiState.update { it.copy(isSavingEmail = false, emailInput = it.user?.email.orEmpty()) }
                _events.send(ProfileEvent.EmailChangePending(newEmail))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingEmail = false, emailError = true) }
            }
        }
    }
}
