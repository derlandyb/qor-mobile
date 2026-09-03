package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import br.com.qualorock.androidApp.ui.components.ConsentCapture
import br.com.qualorock.androidApp.ui.components.EmailField
import br.com.qualorock.androidApp.ui.components.PasswordField
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.components.QorTextField
import br.com.qualorock.androidApp.ui.components.SecondaryButton
import br.com.qualorock.androidApp.ui.viewmodel.BirthdateFieldError
import br.com.qualorock.androidApp.ui.viewmodel.EmailFieldError
import br.com.qualorock.androidApp.ui.viewmodel.NameFieldError
import br.com.qualorock.androidApp.ui.viewmodel.PasswordFieldError
import br.com.qualorock.androidApp.ui.viewmodel.SignupEvent
import br.com.qualorock.androidApp.ui.viewmodel.SignupViewModel
import design.QualORockThemeTokens
import org.koin.androidx.compose.koinViewModel

/**
 * A8 — new-fan signup (auth-fan-profile AUTH-01–AUTH-03). Owns only form + submit UI: on success
 * it calls [onSignupSuccess] with the submitted email — A14 wires this to `EmailVerificationScreen`
 * with the email pre-filled (mirrors `qor-website`'s `router.push('/verificar-email?email=...')`).
 * [onNavigateToLogin] is the "já tem conta?" link; this screen does not own navigation.
 *
 * **"Cadastrar com Google" is a disabled stub.** No Google Sign-In SDK is wired into `androidApp`
 * yet (confirmed during A7) — this button renders per the design system but is inert
 * (`enabled = false`, no-op `onClick`) rather than fabricating an OAuth flow.
 */
@Composable
fun SignupScreen(
    onSignupSuccess: (email: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignupViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SignupEvent.SignupSuccess -> onSignupSuccess(event.email)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space3Dp.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(QualORockThemeTokens.Space4Dp.dp),
    ) {
        Text(
            text = stringResource(R.string.signup_title),
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.Bold,
            fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
        )

        QorTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.field_label_name),
            errorMessage = uiState.nameError?.let { stringResource(it.messageRes()) } ?: uiState.nameServerError,
        )

        EmailField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            errorMessage = uiState.emailError?.let { stringResource(it.messageRes()) } ?: uiState.emailServerError,
        )

        PasswordField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            errorMessage = uiState.passwordError?.let { stringResource(it.messageRes()) } ?: uiState.passwordServerError,
        )

        QorTextField(
            value = uiState.birthdate,
            onValueChange = viewModel::onBirthdateChange,
            label = stringResource(R.string.field_label_birthdate),
            errorMessage = uiState.birthdateError?.let { stringResource(it.messageRes()) } ?: uiState.birthdateServerError,
        )

        ConsentCapture(
            accepted = uiState.consentAccepted,
            onAcceptedChange = viewModel::onConsentChange,
        )

        uiState.consentError?.let {
            Text(
                text = stringResource(R.string.signup_error_consent_required),
                color = Color(QualORockThemeTokens.ColorDanger),
                fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            )
        }

        uiState.submitError?.let { message ->
            Text(
                text = message,
                color = Color(QualORockThemeTokens.ColorDanger),
                fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            )
        }

        PrimaryButton(
            text = stringResource(R.string.cta_cadastrar),
            onClick = viewModel::onSubmit,
            isLoading = uiState.isLoading,
        )

        SecondaryButton(
            text = stringResource(R.string.cta_cadastrar_com_google),
            onClick = {},
            enabled = false,
        )

        Text(
            text = stringResource(R.string.signup_link_login),
            color = Color(QualORockThemeTokens.AccentBlue),
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            modifier = Modifier
                .padding(top = QualORockThemeTokens.Space1Dp.dp)
                .clickable(onClick = onNavigateToLogin),
        )
    }
}

private fun NameFieldError.messageRes(): Int = when (this) {
    NameFieldError.Required -> R.string.signup_error_name_required
}

private fun EmailFieldError.messageRes(): Int = when (this) {
    EmailFieldError.Required -> R.string.signup_error_email_required
    EmailFieldError.InvalidFormat -> R.string.signup_error_email_invalid
}

private fun PasswordFieldError.messageRes(): Int = when (this) {
    PasswordFieldError.Required -> R.string.signup_error_password_required
}

private fun BirthdateFieldError.messageRes(): Int = when (this) {
    BirthdateFieldError.Required -> R.string.signup_error_birthdate_required
}
