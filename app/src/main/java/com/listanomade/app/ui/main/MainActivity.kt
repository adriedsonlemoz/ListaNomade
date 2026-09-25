package com.listanomade.app.ui.main

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.listanomade.app.R
import com.listanomade.app.data.ShoppingRepository
import com.listanomade.app.model.CategoryList
import com.listanomade.app.model.ShoppingItem
import com.listanomade.app.ui.item.AddItemActivity
import com.listanomade.app.ui.settings.SettingsActivity
import com.listanomade.app.util.MoneyFormatter
import com.listanomade.app.util.SystemBarInsets
import com.listanomade.app.util.ThemeManager
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var repository: ShoppingRepository
    private lateinit var adapter: CategoryAdapter
    private lateinit var textGeneralTotal: TextView
    private lateinit var textPendingTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SystemBarInsets.apply(findViewById(R.id.rootMain))
        repository = ShoppingRepository(this)
        bindViews()
        configureList()
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

    private fun bindViews() {
        textGeneralTotal = findViewById(R.id.textGeneralTotal)
        textPendingTotal = findViewById(R.id.textPendingTotal)
    }

    private fun configureList() {
        adapter = CategoryAdapter(
            onAddItem = ::openNewItem,
            onMore = ::showCategoryMenu,
            onPurchasedChanged = ::updatePurchased,
            onItemMore = ::showItemMenu
        )
        findViewById<RecyclerView>(R.id.recyclerCategories).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(false)
        }
    }

    private fun configureActions() {
        findViewById<ImageButton>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonAddCategory).setOnClickListener { showCategoryDialog(null) }
        findViewById<Button>(R.id.buttonAddItem).setOnClickListener { openNewItem(null) }
    }

    private fun reload() {
        worker.execute {
            val data = repository.loadAll()
            mainHandler.post { render(data) }
        }
    }

    private fun render(data: List<CategoryList>) {
        adapter.submitCategories(data)
        val total = data.sumOf { it.totalCents }
        val pending = data.sumOf { it.pendingCents }
        textGeneralTotal.text = MoneyFormatter.format(total)
        textPendingTotal.text = getString(R.string.pending_total, MoneyFormatter.format(pending))
    }

    private fun openNewItem(categoryId: Long?) {
        val intent = Intent(this, AddItemActivity::class.java)
        categoryId?.let { intent.putExtra(AddItemActivity.EXTRA_CATEGORY_ID, it) }
        startActivity(intent)
    }

    private fun openEditItem(item: ShoppingItem) {
        startActivity(Intent(this, AddItemActivity::class.java).putExtra(AddItemActivity.EXTRA_ITEM_ID, item.id))
    }

    private fun updatePurchased(item: ShoppingItem, purchased: Boolean) {
        worker.execute {
            repository.setPurchased(item.id, purchased)
            val data = repository.loadAll()
            mainHandler.post { render(data) }
        }
    }

    private fun showItemMenu(anchor: View, item: ShoppingItem) {
        PopupMenu(this, anchor).apply {
            menu.add(getString(R.string.edit))
            menu.add(getString(R.string.delete))
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.title.toString()) {
                    getString(R.string.edit) -> openEditItem(item)
                    getString(R.string.delete) -> confirmDeleteItem(item)
                }
                true
            }
            show()
        }
    }

    private fun confirmDeleteItem(item: ShoppingItem) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_item_question, item.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                worker.execute {
                    repository.deleteItem(item.id)
                    val data = repository.loadAll()
                    mainHandler.post { render(data) }
                }
            }
            .show()
    }

    private fun showCategoryMenu(anchor: View, categoryList: CategoryList) {
        PopupMenu(this, anchor).apply {
            menu.add(getString(R.string.rename_category))
            menu.add(getString(R.string.delete_category))
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    getString(R.string.rename_category) -> showCategoryDialog(categoryList)
                    getString(R.string.delete_category) -> confirmDeleteCategory(categoryList)
                }
                true
            }
            show()
        }
    }

    private fun showCategoryDialog(existing: CategoryList?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_category)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)

        val title = dialog.findViewById<TextView>(R.id.textDialogTitle)
        val input = dialog.findViewById<EditText>(R.id.inputCategoryName)
        val cancel = dialog.findViewById<Button>(R.id.buttonDialogCancel)
        val save = dialog.findViewById<Button>(R.id.buttonDialogSave)
        title.setText(if (existing == null) R.string.add_category else R.string.rename_category)
        input.setText(existing?.category?.name.orEmpty())
        input.setSelectAllOnFocus(true)

        cancel.setOnClickListener { dialog.dismiss() }
        save.setOnClickListener { saveCategory(dialog, input, existing) }
        input.setOnEditorActionListener { _, _, _ ->
            save.performClick()
            true
        }

        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.90f).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            input.requestFocus()
        }
        dialog.show()
    }

    private fun saveCategory(dialog: Dialog, input: EditText, existing: CategoryList?) {
        val name = input.text.toString().trim()
        if (name.isBlank()) {
            input.error = getString(R.string.invalid_name)
            return
        }
        worker.execute {
            val ok = if (existing == null) repository.addCategory(name)
            else repository.renameCategory(existing.category.id, name)
            val data = repository.loadAll()
            mainHandler.post {
                if (!ok) {
                    input.error = getString(R.string.category_exists)
                } else {
                    dialog.dismiss()
                    render(data)
                    Toast.makeText(
                        this,
                        if (existing == null) R.string.category_added else R.string.category_renamed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun confirmDeleteCategory(categoryList: CategoryList) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_category_question, categoryList.category.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                worker.execute {
                    repository.deleteCategory(categoryList.category.id)
                    val data = repository.loadAll()
                    mainHandler.post { render(data) }
                }
            }
            .show()
    }
}
