package com.qualorock.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepTeal = Color(0xFF006D77)
private val SunsetOrange = Color(0xFFEE964B)
private val PaleAqua = Color(0xFFEDF6F9)

private val LightColors =
    lightColorScheme(
        primary = DeepTeal,
        secondary = PaleAqua,
        tertiary = SunsetOrange,
    )

private val DarkColors =
    darkColorScheme(
        primary = DeepTeal,
        secondary = SunsetOrange,
        tertiary = PaleAqua,
    )

@Composable
fun QualORockTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content,
    )
}
