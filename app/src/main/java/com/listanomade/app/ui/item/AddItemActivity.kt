package com.listanomade.app.ui.item

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.listanomade.app.R
import com.listanomade.app.data.ShoppingRepository
import com.listanomade.app.model.Category
import com.listanomade.app.model.ShoppingItem
import com.listanomade.app.util.CurrencyInputFormatter
import com.listanomade.app.util.MoneyFormatter
import com.listanomade.app.util.SystemBarInsets
import com.listanomade.app.util.ThemeManager
import java.util.concurrent.Executors

class AddItemActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var repository: ShoppingRepository
    private lateinit var inputName: EditText
    private lateinit var inputPrice: EditText
    private lateinit var inputQuantity: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var textTotal: TextView
    private lateinit var buttonSave: Button
    private lateinit var buttonSaveAnother: Button
    private lateinit var priceFormatter: CurrencyInputFormatter
    private var categories: List<Category> = emptyList()
    private var editingItem: ShoppingItem? = null
    private var currentPriceCents = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)
        SystemBarInsets.apply(findViewById(R.id.rootAddItem))
        repository = ShoppingRepository(this)
        bindViews()
        configureActions()
        loadData()
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun bindViews() {
        inputName = findViewById(R.id.inputName)
        inputPrice = findViewById(R.id.inputPrice)
        inputQuantity = findViewById(R.id.inputQuantity)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        textTotal = findViewById(R.id.textCalculatedTotal)
        buttonSave = findViewById(R.id.buttonSave)
        buttonSaveAnother = findViewById(R.id.buttonSaveAnother)
        buttonSave.isEnabled = false
        buttonSaveAnother.isEnabled = false
        findViewById<TextView>(R.id.textTitle).setText(
            if (intent.hasExtra(EXTRA_ITEM_ID)) R.string.edit_item_title else R.string.add_item_title
        )
    }

    private fun configureActions() {
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        priceFormatter = CurrencyInputFormatter(inputPrice) { cents ->
            currentPriceCents = cents
            updateCalculatedTotal()
        }
        inputPrice.addTextChangedListener(priceFormatter)
        inputQuantity.addTextChangedListener(simpleWatcher { updateCalculatedTotal() })
        buttonSave.setOnClickListener { validateAndSave(false) }
        buttonSaveAnother.setOnClickListener { validateAndSave(true) }
    }

    private fun loadData() {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L).takeIf { it > 0 }
        worker.execute {
            val loadedCategories = repository.getCategories()
            val item = itemId?.let(repository::getItem)
            mainHandler.post { populate(loadedCategories, item) }
        }
    }

    private fun populate(loadedCategories: List<Category>, item: ShoppingItem?) {
        categories = loadedCategories
        editingItem = item
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.map { it.name }).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val preferredCategoryId = item?.categoryId ?: intent.getLongExtra(EXTRA_CATEGORY_ID, -1L).takeIf { it > 0 }
        val index = categories.indexOfFirst { it.id == preferredCategoryId }.takeIf { it >= 0 } ?: 0
        if (categories.isNotEmpty()) spinnerCategory.setSelection(index)

        inputQuantity.setText((item?.quantity ?: 1).toString())
        priceFormatter.setCents(item?.unitPriceCents ?: 0L)
        item?.let { inputName.setText(it.name) }
        val enabled = categories.isNotEmpty()
        buttonSave.isEnabled = enabled
        buttonSaveAnother.isEnabled = enabled
        buttonSaveAnother.visibility = if (item == null) View.VISIBLE else View.GONE
        inputName.requestFocus()
    }

    private fun updateCalculatedTotal() {
        val quantity = inputQuantity.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        textTotal.text = MoneyFormatter.format(currentPriceCents * quantity)
    }

    private fun validateAndSave(addAnother: Boolean) {
        val name = inputName.text.toString().trim()
        val quantity = inputQuantity.text.toString().toIntOrNull()
        when {
            name.isBlank() -> inputName.error = getString(R.string.invalid_name)
            currentPriceCents < 0L -> inputPrice.error = getString(R.string.invalid_price)
            quantity == null || quantity <= 0 -> inputQuantity.error = getString(R.string.invalid_quantity)
            categories.isEmpty() -> return
            else -> save(name, currentPriceCents, quantity, addAnother)
        }
    }

    private fun save(name: String, price: Long, quantity: Int, addAnother: Boolean) {
        buttonSave.isEnabled = false
        buttonSaveAnother.isEnabled = false
        val categoryId = categories[spinnerCategory.selectedItemPosition].id
        worker.execute {
            repository.saveItem(editingItem?.id, categoryId, name, price, quantity)
            mainHandler.post {
                Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show()
                if (addAnother && editingItem == null) resetForNextItem() else finish()
            }
        }
    }

    private fun resetForNextItem() {
        inputName.text.clear()
        inputQuantity.setText("1")
        priceFormatter.setCents(0L)
        buttonSave.isEnabled = true
        buttonSaveAnother.isEnabled = true
        inputName.requestFocus()
    }

    private fun simpleWatcher(action: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = action()
        override fun afterTextChanged(s: Editable?) = Unit
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_CATEGORY_ID = "category_id"
    }
}
