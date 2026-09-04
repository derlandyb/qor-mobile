package br.com.qualorock.androidApp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import br.com.qualorock.androidApp.ui.components.OtpCodeField
import br.com.qualorock.androidApp.ui.components.PrimaryButton
import br.com.qualorock.androidApp.ui.viewmodel.CodeFieldError
import br.com.qualorock.androidApp.ui.viewmodel.EmailVerificationEvent
import br.com.qualorock.androidApp.ui.viewmodel.EmailVerificationViewModel
import design.QualORockThemeTokens
import org.koin.androidx.compose.koinViewModel

/**
 * A9 — email-verification OTP entry (auth-fan-profile AUTH-01/AUTH-10; matches `qor-website`'s
 * W20 `/verificar-email`). Owns only form + submit + resend UI: on success it calls [onVerified].
 *
 * **[onVerified] does not mean the fan is logged in.** Verifying an OTP code only marks the
 * account verified server-side — no session/token comes back (see [EmailVerificationViewModel]'s
 * KDoc) — so wiring [onVerified] to navigate to Login rather than Home is A14's job, not this
 * screen's.
 *
 * [email] is nav-graph state (passed in by A14's destination args), not owned by this
 * composable's `ViewModel` — see [EmailVerificationViewModel]'s KDoc for why.
 */
@Composable
fun EmailVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailVerificationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EmailVerificationEvent.Verified -> onVerified()
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
            text = stringResource(R.string.email_verification_title),
            color = Color(QualORockThemeTokens.ColorTextPrimary),
            fontWeight = FontWeight.Bold,
            fontSize = QualORockThemeTokens.TextEventTitleLg.SizeSp.sp,
        )

        Text(
            text = stringResource(R.string.email_verification_instructions, email),
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
            onClick = { viewModel.onSubmit(email) },
            isLoading = uiState.isLoading,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(QualORockThemeTokens.Space1Dp.dp)) {
            Text(
                text = stringResource(R.string.email_verification_resend_prompt),
                color = Color(QualORockThemeTokens.ColorTextSecondary),
                fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            )

            if (uiState.cooldown.canResend) {
                Text(
                    text = stringResource(R.string.cta_reenviar),
                    color = Color(QualORockThemeTokens.AccentBlue),
                    fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                    modifier = Modifier.clickable { viewModel.onResend(email) },
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.email_verification_resend_countdown,
                        uiState.cooldown.remainingSeconds,
                    ),
                    color = Color(QualORockThemeTokens.ColorTextTertiary),
                    fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                )
            }
        }

        if (uiState.resendConfirmation) {
            Text(
                text = stringResource(R.string.email_verification_resend_confirmation),
                color = Color(QualORockThemeTokens.ColorTextSecondary),
                fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            )
        }
    }
}

private fun CodeFieldError.messageRes(): Int = when (this) {
    CodeFieldError.Required -> R.string.email_verification_error_code_required
    CodeFieldError.InvalidLength -> R.string.email_verification_error_code_invalid_length
}
