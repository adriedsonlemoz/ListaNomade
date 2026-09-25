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
    val productUrl: String = ""
) {
    val pending: Boolean get() = !purchased && !owned
    val resolved: Boolean get() = purchased || owned
    val actualTotalCents: Long
        get() = if (purchased) (actualUnitPriceCents.takeIf { it > 0L } ?: unitPriceCents) * quantity else 0L
    val effectiveCostCents: Long
        get() = when {
            owned -> 0L
            purchased -> actualTotalCents
            else -> totalCents
        }
    val savingsCents: Long
        get() = if (purchased) totalCents - actualTotalCents else 0L

    companion object {
        const val PRIORITY_ESSENTIAL = "essential"
        const val PRIORITY_IMPORTANT = "important"
        const val PRIORITY_OPTIONAL = "optional"
    }
}
