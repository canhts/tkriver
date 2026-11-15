package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppColorPalette(
    val displayName: String,
    val seedColor: Color
) {
    OCEAN_BLUE("Xanh biển", Color(0xFF0D47A1)),
    EMERALD_GREEN("Xanh lá", Color(0xFF1B5E20)),
    ROYAL_PURPLE("Tím", Color(0xFF5E35B1)),
    TERRACOTTA("Cam đất", Color(0xFFBF360C)),
    TEAL("Xanh cổ vịt", Color(0xFF00695C))
}

private fun lightSchemeForPalette(palette: AppColorPalette): ColorScheme = when (palette) {
    AppColorPalette.OCEAN_BLUE -> lightColorScheme(
        primary = Color(0xFF0D47A1),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E4FF),
        onPrimaryContainer = Color(0xFF001B3D),
        secondary = Color(0xFF37474F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCFD8DC),
        onSecondaryContainer = Color(0xFF102027),
        tertiary = Color(0xFF00838F),
        background = Color(0xFFF4F8FB),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE7EFF6),
        surfaceContainer = Color(0xFFEAF1F8),
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF475569)
    )

    AppColorPalette.EMERALD_GREEN -> lightColorScheme(
        primary = Color(0xFF1B5E20),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E6C9),
        onPrimaryContainer = Color(0xFF052109),
        secondary = Color(0xFF33691E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCEDC8),
        onSecondaryContainer = Color(0xFF112708),
        tertiary = Color(0xFF00695C),
        background = Color(0xFFF4F9F5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE4EFE6),
        surfaceContainer = Color(0xFFE8F3EA),
        onBackground = Color(0xFF102014),
        onSurface = Color(0xFF102014),
        onSurfaceVariant = Color(0xFF3F5644)
    )

    AppColorPalette.ROYAL_PURPLE -> lightColorScheme(
        primary = Color(0xFF5E35B1),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE7F6),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF512DA8),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD1C4E9),
        onSecondaryContainer = Color(0xFF1B093B),
        tertiary = Color(0xFF8E24AA),
        background = Color(0xFFF8F5FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFECE6F5),
        surfaceContainer = Color(0xFFF1ECF9),
        onBackground = Color(0xFF1C1629),
        onSurface = Color(0xFF1C1629),
        onSurfaceVariant = Color(0xFF4F4563)
    )

    AppColorPalette.TERRACOTTA -> lightColorScheme(
        primary = Color(0xFFBF360C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFCCBC),
        onPrimaryContainer = Color(0xFF3B0900),
        secondary = Color(0xFF8D4E3B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDBCF),
        onSecondaryContainer = Color(0xFF341105),
        tertiary = Color(0xFFD84315),
        background = Color(0xFFFCF6F4),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF5E7E2),
        surfaceContainer = Color(0xFFF8ECE8),
        onBackground = Color(0xFF26140F),
        onSurface = Color(0xFF26140F),
        onSurfaceVariant = Color(0xFF614942)
    )

    AppColorPalette.TEAL -> lightColorScheme(
        primary = Color(0xFF00695C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB2DFDB),
        onPrimaryContainer = Color(0xFF00201B),
        secondary = Color(0xFF00796B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFC8E8E5),
        onSecondaryContainer = Color(0xFF002520),
        tertiary = Color(0xFF0277BD),
        background = Color(0xFFF2F9F8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE0F0EE),
        surfaceContainer = Color(0xFFE5F4F2),
        onBackground = Color(0xFF0B211E),
        onSurface = Color(0xFF0B211E),
        onSurfaceVariant = Color(0xFF3D5754)
    )
}

private fun darkSchemeForPalette(palette: AppColorPalette): ColorScheme = when (palette) {
    AppColorPalette.OCEAN_BLUE -> darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF002171),
        primaryContainer = Color(0xFF0D47A1),
        onPrimaryContainer = Color(0xFFE3F2FD),
        secondary = Color(0xFFB0BEC5),
        onSecondary = Color(0xFF1C313A),
        secondaryContainer = Color(0xFF263238),
        onSecondaryContainer = Color(0xFFECEFF1),
        tertiary = Color(0xFF80DEEA),
        background = Color(0xFF0B131B),
        surface = Color(0xFF111C26),
        surfaceVariant = Color(0xFF1B2A38),
        surfaceContainer = Color(0xFF162432),
        onBackground = Color(0xFFE3F2FD),
        onSurface = Color(0xFFE3F2FD),
        onSurfaceVariant = Color(0xFFB0BEC5)
    )

    AppColorPalette.EMERALD_GREEN -> darkColorScheme(
        primary = Color(0xFFA5D6A7),
        onPrimary = Color(0xFF073510),
        primaryContainer = Color(0xFF1B5E20),
        onPrimaryContainer = Color(0xFFE8F5E9),
        secondary = Color(0xFFC5E1A5),
        onSecondary = Color(0xFF1B3309),
        secondaryContainer = Color(0xFF263B1D),
        onSecondaryContainer = Color(0xFFDCEDC8),
        tertiary = Color(0xFF80CBC4),
        background = Color(0xFF0B160E),
        surface = Color(0xFF111F15),
        surfaceVariant = Color(0xFF1B2E20),
        surfaceContainer = Color(0xFF16271A),
        onBackground = Color(0xFFE8F5E9),
        onSurface = Color(0xFFE8F5E9),
        onSurfaceVariant = Color(0xFFB4C8B8)
    )

    AppColorPalette.ROYAL_PURPLE -> darkColorScheme(
        primary = Color(0xFFD1C4E9),
        onPrimary = Color(0xFF2E1263),
        primaryContainer = Color(0xFF4527A0),
        onPrimaryContainer = Color(0xFFEDE7F6),
        secondary = Color(0xFFB39DDB),
        onSecondary = Color(0xFF231342),
        secondaryContainer = Color(0xFF30234A),
        onSecondaryContainer = Color(0xFFEDE7F6),
        tertiary = Color(0xFFCE93D8),
        background = Color(0xFF110D1A),
        surface = Color(0xFF181326),
        surfaceVariant = Color(0xFF251E38),
        surfaceContainer = Color(0xFF1E182E),
        onBackground = Color(0xFFEDE7F6),
        onSurface = Color(0xFFEDE7F6),
        onSurfaceVariant = Color(0xFFC1B6D6)
    )

    AppColorPalette.TERRACOTTA -> darkColorScheme(
        primary = Color(0xFFFFAB91),
        onPrimary = Color(0xFF561400),
        primaryContainer = Color(0xFF8F2807),
        onPrimaryContainer = Color(0xFFFFE0D6),
        secondary = Color(0xFFFFCCBC),
        onSecondary = Color(0xFF421C10),
        secondaryContainer = Color(0xFF3E231B),
        onSecondaryContainer = Color(0xFFFFDBCF),
        tertiary = Color(0xFFFF8A65),
        background = Color(0xFF180E0B),
        surface = Color(0xFF221511),
        surfaceVariant = Color(0xFF34211B),
        surfaceContainer = Color(0xFF2A1B16),
        onBackground = Color(0xFFFFEDE8),
        onSurface = Color(0xFFFFEDE8),
        onSurfaceVariant = Color(0xFFD7B8B0)
    )

    AppColorPalette.TEAL -> darkColorScheme(
        primary = Color(0xFF80CBC4),
        onPrimary = Color(0xFF003730),
        primaryContainer = Color(0xFF004D40),
        onPrimaryContainer = Color(0xFFE0F2F1),
        secondary = Color(0xFFA7FFEB),
        onSecondary = Color(0xFF00382E),
        secondaryContainer = Color(0xFF183633),
        onSecondaryContainer = Color(0xFFE0F2F1),
        tertiary = Color(0xFF81D4FA),
        background = Color(0xFF091615),
        surface = Color(0xFF10211F),
        surfaceVariant = Color(0xFF19302D),
        surfaceContainer = Color(0xFF142826),
        onBackground = Color(0xFFE0F2F1),
        onSurface = Color(0xFFE0F2F1),
        onSurfaceVariant = Color(0xFFACC7C4)
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPalette: AppColorPalette = AppColorPalette.OCEAN_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkSchemeForPalette(colorPalette)
    } else {
        lightSchemeForPalette(colorPalette)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
