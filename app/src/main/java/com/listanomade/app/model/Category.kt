package com.listanomade.app.model

data class Category(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val budgetCents: Long = 0L,
    val collapsed: Boolean = false
)
