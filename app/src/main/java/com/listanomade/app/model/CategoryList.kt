package com.listanomade.app.model

data class CategoryList(
    val category: Category,
    val items: List<ShoppingItem>,
    val visibleItems: List<ShoppingItem> = items
) {
    val totalCents: Long get() = items.sumOf { it.effectiveCostCents }
    val pendingCents: Long get() = items.sumOf { it.pendingExpectedCents }
    val purchasedCents: Long get() = items.sumOf { it.actualPurchasedCents }
    val ownedReferenceCents: Long get() = items.filter { it.owned }.sumOf { it.totalCents }
    val purchasedCount: Int get() = items.count { it.fullyPurchased }
    val partialCount: Int get() = items.count { it.partial }
    val ownedCount: Int get() = items.count { it.owned }
    val pendingCount: Int get() = items.count { it.pending }
    val resolvedCount: Int get() = purchasedCount + ownedCount
    val totalUnits: Int get() = items.sumOf { it.quantity }
    val resolvedUnits: Int get() = items.sumOf { if (it.owned) it.quantity else it.boughtQuantity }
}
