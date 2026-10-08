package com.partner.studyreminder.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.partner.studyreminder.data.Prefs

private val LightScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    background = Color(0xFFD9E8F8),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0x993C3C43),
    error = Color(0xFFFF3B30),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    background = Color(0xFF0B1020),
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.72f),
    error = Color(0xFFFF453A),
)

@Composable
fun StudyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    ThemeMode.changes()
    val dark = ThemeMode.isDark(context, isSystemInDarkTheme())
    val activity = context as? ComponentActivity
    SideEffect { activity?.edgeToEdge() }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
            bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        ),
        content = content,
    )
}

fun ComponentActivity.edgeToEdge() {
    val transparent = android.graphics.Color.TRANSPARENT
    val theme = Prefs.theme(this)
    val detectDark: (android.content.res.Resources) -> Boolean = { resources ->
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
    val dark = ThemeMode.isDark(this, ThemeMode.systemIsDark(this))
    if (theme == Prefs.THEME_SYSTEM) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(transparent, transparent, detectDark),
            navigationBarStyle = SystemBarStyle.auto(transparent, transparent, detectDark),
        )
    } else if (dark) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent),
        )
    } else {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
    }
}

fun Activity.toast(message: String, long: Boolean = true) {
    val length = if (long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
    android.widget.Toast.makeText(this, message, length).show()
}
