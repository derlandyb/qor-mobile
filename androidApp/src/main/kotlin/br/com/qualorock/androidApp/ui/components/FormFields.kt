package br.com.qualorock.androidApp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import br.com.qualorock.androidApp.R
import design.QualORockThemeTokens

/**
 * A6 — the design system's text field variant with pt-BR inline validation error display,
 * consumed by every auth screen (login/signup/reset). `errorMessage == null` means valid/untouched.
 */
@Composable
fun QorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = errorMessage != null,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(QualORockThemeTokens.AccentBlue),
                errorBorderColor = Color(QualORockThemeTokens.ColorDanger),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(QualORockThemeTokens.ColorDanger),
                fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
            )
        }
    }
}

/** A6 — email field variant, keyboard type + label fixed. */
@Composable
fun EmailField(value: String, onValueChange: (String) -> Unit, errorMessage: String?, modifier: Modifier = Modifier) {
    QorTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.field_label_email),
        errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = modifier,
    )
}

/** A6 — password field variant, with a "Mostrar senha"/"Ocultar senha" visibility toggle. */
@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, errorMessage: String?, modifier: Modifier = Modifier) {
    var revealed by remember { mutableStateOf(false) }

    QorTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.field_label_password),
        errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            Text(
                text = stringResource(if (revealed) R.string.password_toggle_hide else R.string.password_toggle_show),
                color = Color(QualORockThemeTokens.AccentBlue),
                fontSize = QualORockThemeTokens.TextMetadata.SizeSp.sp,
                modifier = Modifier.clickable { revealed = !revealed },
            )
        },
        modifier = modifier,
    )
}
