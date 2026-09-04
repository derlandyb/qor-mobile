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
import br.com.qualorock.androidApp.ui.components.OtpCodeField
import br.com.qualorock.androidApp.ui.components.PasswordField
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.viewmodel.CodeFieldError
import br.com.qualorock.androidApp.ui.viewmodel.EmailFieldError
import br.com.qualorock.androidApp.ui.viewmodel.NewPasswordFieldError
import br.com.qualorock.androidApp.ui.viewmodel.PasswordRecoveryEvent
import br.com.qualorock.androidApp.ui.viewmodel.PasswordRecoveryStep
import br.com.qualorock.androidApp.ui.viewmodel.PasswordRecoveryViewModel
import design.QualORockThemeTokens
import org.koin.androidx.compose.koinViewModel

/**
 * A10/A21 — password recovery, a 3-step wizard (email -> OTP code -> new password;
 * auth-fan-profile AUTH-13–AUTH-16). Mirrors `qor-website`'s `app/recuperar-senha/page.tsx` UX
 * exactly — see [PasswordRecoveryViewModel]'s KDoc for the per-step orchestration. Owns only
 * form + submit UI: on success it calls [onResetSuccess] — actually navigating to Login is A14's
 * job, not this screen's.
 */
@Composable
fun PasswordRecoveryScreen(
    onResetSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordRecoveryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PasswordRecoveryEvent.ResetSuccess -> onResetSuccess()
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
            text = stringResource(R.string.password_recovery_title),
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.Bold,
            fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
        )

        when (uiState.step) {
            is PasswordRecoveryStep.RequestEmail -> {
                Text(
                    text = stringResource(R.string.password_recovery_request_instructions),
                    color = Color(QualORockThemeTokens.ColorTextSecondary),
                    fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                )

                EmailField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    errorMessage = uiState.emailError?.let { stringResource(it.messageRes()) },
                )

                PrimaryButton(
                    text = stringResource(R.string.cta_enviar_link_recuperacao),
                    onClick = viewModel::onSubmitEmail,
                    isLoading = uiState.isLoading,
                )
            }

            is PasswordRecoveryStep.VerifyCode -> {
                Text(
                    text = stringResource(R.string.password_recovery_generic_confirmation),
                    color = Color(QualORockThemeTokens.ColorTextSecondary),
                    fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                )

                OtpCodeField(
                    value = uiState.code,
                    onValueChange = viewModel::onCodeChange,
                    errorMessage = uiState.codeError?.let { stringResource(it.messageRes()) },
                )

                uiState.submitError?.let { message ->
                    Text(
                        text = message,
                        color = Color(QualORockThemeTokens.ColorDanger),
                        fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                    )
                }

                PrimaryButton(
                    text = stringResource(R.string.cta_verificar_codigo),
                    onClick = viewModel::onSubmitCode,
                    isLoading = uiState.isLoading,
                )
            }

            is PasswordRecoveryStep.NewPassword -> {
                Text(
                    text = stringResource(R.string.password_recovery_new_password_instructions),
                    color = Color(QualORockThemeTokens.ColorTextSecondary),
                    fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                )

                PasswordField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    errorMessage = uiState.newPasswordError?.let { stringResource(it.messageRes()) },
                )

                uiState.submitError?.let { message ->
                    Text(
                        text = message,
                        color = Color(QualORockThemeTokens.ColorDanger),
                        fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                    )
                }

                PrimaryButton(
                    text = stringResource(R.string.cta_redefinir_senha),
                    onClick = viewModel::onSubmitNewPassword,
                    isLoading = uiState.isLoading,
                )
            }
        }

        Text(
            text = stringResource(R.string.password_recovery_link_login),
            color = Color(QualORockThemeTokens.AccentBlue),
            fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            modifier = Modifier
                .padding(top = QualORockThemeTokens.Space1Dp.dp)
                .clickable(onClick = onNavigateToLogin),
        )
    }
}

private fun EmailFieldError.messageRes(): Int = when (this) {
    EmailFieldError.Required -> R.string.password_recovery_error_email_required
    EmailFieldError.InvalidFormat -> R.string.password_recovery_error_email_invalid
}

private fun CodeFieldError.messageRes(): Int = when (this) {
    CodeFieldError.Required -> R.string.password_recovery_error_code_required
    CodeFieldError.InvalidLength -> R.string.password_recovery_error_code_invalid_length
}

private fun NewPasswordFieldError.messageRes(): Int = when (this) {
    NewPasswordFieldError.Required -> R.string.password_recovery_error_password_required
    NewPasswordFieldError.TooShort -> R.string.password_recovery_error_password_too_short
}
