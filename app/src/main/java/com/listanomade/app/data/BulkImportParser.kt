package com.listanomade.app.data

import java.math.BigDecimal
import java.math.RoundingMode

object BulkImportParser {
    data class ParsedItem(val name: String, val unitPriceCents: Long, val quantity: Int)

    fun parse(text: String): List<ParsedItem> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull(::parseLine)
        .toList()

    private fun parseLine(line: String): ParsedItem? {
        val parts = when {
            line.contains('|') -> line.split('|')
            line.contains(';') -> line.split(';')
            line.contains('\t') -> line.split('\t')
            else -> listOf(line)
        }.map { it.trim() }

        if (parts.size >= 2) {
            val name = parts[0]
            val price = parseMoney(parts[1]) ?: 0L
            val quantity = parts.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            return name.takeIf { it.isNotBlank() }?.let { ParsedItem(it, price, quantity) }
        }

        val match = Regex("^(.+?)\\s+-\\s+(?:R\\$\\s*)?([0-9.,]+)(?:\\s*[xX]\\s*(\\d+))?$").find(line)
        if (match != null) {
            val name = match.groupValues[1].trim()
            val price = parseMoney(match.groupValues[2]) ?: 0L
            val quantity = match.groupValues.getOrNull(3)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            return ParsedItem(name, price, quantity)
        }
        return ParsedItem(line, 0L, 1)
    }

    private fun parseMoney(raw: String): Long? {
        val clean = raw.replace("R$", "", ignoreCase = true).trim().replace(".", "").replace(',', '.')
        return runCatching {
            BigDecimal(clean).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull()
    }
}
