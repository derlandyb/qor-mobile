package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import br.com.qualorock.androidApp.ui.components.EmailField
import br.com.qualorock.androidApp.ui.components.PasswordField
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.components.SecondaryButton
import br.com.qualorock.androidApp.ui.viewmodel.EmailFieldError
import br.com.qualorock.androidApp.ui.viewmodel.LoginEvent
import br.com.qualorock.androidApp.ui.viewmodel.LoginSubmitError
import br.com.qualorock.androidApp.ui.viewmodel.LoginViewModel
import br.com.qualorock.androidApp.ui.viewmodel.PasswordFieldError
import design.QualORockThemeTokens
import org.koin.androidx.compose.koinViewModel

/**
 * A7 — returning-fan login (auth-fan-profile AUTH-09–AUTH-12; Stitch screen
 * `cfa5690fed3d487897d65de249ad7f1d`). Owns only form + submit UI: on success it calls
 * [onLoginSuccess], on an unverified account it calls [onNavigateToVerifyEmail] with the
 * submitted email — actually pushing a destination is A14's nav-graph job, not this screen's.
 *
 * **"Entrar com Google" is a disabled stub.** No Google Sign-In SDK (Credential Manager /
 * `com.google.android.gms.auth`) is wired into `androidApp` yet — this button renders per the
 * Stitch design but is inert (`enabled = false`, no-op `onClick`) rather than fabricating an
 * OAuth flow. Wiring it up is a separate, not-yet-scheduled task.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToVerifyEmail: (email: String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToPasswordRecovery: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.LoginSuccess -> onLoginSuccess()
                is LoginEvent.NavigateToVerifyEmail -> onNavigateToVerifyEmail(event.email)
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space3Dp.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(QualORockThemeTokens.Space4Dp.dp),
    ) {
        Text(
            text = stringResource(R.string.login_title),
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.Bold,
            fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
        )

        EmailField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            errorMessage = uiState.emailError?.let { stringResource(it.messageRes()) },
        )

        PasswordField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            errorMessage = uiState.passwordError?.let { stringResource(it.messageRes()) },
        )

        uiState.submitError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                color = Color(QualORockThemeTokens.ColorDanger),
                fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            )
        }

        PrimaryButton(
            text = stringResource(R.string.cta_entrar),
            onClick = viewModel::onSubmit,
            isLoading = uiState.isLoading,
        )

        SecondaryButton(
            text = stringResource(R.string.cta_entrar_com_google),
            onClick = {},
            enabled = false,
        )

        Text(
            text = stringResource(R.string.login_link_password_recovery),
            color = Color(QualORockThemeTokens.AccentBlue),
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            modifier = Modifier
                .padding(top = QualORockThemeTokens.Space1Dp.dp)
                .clickable(onClick = onNavigateToPasswordRecovery),
        )

        Text(
            text = stringResource(R.string.login_link_signup),
            color = Color(QualORockThemeTokens.AccentBlue),
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            modifier = Modifier.clickable(onClick = onNavigateToSignup),
        )
    }
}

private fun EmailFieldError.messageRes(): Int = when (this) {
    EmailFieldError.Required -> R.string.login_error_email_required
    EmailFieldError.InvalidFormat -> R.string.login_error_email_invalid
}

private fun PasswordFieldError.messageRes(): Int = when (this) {
    PasswordFieldError.Required -> R.string.login_error_password_required
}

private fun LoginSubmitError.messageRes(): Int = when (this) {
    LoginSubmitError.InvalidCredentials -> R.string.login_error_invalid_credentials
    is LoginSubmitError.UnverifiedAccount -> R.string.login_error_unverified_account
}
