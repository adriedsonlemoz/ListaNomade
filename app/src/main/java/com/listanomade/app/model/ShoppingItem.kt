package com.listanomade.app.model

data class ShoppingItem(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val totalCents: Long,
    val purchased: Boolean,
    val sortOrder: Int = 0,
    val owned: Boolean = false,
    val priority: String = PRIORITY_IMPORTANT,
    val actualUnitPriceCents: Long = 0L,
    val store: String = "",
    val productUrl: String = "",
    val purchasedQuantity: Int = if (purchased) quantity else 0,
    val targetDateMillis: Long = 0L
) {
    val boughtQuantity: Int get() = if (owned) 0 else purchasedQuantity.coerceIn(0, quantity)
    val pendingQuantity: Int get() = if (owned) 0 else (quantity - boughtQuantity).coerceAtLeast(0)
    val fullyPurchased: Boolean get() = !owned && quantity > 0 && boughtQuantity >= quantity
    val partial: Boolean get() = !owned && boughtQuantity in 1 until quantity
    val pending: Boolean get() = !owned && pendingQuantity > 0
    val resolved: Boolean get() = owned || fullyPurchased
    val paidUnitCents: Long get() = actualUnitPriceCents.takeIf { it > 0L } ?: unitPriceCents
    val actualPurchasedCents: Long get() = paidUnitCents * boughtQuantity
    val pendingExpectedCents: Long get() = unitPriceCents * pendingQuantity
    val actualTotalCents: Long get() = actualPurchasedCents
    val effectiveCostCents: Long
        get() = if (owned) 0L else actualPurchasedCents + pendingExpectedCents
    val savingsCents: Long
        get() = if (boughtQuantity > 0) (unitPriceCents - paidUnitCents) * boughtQuantity else 0L

    companion object {
        const val PRIORITY_ESSENTIAL = "essential"
        const val PRIORITY_IMPORTANT = "important"
        const val PRIORITY_OPTIONAL = "optional"
    }
}
