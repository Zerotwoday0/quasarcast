package xyz.lukioff.webstreamvr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OnAccent = Color(0xFF05070D)

private val QuasarColorScheme = darkColorScheme(
    primary = QuasarOrange,
    onPrimary = OnAccent,
    primaryContainer = QuasarOrangeDim,
    onPrimaryContainer = TextPrimary,
    secondary = NebulaCyan,
    onSecondary = OnAccent,
    secondaryContainer = NebulaCyanDim,
    onSecondaryContainer = TextPrimary,
    tertiary = NebulaCyan,
    background = SpaceBackground,
    onBackground = TextPrimary,
    surface = SpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = StatusRed,
    onError = OnAccent,
)

@Composable
fun QuasarCastTheme(content: @Composable () -> Unit) {
    // Horizon OS panels are dark; a light theme is glaring in the headset.
    MaterialTheme(
        colorScheme = QuasarColorScheme,
        typography = Typography,
        content = content,
    )
}
