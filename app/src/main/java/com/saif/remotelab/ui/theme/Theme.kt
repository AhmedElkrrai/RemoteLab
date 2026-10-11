package com.saif.remotelab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ThabatColorScheme = darkColorScheme(
    primary = Palette.Brand,
    onPrimary = Palette.Deep,
    primaryContainer = Palette.BrandContainer,
    onPrimaryContainer = Palette.Text,
    secondaryContainer = Palette.BrandDark,
    onSecondaryContainer = Palette.Text,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Bg,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Card,
    onSurfaceVariant = Palette.TextSoft,
    surfaceContainerLowest = Palette.Deep,
    surfaceContainerLow = Palette.Card,
    surfaceContainer = Palette.Card,
    surfaceContainerHigh = Palette.Card,
    surfaceContainerHighest = Palette.Line,
    outline = Palette.Line,
    outlineVariant = Palette.Line,
    error = Tone.Rose.fg
)

/** Always dark: the palette is designed as a single dark theme. */
@Composable
fun RemoteLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ThabatColorScheme, typography = Typography, content = content)
}
