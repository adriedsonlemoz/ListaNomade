package com.listanomade.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.listanomade.app.model.Category
import com.listanomade.app.model.CategoryList
import com.listanomade.app.model.ShoppingItem

class ShoppingRepository(context: Context) {
    private val dbHelper = AppDatabase.getInstance(context)

    fun loadAll(): List<CategoryList> {
        val db = dbHelper.readableDatabase
        val categories = mutableListOf<Category>()
        db.query("categories", arrayOf("id", "name", "sort_order"), null, null, null, null, "sort_order, name COLLATE NOCASE").use { c ->
            while (c.moveToNext()) {
                categories += Category(c.getLong(0), c.getString(1), c.getInt(2))
            }
        }

        val itemsByCategory = mutableMapOf<Long, MutableList<ShoppingItem>>()
        db.query(
            "items",
            arrayOf("id", "category_id", "name", "unit_price_cents", "quantity", "total_cents", "purchased"),
            null, null, null, null,
            "category_id, purchased ASC, created_at DESC"
        ).use { c ->
            while (c.moveToNext()) {
                val item = ShoppingItem(
                    id = c.getLong(0),
                    categoryId = c.getLong(1),
                    name = c.getString(2),
                    unitPriceCents = c.getLong(3),
                    quantity = c.getInt(4),
                    totalCents = c.getLong(5),
                    purchased = c.getInt(6) == 1
                )
                itemsByCategory.getOrPut(item.categoryId) { mutableListOf() }.add(item)
            }
        }
        return categories.map { CategoryList(it, itemsByCategory[it.id].orEmpty()) }
    }

    fun getCategories(): List<Category> {
        val result = mutableListOf<Category>()
        dbHelper.readableDatabase.query(
            "categories", arrayOf("id", "name", "sort_order"),
            null, null, null, null, "sort_order, name COLLATE NOCASE"
        ).use { c ->
            while (c.moveToNext()) result += Category(c.getLong(0), c.getString(1), c.getInt(2))
        }
        return result
    }

    fun getItem(id: Long): ShoppingItem? {
        dbHelper.readableDatabase.query(
            "items",
            arrayOf("id", "category_id", "name", "unit_price_cents", "quantity", "total_cents", "purchased"),
            "id = ?", arrayOf(id.toString()), null, null, null, "1"
        ).use { c ->
            if (!c.moveToFirst()) return null
            return ShoppingItem(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getInt(4), c.getLong(5), c.getInt(6) == 1)
        }
    }

    fun addCategory(name: String): Boolean = try {
        val order = nextSortOrder()
        val values = ContentValues().apply {
            put("name", name.trim())
            put("sort_order", order)
        }
        dbHelper.writableDatabase.insertOrThrow("categories", null, values)
        true
    } catch (_: SQLiteConstraintException) {
        false
    }

    fun renameCategory(id: Long, name: String): Boolean = try {
        val values = ContentValues().apply { put("name", name.trim()) }
        dbHelper.writableDatabase.update("categories", values, "id = ?", arrayOf(id.toString())) > 0
    } catch (_: SQLiteConstraintException) {
        false
    }

    fun deleteCategory(id: Long) {
        dbHelper.writableDatabase.delete("categories", "id = ?", arrayOf(id.toString()))
    }

    fun saveItem(itemId: Long?, categoryId: Long, name: String, unitPriceCents: Long, quantity: Int): Long {
        val total = unitPriceCents * quantity
        val values = ContentValues().apply {
            put("category_id", categoryId)
            put("name", name.trim())
            put("unit_price_cents", unitPriceCents)
            put("quantity", quantity)
            put("total_cents", total)
            if (itemId == null) put("created_at", System.currentTimeMillis())
        }
        val db = dbHelper.writableDatabase
        return if (itemId == null) {
            db.insertOrThrow("items", null, values)
        } else {
            db.update("items", values, "id = ?", arrayOf(itemId.toString()))
            itemId
        }
    }

    fun setPurchased(id: Long, purchased: Boolean) {
        val values = ContentValues().apply { put("purchased", if (purchased) 1 else 0) }
        dbHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun deleteItem(id: Long) {
        dbHelper.writableDatabase.delete("items", "id = ?", arrayOf(id.toString()))
    }

    private fun nextSortOrder(): Int {
        dbHelper.readableDatabase.rawQuery("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
