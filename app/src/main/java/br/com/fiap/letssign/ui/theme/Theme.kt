package br.com.fiap.letssign.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = VerdeValidado,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEEF2F7),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE8EDF3),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFE1E7EF),
    surfaceBright = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceDim = androidx.compose.ui.graphics.Color(0xFFD9E0E9),
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = VerdeValidadoClaro,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF08121F),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF0F1B2D),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF13233A),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF1A2B44),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF22344F),
    surfaceBright = androidx.compose.ui.graphics.Color(0xFF2A3B55),
    surfaceDim = androidx.compose.ui.graphics.Color(0xFF0B1726),
)

/**
 * Tema do Ratifica. Cores dinâmicas DESLIGADAS de propósito:
 * a identidade visual é a mesma em qualquer celular.
 */
@Composable
fun RatificaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}