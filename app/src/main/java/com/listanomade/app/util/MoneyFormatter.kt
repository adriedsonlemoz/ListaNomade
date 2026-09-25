package com.listanomade.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyFormatter {
    private val locale = Locale("pt", "BR")

    fun format(cents: Long): String = NumberFormat.getCurrencyInstance(locale).format(BigDecimal(cents).movePointLeft(2))

    fun parseToCents(raw: String): Long? {
        val normalized = raw.trim().replace("R$", "").replace(" ", "")
        if (normalized.isBlank()) return null
        val decimal = when {
            normalized.contains(',') -> normalized.replace(".", "").replace(',', '.')
            else -> normalized
        }
        return runCatching {
            BigDecimal(decimal)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()?.takeIf { it >= 0 }
    }

    fun centsToEditable(cents: Long): String = BigDecimal(cents)
        .divide(BigDecimal(100), 2, RoundingMode.UNNECESSARY)
        .toPlainString()
        .replace('.', ',')
}
