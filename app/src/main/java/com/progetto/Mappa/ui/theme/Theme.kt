package com.progetto.Mappa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Colore fisso per i bottoni
val ButtonColor = Color(0xFF6200EE)

// Colori per il testo
val TextWhite = Color.White
val TextBlack = Color.Black

private val DarkColorScheme = darkColorScheme(
    primary = ButtonColor,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = TextWhite,
    onSurface = TextWhite
)


private val LightColorScheme = lightColorScheme(
    primary = ButtonColor,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    onPrimary = TextWhite,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFAFAFA),
    onBackground = TextBlack,
    onSurface = TextBlack
)

@Composable
fun MappaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        content = content
    )
}
