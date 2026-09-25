package com.listanomade.app.util

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) { prefs.edit().putBoolean(KEY_DARK_THEME, value).apply() }

    var itemFilter: String
        get() = prefs.getString(KEY_ITEM_FILTER, FILTER_ALL) ?: FILTER_ALL
        set(value) { prefs.edit().putString(KEY_ITEM_FILTER, value).apply() }

    var itemSort: String
        get() = prefs.getString(KEY_ITEM_SORT, SORT_CUSTOM) ?: SORT_CUSTOM
        set(value) { prefs.edit().putString(KEY_ITEM_SORT, value).apply() }

    companion object {
        const val FILTER_ALL = "all"
        const val FILTER_PENDING = "pending"
        const val FILTER_PURCHASED = "purchased"
        const val SORT_CUSTOM = "custom"
        const val SORT_NAME = "name"
        const val SORT_PRICE_DESC = "price_desc"
        const val SORT_PRICE_ASC = "price_asc"
        const val SORT_PENDING_FIRST = "pending_first"

        private const val PREFS_NAME = "lista_nomade_settings"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_ITEM_FILTER = "item_filter"
        private const val KEY_ITEM_SORT = "item_sort"
    }
}
