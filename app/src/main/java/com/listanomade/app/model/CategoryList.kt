package com.listanomade.app.model

data class CategoryList(
    val category: Category,
    val items: List<ShoppingItem>,
    val visibleItems: List<ShoppingItem> = items
) {
    val totalCents: Long get() = items.sumOf { it.effectiveCostCents }
    val pendingCents: Long get() = items.filter { it.pending }.sumOf { it.totalCents }
    val purchasedCents: Long get() = items.filter { it.purchased }.sumOf { it.actualTotalCents }
    val ownedReferenceCents: Long get() = items.filter { it.owned }.sumOf { it.totalCents }
    val purchasedCount: Int get() = items.count { it.purchased }
    val ownedCount: Int get() = items.count { it.owned }
    val pendingCount: Int get() = items.count { it.pending }
    val resolvedCount: Int get() = purchasedCount + ownedCount
}
