package com.listanomade.app.ui.item

import android.app.DatePickerDialog
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
import com.listanomade.app.util.DateFormatter
import com.listanomade.app.util.MoneyFormatter
import com.listanomade.app.util.SystemBarInsets
import com.listanomade.app.util.ThemeManager
import java.util.Calendar
import java.util.concurrent.Executors

class AddItemActivity : AppCompatActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var repository: ShoppingRepository
    private lateinit var inputName: EditText
    private lateinit var inputPrice: EditText
    private lateinit var inputPaidPrice: EditText
    private lateinit var inputQuantity: EditText
    private lateinit var inputPurchasedQuantity: EditText
    private lateinit var inputStore: EditText
    private lateinit var inputProductUrl: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerPriority: Spinner
    private lateinit var textTotal: TextView
    private lateinit var textPaidTotal: TextView
    private lateinit var containerPaidPrice: View
    private lateinit var containerPaidTotal: View
    private lateinit var containerPurchasedQuantity: View
    private lateinit var buttonTargetDate: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonSaveAnother: Button
    private lateinit var priceFormatter: CurrencyInputFormatter
    private lateinit var paidPriceFormatter: CurrencyInputFormatter
    private var categories: List<Category> = emptyList()
    private var editingItem: ShoppingItem? = null
    private var currentPriceCents = 0L
    private var currentPaidPriceCents = 0L
    private var targetDateMillis = 0L

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
        inputPurchasedQuantity = findViewById(R.id.inputPurchasedQuantity)
        inputStore = findViewById(R.id.inputStore)
        inputProductUrl = findViewById(R.id.inputProductUrl)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        textTotal = findViewById(R.id.textCalculatedTotal)
        textPaidTotal = findViewById(R.id.textCalculatedPaidTotal)
        containerPaidPrice = findViewById(R.id.containerPaidPrice)
        containerPaidTotal = findViewById(R.id.containerPaidTotal)
        containerPurchasedQuantity = findViewById(R.id.containerPurchasedQuantity)
        buttonTargetDate = findViewById(R.id.buttonTargetDate)
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
            getString(R.string.not_purchased),
            getString(R.string.purchased),
            getString(R.string.partial_purchase),
            getString(R.string.already_have)
        ))
        spinnerPriority.adapter = simpleSpinner(listOf(
            getString(R.string.priority_essential), getString(R.string.priority_important), getString(R.string.priority_optional)
        ))
        spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updateStatusVisibility()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun simpleSpinner(values: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, values).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun configureActions() {
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        priceFormatter = CurrencyInputFormatter(inputPrice) { cents -> currentPriceCents = cents; updateCalculatedTotal() }
        paidPriceFormatter = CurrencyInputFormatter(inputPaidPrice) { cents -> currentPaidPriceCents = cents; updateCalculatedTotal() }
        inputPrice.addTextChangedListener(priceFormatter)
        inputPaidPrice.addTextChangedListener(paidPriceFormatter)
        inputQuantity.addTextChangedListener(simpleWatcher { updateCalculatedTotal() })
        inputPurchasedQuantity.addTextChangedListener(simpleWatcher { updateCalculatedTotal() })
        buttonTargetDate.setOnClickListener { chooseTargetDate() }
        findViewById<Button>(R.id.buttonClearTargetDate).setOnClickListener { targetDateMillis = 0L; renderTargetDate() }
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
        inputPurchasedQuantity.setText((item?.boughtQuantity ?: 0).toString())
        priceFormatter.setCents(item?.unitPriceCents ?: 0L)
        paidPriceFormatter.setCents(item?.actualUnitPriceCents ?: 0L)
        inputName.setText(item?.name.orEmpty())
        inputStore.setText(item?.store.orEmpty())
        inputProductUrl.setText(item?.productUrl.orEmpty())
        targetDateMillis = item?.targetDateMillis ?: 0L
        spinnerStatus.setSelection(when {
            item?.owned == true -> STATUS_OWNED
            item?.partial == true -> STATUS_PARTIAL
            item?.fullyPurchased == true -> STATUS_PURCHASED
            else -> STATUS_PENDING
        })
        spinnerPriority.setSelection(when (item?.priority) {
            ShoppingItem.PRIORITY_ESSENTIAL -> 0
            ShoppingItem.PRIORITY_OPTIONAL -> 2
            else -> 1
        })
        renderTargetDate()
        updateStatusVisibility()
        updateCalculatedTotal()

        val enabled = categories.isNotEmpty()
        buttonSave.isEnabled = enabled
        buttonSaveAnother.isEnabled = enabled
        buttonSaveAnother.visibility = if (item == null) View.VISIBLE else View.GONE
        inputName.requestFocus()
    }

    private fun updateStatusVisibility() {
        val status = spinnerStatus.selectedItemPosition
        val hasPaid = status == STATUS_PURCHASED || status == STATUS_PARTIAL
        containerPaidPrice.visibility = if (hasPaid) View.VISIBLE else View.GONE
        containerPaidTotal.visibility = if (hasPaid) View.VISIBLE else View.GONE
        containerPurchasedQuantity.visibility = if (status == STATUS_PARTIAL) View.VISIBLE else View.GONE
        updateCalculatedTotal()
    }

    private fun updateCalculatedTotal() {
        val quantity = inputQuantity.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        textTotal.text = MoneyFormatter.format(currentPriceCents * quantity)
        val bought = when (spinnerStatus.selectedItemPosition) {
            STATUS_PURCHASED -> quantity
            STATUS_PARTIAL -> inputPurchasedQuantity.text?.toString()?.toIntOrNull()?.coerceIn(0, quantity) ?: 0
            else -> 0
        }
        val paidUnit = currentPaidPriceCents.takeIf { it > 0L } ?: currentPriceCents
        textPaidTotal.text = MoneyFormatter.format(paidUnit * bought)
    }

    private fun chooseTargetDate() {
        val calendar = Calendar.getInstance().apply {
            if (targetDateMillis > 0L) timeInMillis = targetDateMillis
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 12, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                targetDateMillis = calendar.timeInMillis
                renderTargetDate()
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun renderTargetDate() {
        buttonTargetDate.text = if (targetDateMillis > 0L) DateFormatter.format(targetDateMillis) else getString(R.string.no_target_date)
    }

    private fun validateAndSave(addAnother: Boolean) {
        val name = inputName.text.toString().trim()
        val quantity = inputQuantity.text.toString().toIntOrNull()
        val partialQuantity = inputPurchasedQuantity.text.toString().toIntOrNull() ?: 0
        when {
            name.isBlank() -> inputName.error = getString(R.string.invalid_name)
            currentPriceCents < 0L -> inputPrice.error = getString(R.string.invalid_price)
            quantity == null || quantity <= 0 -> inputQuantity.error = getString(R.string.invalid_quantity)
            spinnerStatus.selectedItemPosition == STATUS_PARTIAL && partialQuantity !in 1 until quantity ->
                inputPurchasedQuantity.error = getString(R.string.invalid_purchased_quantity)
            categories.isEmpty() -> return
            else -> save(name, quantity, partialQuantity, addAnother)
        }
    }

    private fun save(name: String, quantity: Int, partialQuantity: Int, addAnother: Boolean) {
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
                editingItem?.id,
                categoryId,
                name,
                currentPriceCents,
                quantity,
                purchased = status == STATUS_PURCHASED,
                owned = status == STATUS_OWNED,
                purchasedQuantity = if (status == STATUS_PARTIAL) partialQuantity else 0,
                priority = priority,
                actualUnitPriceCents = currentPaidPriceCents,
                store = inputStore.text.toString(),
                productUrl = inputProductUrl.text.toString(),
                targetDateMillis = targetDateMillis
            )
            mainHandler.post {
                Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show()
                if (addAnother && editingItem == null) resetForNextItem() else finish()
            }
        }
    }

    private fun resetForNextItem() {
        inputName.setText("")
        inputQuantity.setText("1")
        inputPurchasedQuantity.setText("0")
        inputStore.setText("")
        inputProductUrl.setText("")
        priceFormatter.setCents(0L)
        paidPriceFormatter.setCents(0L)
        targetDateMillis = 0L
        renderTargetDate()
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
        private const val STATUS_PARTIAL = 2
        private const val STATUS_OWNED = 3
    }
}
