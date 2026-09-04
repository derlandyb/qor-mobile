package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.EmailField
import br.com.qualorock.androidApp.ui.components.PlaceholderImage
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.components.QorTextField
import br.com.qualorock.androidApp.ui.components.SecondaryButton
import br.com.qualorock.androidApp.ui.viewmodel.ProfileEvent
import br.com.qualorock.androidApp.ui.viewmodel.ProfileViewModel
import design.QualORockThemeTokens
import org.koin.androidx.compose.koinViewModel

/**
 * A13 — basic profile display + inline edit (AUTH-17–AUTH-19). Shows username/email/phone/
 * birthdate/profile-picture (AUTH-17); name and phone save inline and reflect immediately
 * (AUTH-18, [ProfileViewModel.onSaveName]/[ProfileViewModel.onSavePhone] write straight back
 * into the shared session). Birthdate has no edit UI — neither this task's brief (AUTH-17–AUTH-19)
 * nor `UpdateProfile`'s [domain.user.ProfileUpdateFields] contract exposes a birthdate field to
 * change, so it renders read-only.
 *
 * **Profile-picture editing is stubbed, not wired.** No image-picker/upload flow exists anywhere
 * in this app yet — same gap A7/A8 already left for Google sign-in (a visible, inert button
 * rather than an invented photo-upload flow). "Alterar foto" renders disabled; picking this back
 * up is a future task once a picker/upload path exists client-side.
 *
 * **Email edits never apply immediately (AUTH-19).** [onEmailChangePending] fires instead of any
 * local/session update on a successful save — see [ProfileViewModel]'s KDoc for the mechanism.
 * A14 wires this callback to navigate to `EmailVerificationScreen` with the new email, reusing
 * its existing OTP UX rather than duplicating it on this screen.
 *
 * P2 concerns (address/location, favorite genres/radius, notification prefs, LGPD data-rights UI)
 * are explicitly out of scope — see mobile.md A13.
 */
@Composable
fun ProfileScreen(
    onEmailChangePending: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveErrorMessage = stringResource(R.string.profile_save_error)
    val photoUnavailableMessage = stringResource(R.string.profile_picture_change_unavailable)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.EmailChangePending -> onEmailChangePending(event.newEmail)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space4Dp.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(QualORockThemeTokens.Space4Dp.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.Bold,
            fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
        )

        PlaceholderImage(modifier = Modifier.fillMaxWidth())
        SecondaryButton(
            text = stringResource(R.string.cta_alterar_foto),
            onClick = {},
            enabled = false,
        )
        Text(
            text = photoUnavailableMessage,
            color = Color(QualORockThemeTokens.ColorTextTertiary),
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space1Dp.dp)) {
            Text(
                text = stringResource(R.string.field_label_birthdate) + ":",
                color = Color(QualORockThemeTokens.ColorTextTertiary),
                fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            )
            Text(
                text = uiState.user?.birthdate.orEmpty(),
                color = Color(QualORockThemeTokens.ColorTextSecondary),
                fontWeight = FontWeight.SemiBold,
                fontSize = QualORockThemeTokens.TextBody.SizeSp.sp,
            )
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp)) {
            QorTextField(
                value = uiState.nameInput,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.field_label_name),
                errorMessage = if (uiState.nameError) saveErrorMessage else null,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.cta_salvar),
                onClick = viewModel::onSaveName,
                isLoading = uiState.isSavingName,
            )
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp)) {
            QorTextField(
                value = uiState.phoneInput,
                onValueChange = viewModel::onPhoneChange,
                label = stringResource(R.string.field_label_phone),
                errorMessage = if (uiState.phoneError) saveErrorMessage else null,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.cta_salvar),
                onClick = viewModel::onSavePhone,
                isLoading = uiState.isSavingPhone,
            )
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space2Dp.dp)) {
            EmailField(
                value = uiState.emailInput,
                onValueChange = viewModel::onEmailChange,
                errorMessage = if (uiState.emailError) saveErrorMessage else null,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.cta_salvar),
                onClick = viewModel::onSaveEmail,
                isLoading = uiState.isSavingEmail,
            )
        }
    }
}
