package com.listanomade.app.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class CurrencyInputFormatter(
    private val editText: EditText,
    private val onValueChanged: ((Long) -> Unit)? = null
) : TextWatcher {
    private var changing = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) {
        if (changing) return
        val digits = s?.toString().orEmpty().filter(Char::isDigit).trimStart('0')
        val cents = digits.ifBlank { "0" }.toLongOrNull()?.coerceAtMost(MAX_CENTS) ?: 0L
        val formatted = MoneyFormatter.format(cents)
        changing = true
        editText.setText(formatted)
        editText.setSelection(formatted.length)
        changing = false
        onValueChanged?.invoke(cents)
    }

    fun setCents(cents: Long) {
        changing = true
        val formatted = MoneyFormatter.format(cents.coerceAtLeast(0L))
        editText.setText(formatted)
        editText.setSelection(formatted.length)
        changing = false
        onValueChanged?.invoke(cents.coerceAtLeast(0L))
    }

    companion object {
        private const val MAX_CENTS = 999_999_999_99L
    }
}
