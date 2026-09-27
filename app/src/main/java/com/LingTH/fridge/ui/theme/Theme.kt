package com.LingTH.fridge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Mint80,
    onPrimary = Color(0xFF00391F),
    primaryContainer = MintContainer30,
    onPrimaryContainer = OnMintContainer90,
    secondary = Teal80,
    secondaryContainer = TealContainer30,
    onSecondaryContainer = TealContainer90,
    tertiary = Lime80,
    tertiaryContainer = LimeContainer30,
    background = MintBackgroundDark,
    onBackground = OnMintSurfaceDark,
    surface = MintBackgroundDark,
    onSurface = OnMintSurfaceDark,
    surfaceVariant = MintSurfaceVariantDark,
    onSurfaceVariant = OnMintSurfaceVariantDark,
    outline = MintOutlineDark,
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainerHigh = Color(0xFF252B28),
)

private val LightColorScheme = lightColorScheme(
    primary = Mint40,
    onPrimary = Color.White,
    primaryContainer = MintContainer90,
    onPrimaryContainer = OnMintContainer10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = TealContainer90,
    onSecondaryContainer = OnTealContainer10,
    tertiary = Lime40,
    tertiaryContainer = LimeContainer90,
    background = MintBackground,
    onBackground = OnMintSurface,
    surface = MintBackground,
    onSurface = OnMintSurface,
    surfaceVariant = MintSurfaceVariant,
    onSurfaceVariant = OnMintSurfaceVariant,
    outline = MintOutline,
    surfaceContainer = Color(0xFFEAF1EB),
    surfaceContainerLow = Color(0xFFF0F6F1),
    surfaceContainerHigh = Color(0xFFE4EBE5),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app keeps its own mint identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
