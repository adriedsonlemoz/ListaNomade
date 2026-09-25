package com.listanomade.app.ui.item

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
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
    private lateinit var inputPaidPrice: EditText
    private lateinit var inputQuantity: EditText
    private lateinit var inputStore: EditText
    private lateinit var inputProductUrl: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerPriority: Spinner
    private lateinit var textTotal: TextView
    private lateinit var textPaidTotal: TextView
    private lateinit var containerPaidPrice: View
    private lateinit var containerPaidTotal: View
    private lateinit var buttonSave: Button
    private lateinit var buttonSaveAnother: Button
    private lateinit var priceFormatter: CurrencyInputFormatter
    private lateinit var paidPriceFormatter: CurrencyInputFormatter
    private var categories: List<Category> = emptyList()
    private var editingItem: ShoppingItem? = null
    private var currentPriceCents = 0L
    private var currentPaidPriceCents = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)
        SystemBarInsets.apply(findViewById(R.id.rootAddItem))
        repository = ShoppingRepository(this)
        bindViews()
        configureSpinners()
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
        inputPaidPrice = findViewById(R.id.inputPaidPrice)
        inputQuantity = findViewById(R.id.inputQuantity)
        inputStore = findViewById(R.id.inputStore)
        inputProductUrl = findViewById(R.id.inputProductUrl)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        textTotal = findViewById(R.id.textCalculatedTotal)
        textPaidTotal = findViewById(R.id.textCalculatedPaidTotal)
        containerPaidPrice = findViewById(R.id.containerPaidPrice)
        containerPaidTotal = findViewById(R.id.containerPaidTotal)
        buttonSave = findViewById(R.id.buttonSave)
        buttonSaveAnother = findViewById(R.id.buttonSaveAnother)
        buttonSave.isEnabled = false
        buttonSaveAnother.isEnabled = false
        findViewById<TextView>(R.id.textTitle).setText(
            if (intent.hasExtra(EXTRA_ITEM_ID)) R.string.edit_item_title else R.string.add_item_title
        )
    }

    private fun configureSpinners() {
        spinnerStatus.adapter = simpleSpinner(listOf(
            getString(R.string.not_purchased), getString(R.string.purchased), getString(R.string.already_have)
        ))
        spinnerPriority.adapter = simpleSpinner(listOf(
            getString(R.string.priority_essential), getString(R.string.priority_important), getString(R.string.priority_optional)
        ))
        spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updatePaidVisibility()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun simpleSpinner(values: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, values).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun configureActions() {
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        priceFormatter = CurrencyInputFormatter(inputPrice) { cents ->
            currentPriceCents = cents
            updateCalculatedTotal()
        }
        paidPriceFormatter = CurrencyInputFormatter(inputPaidPrice) { cents ->
            currentPaidPriceCents = cents
            updateCalculatedTotal()
        }
        inputPrice.addTextChangedListener(priceFormatter)
        inputPaidPrice.addTextChangedListener(paidPriceFormatter)
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
        spinnerCategory.adapter = simpleSpinner(categories.map { it.name })
        val preferredCategoryId = item?.categoryId ?: intent.getLongExtra(EXTRA_CATEGORY_ID, -1L).takeIf { it > 0 }
        val index = categories.indexOfFirst { it.id == preferredCategoryId }.takeIf { it >= 0 } ?: 0
        if (categories.isNotEmpty()) spinnerCategory.setSelection(index)

        inputQuantity.setText((item?.quantity ?: 1).toString())
        priceFormatter.setCents(item?.unitPriceCents ?: 0L)
        paidPriceFormatter.setCents(item?.actualUnitPriceCents ?: 0L)
        inputName.setText(item?.name.orEmpty())
        inputStore.setText(item?.store.orEmpty())
        inputProductUrl.setText(item?.productUrl.orEmpty())
        spinnerStatus.setSelection(when {
            item?.owned == true -> STATUS_OWNED
            item?.purchased == true -> STATUS_PURCHASED
            else -> STATUS_PENDING
        })
        spinnerPriority.setSelection(when (item?.priority) {
            ShoppingItem.PRIORITY_ESSENTIAL -> 0
            ShoppingItem.PRIORITY_OPTIONAL -> 2
            else -> 1
        })
        updatePaidVisibility()
        updateCalculatedTotal()

        val enabled = categories.isNotEmpty()
        buttonSave.isEnabled = enabled
        buttonSaveAnother.isEnabled = enabled
        buttonSaveAnother.visibility = if (item == null) View.VISIBLE else View.GONE
        inputName.requestFocus()
    }

    private fun updatePaidVisibility() {
        val purchased = spinnerStatus.selectedItemPosition == STATUS_PURCHASED
        containerPaidPrice.visibility = if (purchased) View.VISIBLE else View.GONE
        containerPaidTotal.visibility = if (purchased) View.VISIBLE else View.GONE
        updateCalculatedTotal()
    }

    private fun updateCalculatedTotal() {
        val quantity = inputQuantity.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        textTotal.text = MoneyFormatter.format(currentPriceCents * quantity)
        val paidUnit = currentPaidPriceCents.takeIf { it > 0L } ?: currentPriceCents
        textPaidTotal.text = MoneyFormatter.format(paidUnit * quantity)
    }

    private fun validateAndSave(addAnother: Boolean) {
        val name = inputName.text.toString().trim()
        val quantity = inputQuantity.text.toString().toIntOrNull()
        when {
            name.isBlank() -> inputName.error = getString(R.string.invalid_name)
            currentPriceCents < 0L -> inputPrice.error = getString(R.string.invalid_price)
            quantity == null || quantity <= 0 -> inputQuantity.error = getString(R.string.invalid_quantity)
            categories.isEmpty() -> return
            else -> save(name, quantity, addAnother)
        }
    }

    private fun save(name: String, quantity: Int, addAnother: Boolean) {
        buttonSave.isEnabled = false
        buttonSaveAnother.isEnabled = false
        val categoryId = categories[spinnerCategory.selectedItemPosition].id
        val status = spinnerStatus.selectedItemPosition
        val priority = when (spinnerPriority.selectedItemPosition) {
            0 -> ShoppingItem.PRIORITY_ESSENTIAL
            2 -> ShoppingItem.PRIORITY_OPTIONAL
            else -> ShoppingItem.PRIORITY_IMPORTANT
        }
        worker.execute {
            repository.saveItem(
                editingItem?.id, categoryId, name, currentPriceCents, quantity,
                purchased = status == STATUS_PURCHASED,
                owned = status == STATUS_OWNED,
                priority = priority,
                actualUnitPriceCents = currentPaidPriceCents,
                store = inputStore.text.toString(),
                productUrl = inputProductUrl.text.toString()
            )
            mainHandler.post {
                Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show()
                if (addAnother && editingItem == null) resetForNextItem() else finish()
            }
        }
    }

    private fun resetForNextItem() {
        inputName.text.clear()
        inputQuantity.setText("1")
        inputStore.text.clear()
        inputProductUrl.text.clear()
        priceFormatter.setCents(0L)
        paidPriceFormatter.setCents(0L)
        spinnerStatus.setSelection(STATUS_PENDING)
        spinnerPriority.setSelection(1)
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
        private const val STATUS_PENDING = 0
        private const val STATUS_PURCHASED = 1
        private const val STATUS_OWNED = 2
    }
}
