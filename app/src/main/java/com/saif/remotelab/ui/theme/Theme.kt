package com.saif.remotelab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

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

/** Always dark and always RTL: the app is Arabic-first regardless of the device language. */
@Composable
fun RemoteLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ThabatColorScheme, typography = Typography) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}
