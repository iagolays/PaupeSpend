package com.tuusuario.gastos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Morado = Color(0xFF6C5CE7)
private val MoradoClaro = Color(0xFFB8AFFF)
private val Verde = Color(0xFF00B894)
private val Azul = Color(0xFF0984E3)
private val Noche = Color(0xFF2D2D44)

private val EsquemaClaro = lightColorScheme(
    primary = Morado,
    onPrimary = Color.White,
    secondary = Verde,
    onSecondary = Color.White,
    tertiary = Azul,
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1B1C2A),
    surface = Color.White,
    onSurface = Color(0xFF1B1C2A),
    surfaceVariant = Color(0xFFEDEFF7),
    onSurfaceVariant = Color(0xFF4A4C5E),
    outline = Color(0xFFB9BCC9),
    error = Color(0xFFE17055),
)

private val EsquemaOscuro = darkColorScheme(
    primary = MoradoClaro,
    onPrimary = Noche,
    secondary = Verde,
    onSecondary = Color.White,
    tertiary = Azul,
    background = Color(0xFF15151F),
    onBackground = Color(0xFFE9EAF2),
    surface = Color(0xFF1E1E2C),
    onSurface = Color(0xFFE9EAF2),
    surfaceVariant = Color(0xFF2D2D44),
    onSurfaceVariant = Color(0xFFB9BCC9),
    outline = Color(0xFF6A6C80),
    error = Color(0xFFFFB4A5),
)

@Composable
fun TemaGastos(content: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        content = content
    )
}