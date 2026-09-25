package com.listanomade.app.model

data class PriceHistoryEntry(
    val expectedUnitPriceCents: Long,
    val actualUnitPriceCents: Long,
    val changedAt: Long
)
