package com.listanomade.app.ui.item

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
import com.listanomade.app.util.MoneyFormatter
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
    private var categories: List<Category> = emptyList()
    private var editingItem: ShoppingItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)
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
        buttonSave.isEnabled = false
        findViewById<TextView>(R.id.textTitle).setText(
            if (intent.hasExtra(EXTRA_ITEM_ID)) R.string.edit_item_title else R.string.add_item_title
        )
    }

    private fun configureActions() {
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateCalculatedTotal()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        inputPrice.addTextChangedListener(watcher)
        inputQuantity.addTextChangedListener(watcher)
        buttonSave.setOnClickListener { validateAndSave() }
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
        val names = categories.map { it.name }
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)

        val preferredCategoryId = item?.categoryId
            ?: intent.getLongExtra(EXTRA_CATEGORY_ID, -1L).takeIf { it > 0 }
        val categoryIndex = categories.indexOfFirst { it.id == preferredCategoryId }.takeIf { it >= 0 } ?: 0
        if (categories.isNotEmpty()) spinnerCategory.setSelection(categoryIndex)

        item?.let {
            inputName.setText(it.name)
            inputPrice.setText(MoneyFormatter.centsToEditable(it.unitPriceCents))
            inputQuantity.setText(it.quantity.toString())
        }
        buttonSave.isEnabled = categories.isNotEmpty()
        updateCalculatedTotal()
        inputName.requestFocus()
    }

    private fun updateCalculatedTotal() {
        val price = MoneyFormatter.parseToCents(inputPrice.text?.toString().orEmpty()) ?: 0L
        val quantity = inputQuantity.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        textTotal.text = MoneyFormatter.format(price * quantity)
    }

    private fun validateAndSave() {
        val name = inputName.text.toString().trim()
        val price = MoneyFormatter.parseToCents(inputPrice.text.toString())
        val quantity = inputQuantity.text.toString().toIntOrNull()
        when {
            name.isBlank() -> inputName.error = getString(R.string.invalid_name)
            price == null -> inputPrice.error = getString(R.string.invalid_price)
            quantity == null || quantity <= 0 -> inputQuantity.error = getString(R.string.invalid_quantity)
            categories.isEmpty() -> return
            else -> {
                buttonSave.isEnabled = false
                val categoryId = categories[spinnerCategory.selectedItemPosition].id
                worker.execute {
                    repository.saveItem(editingItem?.id, categoryId, name, price, quantity)
                    mainHandler.post {
                        Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_CATEGORY_ID = "category_id"
    }
}
