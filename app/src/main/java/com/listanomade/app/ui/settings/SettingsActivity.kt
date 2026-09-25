package com.listanomade.app.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.listanomade.app.BuildConfig
import com.listanomade.app.R
import com.listanomade.app.data.BackupManager
import com.listanomade.app.util.SettingsStore
import com.listanomade.app.util.SystemBarInsets
import com.listanomade.app.util.ThemeManager
import java.util.concurrent.Executors

class SettingsActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        SystemBarInsets.apply(findViewById(R.id.rootSettings))
        backupManager = BackupManager(this)

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.textVersion).text = getString(R.string.version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        findViewById<TextView>(R.id.textChanges).text = resources.getStringArray(R.array.recent_changes)
            .mapIndexed { index, text -> "${index + 1}. $text" }.joinToString("\n")

        val darkSwitch = findViewById<SwitchCompat>(R.id.switchDarkTheme)
        darkSwitch.isChecked = SettingsStore(this).darkTheme
        darkSwitch.setOnCheckedChangeListener { _, enabled -> ThemeManager.setDarkMode(this, enabled) }

        findViewById<Button>(R.id.buttonExportBackup).setOnClickListener { chooseBackupDestination() }
        findViewById<Button>(R.id.buttonRestoreBackup).setOnClickListener { chooseBackupFile() }
        findViewById<Button>(R.id.buttonDonation).setOnClickListener { copyPixKey() }
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    @Deprecated("Compatibilidade com Android 8+")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_EXPORT -> exportBackup(uri)
            REQUEST_RESTORE -> confirmRestore(uri)
        }
    }

    private fun chooseBackupDestination() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "Lista-Nomade-backup-v${BuildConfig.VERSION_NAME}.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    private fun chooseBackupFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_RESTORE)
    }

    private fun exportBackup(uri: Uri) {
        worker.execute {
            val result = runCatching {
                val json = backupManager.createBackup()
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                    ?: error("Não foi possível abrir o arquivo")
            }
            runOnUiThread {
                Toast.makeText(this, if (result.isSuccess) R.string.backup_exported else R.string.backup_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmRestore(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(R.string.restore_backup)
            .setMessage(R.string.restore_backup_warning)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.restore) { _, _ -> restoreBackup(uri) }
            .show()
    }

    private fun restoreBackup(uri: Uri) {
        worker.execute {
            val result = runCatching {
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Não foi possível ler o arquivo")
                backupManager.restoreBackup(json)
            }
            runOnUiThread {
                Toast.makeText(this, if (result.isSuccess) R.string.backup_restored else R.string.backup_invalid, Toast.LENGTH_LONG).show()
                if (result.isSuccess) recreate()
            }
        }
    }

    private fun copyPixKey() {
        val key = getString(R.string.pix_key)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pix Lista Nômade", key))
        Toast.makeText(this, R.string.pix_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_EXPORT = 4101
        private const val REQUEST_RESTORE = 4102
    }
}
