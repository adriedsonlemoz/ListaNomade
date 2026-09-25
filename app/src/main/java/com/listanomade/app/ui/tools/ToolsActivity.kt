package com.listanomade.app.ui.tools

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.listanomade.app.R
import com.listanomade.app.data.BulkImportParser
import com.listanomade.app.data.ShoppingRepository
import com.listanomade.app.data.TemplateRepository
import com.listanomade.app.model.CategoryList
import com.listanomade.app.model.ShoppingTemplate
import com.listanomade.app.util.MoneyFormatter
import com.listanomade.app.util.SettingsStore
import com.listanomade.app.util.ShareTextBuilder
import com.listanomade.app.util.SystemBarInsets
import com.listanomade.app.util.ThemeManager
import java.util.concurrent.Executors

class ToolsActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var repository: ShoppingRepository
    private lateinit var templates: TemplateRepository
    private lateinit var settings: SettingsStore
    private var sourceData: List<CategoryList> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tools)
        SystemBarInsets.apply(findViewById(R.id.rootTools))
        repository = ShoppingRepository(this)
        templates = TemplateRepository(this)
        settings = SettingsStore(this)
        configureActions()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun configureActions() {
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonBulkImport).setOnClickListener { showImportDialog() }
        findViewById<Button>(R.id.buttonTemplates).setOnClickListener { showTemplates() }
        findViewById<Button>(R.id.buttonArchived).setOnClickListener { showArchived() }
        findViewById<Button>(R.id.buttonShareAll).setOnClickListener { shareAll() }
        findViewById<SwitchCompat>(R.id.switchTravelMode).apply {
            isChecked = settings.travelMode
            setOnCheckedChangeListener { _, enabled -> settings.travelMode = enabled }
        }
    }

    private fun reload() {
        worker.execute {
            val data = repository.loadAll()
            mainHandler.post { sourceData = data; renderSummary() }
        }
    }

    private fun renderSummary() {
        val items = sourceData.flatMap { it.items }
        val totalUnits = items.sumOf { it.quantity }
        val resolvedUnits = items.sumOf { if (it.owned) it.quantity else it.boughtQuantity }
        val percent = if (totalUnits == 0) 0 else (resolvedUnits * 100 / totalUnits).coerceIn(0, 100)
        val purchased = items.count { it.fullyPurchased }
        val owned = items.count { it.owned }
        val partial = items.count { it.partial }
        val pending = items.count { it.pending && !it.partial }
        val spent = sourceData.sumOf { it.purchasedCents }
        val remaining = sourceData.sumOf { it.pendingCents }
        findViewById<TextView>(R.id.textProgressPercent).text = getString(R.string.progress_percent, percent)
        findViewById<ProgressBar>(R.id.progressOverall).progress = percent
        findViewById<TextView>(R.id.textProgressCounts).text = getString(R.string.progress_counts, items.size, purchased, owned, partial, pending)
        findViewById<TextView>(R.id.textProgressMoney).text = getString(R.string.progress_money, MoneyFormatter.format(spent), MoneyFormatter.format(remaining))
    }

    private fun showImportDialog() {
        if (sourceData.isEmpty()) return
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_import_text)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val spinner = dialog.findViewById<Spinner>(R.id.spinnerImportCategory)
        val input = dialog.findViewById<EditText>(R.id.inputImportText)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sourceData.map { it.category.name }).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        dialog.findViewById<Button>(R.id.buttonImportCancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.buttonImportSave).setOnClickListener {
            val parsed = BulkImportParser.parse(input.text.toString())
            if (parsed.isEmpty()) {
                Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val categoryId = sourceData[spinner.selectedItemPosition].category.id
            dialog.dismiss()
            worker.execute {
                repository.addImportedItems(categoryId, parsed)
                mainHandler.post { Toast.makeText(this, getString(R.string.import_success, parsed.size), Toast.LENGTH_SHORT).show(); reload() }
            }
        }
        dialog.setOnShowListener { resizeDialog(dialog); input.requestFocus() }
        dialog.show()
    }

    private fun showTemplates() {
        worker.execute {
            val list = templates.list()
            mainHandler.post {
                if (list.isEmpty()) AlertDialog.Builder(this).setMessage(R.string.template_empty).setPositiveButton(android.R.string.ok, null).show()
                else AlertDialog.Builder(this)
                    .setTitle(R.string.templates)
                    .setItems(list.map { "${it.name} (${it.itemCount})" }.toTypedArray()) { _, which -> showTemplateActions(list[which]) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun showTemplateActions(template: ShoppingTemplate) {
        AlertDialog.Builder(this)
            .setTitle(template.name)
            .setItems(arrayOf(getString(R.string.create_from_template), getString(R.string.delete_template))) { _, which ->
                if (which == 0) promptName(R.string.template_category_name, template.name) { name -> createFromTemplate(template, name) }
                else worker.execute { templates.delete(template.id); mainHandler.post { showTemplates() } }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createFromTemplate(template: ShoppingTemplate, name: String) {
        worker.execute {
            val ok = templates.instantiate(template.id, name)
            mainHandler.post {
                Toast.makeText(this, if (ok) R.string.template_created else R.string.category_exists, Toast.LENGTH_SHORT).show()
                if (ok) reload()
            }
        }
    }

    private fun showArchived() {
        worker.execute {
            val archived = repository.loadArchived()
            mainHandler.post {
                if (archived.isEmpty()) AlertDialog.Builder(this).setMessage(R.string.archived_empty).setPositiveButton(android.R.string.ok, null).show()
                else AlertDialog.Builder(this)
                    .setTitle(R.string.archived_categories)
                    .setItems(archived.map { it.category.name }.toTypedArray()) { _, which ->
                        worker.execute {
                            repository.setCategoryArchived(archived[which].category.id, false)
                            mainHandler.post { Toast.makeText(this, R.string.category_unarchived, Toast.LENGTH_SHORT).show(); reload() }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun shareAll() {
        if (sourceData.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, ShareTextBuilder.all(sourceData))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_with)))
    }

    private fun promptName(titleRes: Int, initial: String, onSave: (String) -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_category)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.textDialogTitle).setText(titleRes)
        val input = dialog.findViewById<EditText>(R.id.inputCategoryName)
        input.setText(initial)
        input.setSelectAllOnFocus(true)
        dialog.findViewById<Button>(R.id.buttonDialogCancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.buttonDialogSave).setOnClickListener {
            val name = input.text.toString().trim()
            if (name.isBlank()) input.error = getString(R.string.invalid_name) else { dialog.dismiss(); onSave(name) }
        }
        dialog.setOnShowListener { resizeDialog(dialog); input.requestFocus() }
        dialog.show()
    }

    private fun resizeDialog(dialog: Dialog) {
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
}
