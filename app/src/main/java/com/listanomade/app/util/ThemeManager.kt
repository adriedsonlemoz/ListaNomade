package com.listanomade.app.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    fun applySavedTheme(context: Context) {
        val mode = if (SettingsStore(context).darkTheme) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        SettingsStore(context).darkTheme = enabled
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
