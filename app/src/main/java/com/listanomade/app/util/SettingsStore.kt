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

    var storeFilter: String
        get() = prefs.getString(KEY_STORE_FILTER, STORE_ALL) ?: STORE_ALL
        set(value) { prefs.edit().putString(KEY_STORE_FILTER, value).apply() }

    var travelMode: Boolean
        get() = prefs.getBoolean(KEY_TRAVEL_MODE, false)
        set(value) { prefs.edit().putBoolean(KEY_TRAVEL_MODE, value).apply() }

    var globalBudgetCents: Long
        get() = prefs.getLong(KEY_GLOBAL_BUDGET, 0L)
        set(value) { prefs.edit().putLong(KEY_GLOBAL_BUDGET, value.coerceAtLeast(0L)).apply() }

    companion object {
        const val FILTER_ALL = "all"
        const val FILTER_PENDING = "pending"
        const val FILTER_PURCHASED = "purchased"
        const val FILTER_OWNED = "owned"
        const val FILTER_PARTIAL = "partial"
        const val STORE_ALL = "__all__"
        const val SORT_CUSTOM = "custom"
        const val SORT_NAME = "name"
        const val SORT_PRICE_DESC = "price_desc"
        const val SORT_PRICE_ASC = "price_asc"
        const val SORT_PENDING_FIRST = "pending_first"
        const val SORT_PRIORITY = "priority"
        const val SORT_TARGET_DATE = "target_date"

        private const val PREFS_NAME = "lista_nomade_settings"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_ITEM_FILTER = "item_filter"
        private const val KEY_ITEM_SORT = "item_sort"
        private const val KEY_STORE_FILTER = "store_filter"
        private const val KEY_TRAVEL_MODE = "travel_mode"
        private const val KEY_GLOBAL_BUDGET = "global_budget_cents"
    }
}
