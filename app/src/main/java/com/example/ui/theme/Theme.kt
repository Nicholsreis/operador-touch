package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = TealAccentLight,
    onPrimary = SlateDarkBackground,
    secondary = PurpleGrey80,
    tertiary = AmberOrange,
    background = SlateDarkBackground,
    surface = SlateSurface,
    onPrimaryContainer = Color.White,
    onSecondaryContainer = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TealAccent,
    onPrimary = Color.White,
    secondary = PurpleGrey40,
    tertiary = AmberOrange,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = SlateDarkBackground,
    onSurface = SlateDarkBackground,
    onPrimaryContainer = Color.White,
    onSecondaryContainer = Color.White
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
