package com.partner.studyreminder.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import com.partner.studyreminder.data.Prefs

/** Process-wide tick so every open screen recomposes after a theme change. */
object ThemeMode {
    private val revision = mutableIntStateOf(0)

    @Composable
    fun changes(): Int = revision.intValue

    fun notifyChanged() {
        val apply = Runnable { revision.intValue++ }
        if (Looper.myLooper() == Looper.getMainLooper()) apply.run()
        else Handler(Looper.getMainLooper()).post(apply)
    }

    fun isDark(context: Context, systemDark: Boolean): Boolean = when (Prefs.theme(context)) {
        Prefs.THEME_DARK -> true
        Prefs.THEME_LIGHT -> false
        else -> systemDark
    }

    fun systemIsDark(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }
}
