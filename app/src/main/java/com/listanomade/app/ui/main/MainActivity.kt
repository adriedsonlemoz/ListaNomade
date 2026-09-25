package com.listanomade.app.ui.main

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
import com.listanomade.app.util.CurrencyInputFormatter
import com.listanomade.app.util.MoneyFormatter
import com.listanomade.app.util.SettingsStore
import com.listanomade.app.util.SystemBarInsets
import com.listanomade.app.util.ThemeManager
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var repository: ShoppingRepository
    private lateinit var settings: SettingsStore
    private lateinit var adapter: CategoryAdapter
    private lateinit var textGeneralTotal: TextView
    private lateinit var textPendingTotal: TextView
    private lateinit var textPurchasedTotal: TextView
    private lateinit var textGlobalBudget: TextView
    private lateinit var inputSearch: EditText
    private lateinit var buttonFilter: Button
    private lateinit var buttonSort: Button
    private lateinit var undoBar: View
    private lateinit var textUndo: TextView
    private var sourceData: List<CategoryList> = emptyList()
    private var deletedItem: ShoppingItem? = null
    private val hideUndo = Runnable { undoBar.visibility = View.GONE; deletedItem = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SystemBarInsets.apply(findViewById(R.id.rootMain))
        repository = ShoppingRepository(this)
        settings = SettingsStore(this)
        bindViews()
        configureList()
        configureActions()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(hideUndo)
        worker.shutdown()
        super.onDestroy()
    }

    private fun bindViews() {
        textGeneralTotal = findViewById(R.id.textGeneralTotal)
        textPendingTotal = findViewById(R.id.textPendingTotal)
        textPurchasedTotal = findViewById(R.id.textPurchasedTotal)
        textGlobalBudget = findViewById(R.id.textGlobalBudget)
        inputSearch = findViewById(R.id.inputSearch)
        buttonFilter = findViewById(R.id.buttonFilter)
        buttonSort = findViewById(R.id.buttonSort)
        undoBar = findViewById(R.id.undoBar)
        textUndo = findViewById(R.id.textUndo)
    }

    private fun configureList() {
        adapter = CategoryAdapter(
            onAddItem = ::openNewItem,
            onMore = ::showCategoryMenu,
            onToggleCollapsed = ::toggleCategory,
            onResolvedChanged = ::updateResolved,
            onItemMore = ::showItemMenu
        )
        findViewById<RecyclerView>(R.id.recyclerCategories).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun configureActions() {
        findViewById<ImageButton>(R.id.buttonSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<Button>(R.id.buttonAddCategory).setOnClickListener { showCategoryDialog(null) }
        findViewById<Button>(R.id.buttonAddItem).setOnClickListener { openNewItem(null) }
        findViewById<Button>(R.id.buttonGlobalBudget).setOnClickListener { showGlobalBudgetDialog() }
        findViewById<Button>(R.id.buttonUndo).setOnClickListener { undoDelete() }
        buttonFilter.setOnClickListener { showFilterMenu(it) }
        buttonSort.setOnClickListener { showSortMenu(it) }
        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderVisibleData()
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun reload() {
        worker.execute {
            val data = repository.loadAll()
            mainHandler.post {
                sourceData = data
                renderTotals()
                renderVisibleData()
            }
        }
    }

    private fun renderTotals() {
        val total = sourceData.sumOf { it.totalCents }
        textGeneralTotal.text = MoneyFormatter.format(total)
        textPendingTotal.text = MoneyFormatter.format(sourceData.sumOf { it.pendingCents })
        textPurchasedTotal.text = MoneyFormatter.format(sourceData.sumOf { it.purchasedCents })
        renderGlobalBudget(total)
    }

    private fun renderGlobalBudget(total: Long) {
        val budget = settings.globalBudgetCents
        if (budget <= 0L) {
            textGlobalBudget.setText(R.string.global_budget_not_set)
            textGlobalBudget.setTextColor(getColor(R.color.text_secondary))
            return
        }
        val delta = budget - total
        textGlobalBudget.text = if (delta >= 0L) {
            getString(R.string.budget_remaining, MoneyFormatter.format(budget), MoneyFormatter.format(delta))
        } else {
            getString(R.string.budget_over, MoneyFormatter.format(budget), MoneyFormatter.format(-delta))
        }
        textGlobalBudget.setTextColor(getColor(if (delta >= 0L) R.color.status_purchased else R.color.danger))
    }

    private fun renderVisibleData() {
        val query = inputSearch.text.toString().trim().lowercase(Locale.getDefault())
        val filter = settings.itemFilter
        val sort = settings.itemSort
        val activeFilter = query.isNotBlank() || filter != SettingsStore.FILTER_ALL
        val data = sourceData.mapNotNull { list ->
            val filtered = list.items.filter { item ->
                val textOk = query.isBlank() || listOf(item.name, item.store).any { it.lowercase(Locale.getDefault()).contains(query) }
                val stateOk = when (filter) {
                    SettingsStore.FILTER_PENDING -> item.pending
                    SettingsStore.FILTER_PURCHASED -> item.purchased
                    SettingsStore.FILTER_OWNED -> item.owned
                    else -> true
                }
                textOk && stateOk
            }
            if (activeFilter && filtered.isEmpty()) null else list.copy(visibleItems = sortItems(filtered, sort))
        }
        adapter.submitCategories(data)
        updateFilterButtons()
    }

    private fun sortItems(items: List<ShoppingItem>, mode: String): List<ShoppingItem> = when (mode) {
        SettingsStore.SORT_NAME -> items.sortedBy { it.name.lowercase(Locale.getDefault()) }
        SettingsStore.SORT_PRICE_DESC -> items.sortedByDescending { it.effectiveCostCents }
        SettingsStore.SORT_PRICE_ASC -> items.sortedBy { it.effectiveCostCents }
        SettingsStore.SORT_PENDING_FIRST -> items.sortedWith(compareBy<ShoppingItem> { !it.pending }.thenBy { it.sortOrder })
        SettingsStore.SORT_PRIORITY -> items.sortedWith(compareBy<ShoppingItem> { priorityRank(it.priority) }.thenBy { it.sortOrder })
        else -> items.sortedBy { it.sortOrder }
    }

    private fun priorityRank(priority: String): Int = when (priority) {
        ShoppingItem.PRIORITY_ESSENTIAL -> 0
        ShoppingItem.PRIORITY_OPTIONAL -> 2
        else -> 1
    }

    private fun updateFilterButtons() {
        buttonFilter.setText(when (settings.itemFilter) {
            SettingsStore.FILTER_PENDING -> R.string.filter_pending
            SettingsStore.FILTER_PURCHASED -> R.string.filter_purchased
            SettingsStore.FILTER_OWNED -> R.string.filter_owned
            else -> R.string.filter_all
        })
        buttonSort.setText(when (settings.itemSort) {
            SettingsStore.SORT_NAME -> R.string.sort_name
            SettingsStore.SORT_PRICE_DESC -> R.string.sort_price_desc
            SettingsStore.SORT_PRICE_ASC -> R.string.sort_price_asc
            SettingsStore.SORT_PENDING_FIRST -> R.string.sort_pending_first
            SettingsStore.SORT_PRIORITY -> R.string.sort_priority
            else -> R.string.sort_custom
        })
    }

    private fun showFilterMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            listOf(R.string.filter_all, R.string.filter_pending, R.string.filter_purchased, R.string.filter_owned)
                .forEach { menu.add(getString(it)) }
            setOnMenuItemClickListener {
                settings.itemFilter = when (it.title.toString()) {
                    getString(R.string.filter_pending) -> SettingsStore.FILTER_PENDING
                    getString(R.string.filter_purchased) -> SettingsStore.FILTER_PURCHASED
                    getString(R.string.filter_owned) -> SettingsStore.FILTER_OWNED
                    else -> SettingsStore.FILTER_ALL
                }
                renderVisibleData(); true
            }
            show()
        }
    }

    private fun showSortMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            listOf(R.string.sort_custom, R.string.sort_priority, R.string.sort_name, R.string.sort_price_desc, R.string.sort_price_asc, R.string.sort_pending_first)
                .forEach { menu.add(getString(it)) }
            setOnMenuItemClickListener {
                settings.itemSort = when (it.title.toString()) {
                    getString(R.string.sort_priority) -> SettingsStore.SORT_PRIORITY
                    getString(R.string.sort_name) -> SettingsStore.SORT_NAME
                    getString(R.string.sort_price_desc) -> SettingsStore.SORT_PRICE_DESC
                    getString(R.string.sort_price_asc) -> SettingsStore.SORT_PRICE_ASC
                    getString(R.string.sort_pending_first) -> SettingsStore.SORT_PENDING_FIRST
                    else -> SettingsStore.SORT_CUSTOM
                }
                renderVisibleData(); true
            }
            show()
        }
    }

    private fun openNewItem(categoryId: Long?) {
        val intent = Intent(this, AddItemActivity::class.java)
        categoryId?.let { intent.putExtra(AddItemActivity.EXTRA_CATEGORY_ID, it) }
        startActivity(intent)
    }

    private fun openEditItem(item: ShoppingItem) {
        startActivity(Intent(this, AddItemActivity::class.java).putExtra(AddItemActivity.EXTRA_ITEM_ID, item.id))
    }

    private fun updateResolved(item: ShoppingItem, resolved: Boolean) = runAndReload {
        if (resolved) repository.setPurchased(item.id, true) else repository.setPending(item.id)
    }

    private fun toggleCategory(id: Long, collapsed: Boolean) = runAndReload { repository.setCategoryCollapsed(id, collapsed) }

    private fun showItemMenu(anchor: View, item: ShoppingItem) {
        PopupMenu(this, anchor).apply {
            menu.add(getString(R.string.edit))
            menu.add(getString(if (item.owned) R.string.mark_pending else R.string.mark_owned))
            if (item.productUrl.isNotBlank()) menu.add(getString(R.string.open_product))
            listOf(R.string.duplicate_item, R.string.move_up, R.string.move_down, R.string.delete).forEach { menu.add(getString(it)) }
            setOnMenuItemClickListener {
                when (it.title.toString()) {
                    getString(R.string.edit) -> openEditItem(item)
                    getString(R.string.mark_owned) -> runAndReload { repository.setOwned(item.id, true) }
                    getString(R.string.mark_pending) -> runAndReload { repository.setPending(item.id) }
                    getString(R.string.open_product) -> openProduct(item.productUrl)
                    getString(R.string.duplicate_item) -> runAndReload { repository.duplicateItem(item.id) }
                    getString(R.string.move_up) -> runAndReload { repository.moveItem(item.id, -1) }
                    getString(R.string.move_down) -> runAndReload { repository.moveItem(item.id, 1) }
                    getString(R.string.delete) -> deleteItemWithUndo(item)
                }
                true
            }
            show()
        }
    }

    private fun openProduct(rawUrl: String) {
        val normalized = if (rawUrl.contains("://")) rawUrl else "https://$rawUrl"
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized))) }
            .onFailure { Toast.makeText(this, R.string.invalid_product_link, Toast.LENGTH_SHORT).show() }
    }

    private fun deleteItemWithUndo(item: ShoppingItem) {
        worker.execute {
            repository.deleteItem(item.id)
            val data = repository.loadAll()
            mainHandler.post {
                sourceData = data
                renderTotals(); renderVisibleData(); showUndo(item)
            }
        }
    }

    private fun showUndo(item: ShoppingItem) {
        deletedItem = item
        textUndo.text = getString(R.string.item_deleted, item.name)
        undoBar.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideUndo)
        mainHandler.postDelayed(hideUndo, 6000L)
    }

    private fun undoDelete() {
        val item = deletedItem ?: return
        mainHandler.removeCallbacks(hideUndo)
        undoBar.visibility = View.GONE
        deletedItem = null
        runAndReload { repository.restoreItem(item) }
    }

    private fun showCategoryMenu(anchor: View, list: CategoryList) {
        PopupMenu(this, anchor).apply {
            listOf(R.string.category_budget, R.string.rename_category, R.string.move_up, R.string.move_down, R.string.delete_category)
                .forEach { menu.add(getString(it)) }
            setOnMenuItemClickListener {
                when (it.title.toString()) {
                    getString(R.string.category_budget) -> showCategoryBudgetDialog(list)
                    getString(R.string.rename_category) -> showCategoryDialog(list)
                    getString(R.string.move_up) -> runAndReload { repository.moveCategory(list.category.id, -1) }
                    getString(R.string.move_down) -> runAndReload { repository.moveCategory(list.category.id, 1) }
                    getString(R.string.delete_category) -> confirmDeleteCategory(list)
                }
                true
            }
            show()
        }
    }

    private fun showCategoryBudgetDialog(list: CategoryList) = showBudgetDialog(
        R.string.category_budget, R.string.category_budget_hint, list.category.budgetCents,
        onRemove = { runAndReload { repository.setCategoryBudget(list.category.id, 0L) } },
        onSave = { cents -> runAndReload { repository.setCategoryBudget(list.category.id, cents) } }
    )

    private fun showGlobalBudgetDialog() = showBudgetDialog(
        R.string.global_budget, R.string.global_budget_hint, settings.globalBudgetCents,
        onRemove = { settings.globalBudgetCents = 0L; renderTotals() },
        onSave = { cents -> settings.globalBudgetCents = cents; renderTotals() }
    )

    private fun showBudgetDialog(titleRes: Int, hintRes: Int, current: Long, onRemove: () -> Unit, onSave: (Long) -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_budget)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<TextView>(R.id.textBudgetTitle).setText(titleRes)
        dialog.findViewById<TextView>(R.id.textBudgetHint).setText(hintRes)
        val input = dialog.findViewById<EditText>(R.id.inputBudget)
        var cents = current
        val formatter = CurrencyInputFormatter(input) { cents = it }
        input.addTextChangedListener(formatter)
        formatter.setCents(cents)
        dialog.findViewById<Button>(R.id.buttonBudgetCancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.buttonBudgetRemove).setOnClickListener { dialog.dismiss(); onRemove() }
        dialog.findViewById<Button>(R.id.buttonBudgetSave).setOnClickListener { dialog.dismiss(); onSave(cents) }
        dialog.setOnShowListener { resizeDialog(dialog); input.requestFocus() }
        dialog.show()
    }

    private fun showCategoryDialog(existing: CategoryList?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_category)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val title = dialog.findViewById<TextView>(R.id.textDialogTitle)
        val input = dialog.findViewById<EditText>(R.id.inputCategoryName)
        title.setText(if (existing == null) R.string.add_category else R.string.rename_category)
        input.setText(existing?.category?.name.orEmpty())
        input.setSelectAllOnFocus(true)
        dialog.findViewById<Button>(R.id.buttonDialogCancel).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<Button>(R.id.buttonDialogSave).setOnClickListener { saveCategory(dialog, input, existing) }
        input.setOnEditorActionListener { _, _, _ -> dialog.findViewById<Button>(R.id.buttonDialogSave).performClick(); true }
        dialog.setOnShowListener { resizeDialog(dialog); input.requestFocus() }
        dialog.show()
    }

    private fun resizeDialog(dialog: Dialog) {
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun saveCategory(dialog: Dialog, input: EditText, existing: CategoryList?) {
        val name = input.text.toString().trim()
        if (name.isBlank()) { input.error = getString(R.string.invalid_name); return }
        worker.execute {
            val ok = if (existing == null) repository.addCategory(name) else repository.renameCategory(existing.category.id, name)
            val data = repository.loadAll()
            mainHandler.post {
                if (!ok) input.error = getString(R.string.category_exists)
                else { dialog.dismiss(); sourceData = data; renderTotals(); renderVisibleData() }
            }
        }
    }

    private fun confirmDeleteCategory(list: CategoryList) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_category_question, list.category.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> runAndReload { repository.deleteCategory(list.category.id) } }
            .show()
    }

    private fun runAndReload(action: () -> Unit) {
        worker.execute {
            action()
            val data = repository.loadAll()
            mainHandler.post { sourceData = data; renderTotals(); renderVisibleData() }
        }
    }
}
