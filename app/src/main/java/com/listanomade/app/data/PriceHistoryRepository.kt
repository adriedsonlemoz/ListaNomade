package com.listanomade.app.data

import android.content.ContentValues
import android.content.Context
import com.listanomade.app.model.PriceHistoryEntry
import com.listanomade.app.model.ShoppingItem

class PriceHistoryRepository(context: Context) {
    private val dbHelper = AppDatabase.getInstance(context)

    fun recordIfChanged(before: ShoppingItem?, after: ShoppingItem) {
        if (before == null) return
        if (before.unitPriceCents == after.unitPriceCents && before.actualUnitPriceCents == after.actualUnitPriceCents) return
        val db = dbHelper.writableDatabase
        if (!hasMatchingLatest(before)) insert(before)
        insert(after)
    }

    fun list(itemId: Long): List<PriceHistoryEntry> {
        val result = mutableListOf<PriceHistoryEntry>()
        dbHelper.readableDatabase.query(
            "price_history",
            arrayOf("expected_unit_price_cents", "actual_unit_price_cents", "changed_at"),
            "item_id = ?",
            arrayOf(itemId.toString()),
            null, null,
            "changed_at DESC"
        ).use { c ->
            while (c.moveToNext()) result += PriceHistoryEntry(c.getLong(0), c.getLong(1), c.getLong(2))
        }
        return result
    }

    private fun hasMatchingLatest(item: ShoppingItem): Boolean {
        dbHelper.readableDatabase.query(
            "price_history",
            arrayOf("expected_unit_price_cents", "actual_unit_price_cents"),
            "item_id = ?",
            arrayOf(item.id.toString()),
            null, null,
            "changed_at DESC",
            "1"
        ).use { c ->
            return c.moveToFirst() && c.getLong(0) == item.unitPriceCents && c.getLong(1) == item.actualUnitPriceCents
        }
    }

    private fun insert(item: ShoppingItem) {
        dbHelper.writableDatabase.insert(
            "price_history",
            null,
            ContentValues().apply {
                put("item_id", item.id)
                put("expected_unit_price_cents", item.unitPriceCents)
                put("actual_unit_price_cents", item.actualUnitPriceCents)
                put("changed_at", System.currentTimeMillis())
            }
        )
    }
}
