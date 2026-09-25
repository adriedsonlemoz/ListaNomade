package com.listanomade.app.data

import android.content.Context
import com.listanomade.app.BuildConfig
import com.listanomade.app.util.SettingsStore
import org.json.JSONObject

class BackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val repository = ShoppingRepository(appContext)
    private val templates = TemplateRepository(appContext)
    private val settings = SettingsStore(appContext)

    fun createBackup(): String {
        val root = repository.exportDataJson()
        root.put("app", "Lista Nômade")
        root.put("version", BuildConfig.VERSION_NAME)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("templates", templates.exportJson())
        root.put(
            "settings",
            JSONObject()
                .put("darkTheme", settings.darkTheme)
                .put("itemFilter", settings.itemFilter)
                .put("itemSort", settings.itemSort)
                .put("storeFilter", settings.storeFilter)
                .put("travelMode", settings.travelMode)
                .put("globalBudgetCents", settings.globalBudgetCents)
        )
        return root.toString(2)
    }

    fun restoreBackup(json: String) {
        val root = JSONObject(json)
        require(root.optString("app", "Lista Nômade") == "Lista Nômade") { "Arquivo de backup inválido" }
        repository.importDataJson(root)
        templates.replaceFromJson(root.optJSONArray("templates"))
        root.optJSONObject("settings")?.let { saved ->
            settings.darkTheme = saved.optBoolean("darkTheme", settings.darkTheme)
            settings.itemFilter = saved.optString("itemFilter", SettingsStore.FILTER_ALL)
            settings.itemSort = saved.optString("itemSort", SettingsStore.SORT_CUSTOM)
            settings.storeFilter = saved.optString("storeFilter", SettingsStore.STORE_ALL)
            settings.travelMode = saved.optBoolean("travelMode", false)
            settings.globalBudgetCents = saved.optLong("globalBudgetCents", settings.globalBudgetCents)
        }
    }
}
