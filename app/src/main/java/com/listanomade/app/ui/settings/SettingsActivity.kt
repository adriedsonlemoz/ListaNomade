package com.listanomade.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.listanomade.app.BuildConfig
import com.listanomade.app.R
import com.listanomade.app.util.SettingsStore
import com.listanomade.app.util.ThemeManager

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.textVersion).text = getString(
            R.string.version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        val changes = resources.getStringArray(R.array.recent_changes)
            .mapIndexed { index, text -> "${index + 1}. $text" }
            .joinToString("\n")
        findViewById<TextView>(R.id.textChanges).text = changes

        val darkSwitch = findViewById<SwitchCompat>(R.id.switchDarkTheme)
        darkSwitch.isChecked = SettingsStore(this).darkTheme
        darkSwitch.setOnCheckedChangeListener { _, enabled -> ThemeManager.setDarkMode(this, enabled) }

        findViewById<Button>(R.id.buttonDonation).setOnClickListener { copyPixKey() }
    }

    private fun copyPixKey() {
        val key = getString(R.string.pix_key)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pix Lista Nômade", key))
        Toast.makeText(this, R.string.pix_copied, Toast.LENGTH_SHORT).show()
    }
}
