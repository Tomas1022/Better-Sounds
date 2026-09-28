package com.example.bettersounds.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaOscuro = darkColorScheme(
    primary = OscuroVerde,
    onPrimary = Color(0xFF00391B),
    background = OscuroFondo,
    onBackground = OscuroTexto,
    surface = OscuroSuperficie,
    onSurface = OscuroTexto
)

private val EsquemaClaro = lightColorScheme(
    primary = ClaroVerde,
    onPrimary = Color.White,
    background = ClaroFondo,
    onBackground = ClaroTexto,
    surface = ClaroSuperficie,
    onSurface = ClaroTexto
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}