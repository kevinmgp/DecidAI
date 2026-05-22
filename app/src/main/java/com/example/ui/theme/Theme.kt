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

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD3E4FF),
    secondary = CleanSecondary,
    tertiary = CleanPrimary,
    background = Color(0xFF111318),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF30343C),
    onPrimary = Color(0xFF001C38),
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E2E9),
    onSurface = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E919F),
    outlineVariant = Color(0xFF44474E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CleanPrimary,
    secondary = CleanSecondary,
    tertiary = CleanTertiary,
    background = CleanBackground,
    surface = Color.White,
    surfaceVariant = CleanSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = CleanTextPrimary,
    onSurface = CleanTextPrimary,
    onSurfaceVariant = CleanSupport,
    outline = CleanOutline,
    outlineVariant = CleanDivider
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
