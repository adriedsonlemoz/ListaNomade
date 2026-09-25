package com.listanomade.app.model

data class CategoryList(
    val category: Category,
    val items: List<ShoppingItem>,
    val visibleItems: List<ShoppingItem> = items
) {
    val totalCents: Long get() = items.sumOf { it.totalCents }
    val pendingCents: Long get() = items.filterNot { it.purchased }.sumOf { it.totalCents }
    val purchasedCents: Long get() = items.filter { it.purchased }.sumOf { it.totalCents }
    val purchasedCount: Int get() = items.count { it.purchased }
    val pendingCount: Int get() = items.size - purchasedCount
}
