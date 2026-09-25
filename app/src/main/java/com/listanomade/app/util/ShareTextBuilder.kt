package com.listanomade.app.util

import com.listanomade.app.model.CategoryList

object ShareTextBuilder {
    fun category(list: CategoryList): String = buildString {
        appendLine(list.category.name)
        appendLine("${list.items.size} itens • ${list.resolvedCount} resolvidos")
        list.items.forEach { item ->
            val status = when {
                item.owned -> "Já tenho"
                item.fullyPurchased -> "Comprado"
                item.partial -> "${item.boughtQuantity}/${item.quantity} comprados"
                else -> "Pendente"
            }
            append("• ${item.name} — ${MoneyFormatter.format(item.effectiveCostCents)} — $status")
            if (item.store.isNotBlank()) append(" — ${item.store}")
            appendLine()
        }
        appendLine("Pendente: ${MoneyFormatter.format(list.pendingCents)}")
        append("Total efetivo: ${MoneyFormatter.format(list.totalCents)}")
    }

    fun all(categories: List<CategoryList>): String = buildString {
        appendLine("Lista Nômade")
        appendLine()
        categories.forEachIndexed { index, list ->
            appendLine(category(list))
            if (index < categories.lastIndex) appendLine("\n—")
        }
        appendLine()
        append("Total: ${MoneyFormatter.format(categories.sumOf { it.totalCents })}")
    }
}
