package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Dark Command Terminal Palette
private val TacticalDarkColorScheme = darkColorScheme(
    primary = Color(0xFF86A594), // Olive light
    secondary = Color(0xFFC9BEAA), // Sand dry
    tertiary = Color(0xFFDCAE6C), // Brass accent
    background = Color(0xFF101412), // Deep carbon green
    surface = Color(0xFF191F1C), // Carbon card
    onPrimary = Color(0xFF1D2823),
    onSecondary = Color(0xFF2B2114),
    onTertiary = Color(0xFF33200D),
    onBackground = Color(0xFFE2E7E4),
    onSurface = Color(0xFFE2E7E4),
    primaryContainer = Color(0xFF2E3E37),
    onPrimaryContainer = Color(0xFF9FC1AF),
    secondaryContainer = Color(0xFF3B443F),
    onSecondaryContainer = Color(0xFFDCD2C1)
)

// Light Field Operations Palette
private val TacticalLightColorScheme = lightColorScheme(
    primary = Color(0xFF3F5449), // Deep Olive
    secondary = Color(0xFF6F5E4E), // Mud Sand
    tertiary = Color(0xFF8B6B38), // Brass
    background = Color(0xFFF4F7F5), // Chalk light sand background
    surface = Color(0xFFFFFFFF), // M3 Card surface
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1F1C),
    onSurface = Color(0xFF1A1F1C),
    primaryContainer = Color(0xFFD2E6DC),
    onPrimaryContainer = Color(0xFF102119),
    secondaryContainer = Color(0xFFF3EFE9),
    onSecondaryContainer = Color(0xFF2D251D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our high-quality custom military theme ALWAYS
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TacticalDarkColorScheme
        else -> TacticalLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
