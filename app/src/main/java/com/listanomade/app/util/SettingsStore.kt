package com.listanomade.app.util

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) { prefs.edit().putBoolean(KEY_DARK_THEME, value).apply() }

    companion object {
        private const val PREFS_NAME = "lista_nomade_settings"
        private const val KEY_DARK_THEME = "dark_theme"
    }
}
