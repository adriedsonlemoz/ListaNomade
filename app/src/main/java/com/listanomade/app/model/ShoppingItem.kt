package com.listanomade.app.model

data class ShoppingItem(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val totalCents: Long,
    val purchased: Boolean
)
