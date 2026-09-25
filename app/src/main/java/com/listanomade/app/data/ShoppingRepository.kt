package com.listanomade.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.listanomade.app.model.Category
import com.listanomade.app.model.CategoryList
import com.listanomade.app.model.ShoppingItem
import org.json.JSONArray
import org.json.JSONObject

class ShoppingRepository(context: Context) {
    private val dbHelper = AppDatabase.getInstance(context)

    fun loadAll(): List<CategoryList> {
        val db = dbHelper.readableDatabase
        val categories = mutableListOf<Category>()
        db.query(
            "categories",
            arrayOf("id", "name", "sort_order", "budget_cents", "collapsed"),
            null, null, null, null,
            "sort_order, name COLLATE NOCASE"
        ).use { c ->
            while (c.moveToNext()) {
                categories += Category(c.getLong(0), c.getString(1), c.getInt(2), c.getLong(3), c.getInt(4) == 1)
            }
        }

        val itemsByCategory = mutableMapOf<Long, MutableList<ShoppingItem>>()
        db.query(
            "items",
            arrayOf("id", "category_id", "name", "unit_price_cents", "quantity", "total_cents", "purchased", "sort_order"),
            null, null, null, null,
            "category_id, sort_order, created_at"
        ).use { c ->
            while (c.moveToNext()) {
                val item = ShoppingItem(
                    c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3),
                    c.getInt(4), c.getLong(5), c.getInt(6) == 1, c.getInt(7)
                )
                itemsByCategory.getOrPut(item.categoryId) { mutableListOf() }.add(item)
            }
        }
        return categories.map { CategoryList(it, itemsByCategory[it.id].orEmpty()) }
    }

    fun getCategories(): List<Category> = loadAll().map { it.category }

    fun getItem(id: Long): ShoppingItem? {
        dbHelper.readableDatabase.query(
            "items",
            arrayOf("id", "category_id", "name", "unit_price_cents", "quantity", "total_cents", "purchased", "sort_order"),
            "id = ?", arrayOf(id.toString()), null, null, null, "1"
        ).use { c ->
            if (!c.moveToFirst()) return null
            return ShoppingItem(c.getLong(0), c.getLong(1), c.getString(2), c.getLong(3), c.getInt(4), c.getLong(5), c.getInt(6) == 1, c.getInt(7))
        }
    }

    fun addCategory(name: String): Boolean = try {
        val values = ContentValues().apply {
            put("name", name.trim())
            put("sort_order", nextCategorySortOrder())
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

    fun setCategoryBudget(id: Long, cents: Long) {
        val values = ContentValues().apply { put("budget_cents", cents.coerceAtLeast(0L)) }
        dbHelper.writableDatabase.update("categories", values, "id = ?", arrayOf(id.toString()))
    }

    fun setCategoryCollapsed(id: Long, collapsed: Boolean) {
        val values = ContentValues().apply { put("collapsed", if (collapsed) 1 else 0) }
        dbHelper.writableDatabase.update("categories", values, "id = ?", arrayOf(id.toString()))
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
            if (itemId == null) {
                put("created_at", System.currentTimeMillis())
                put("sort_order", nextItemSortOrder(categoryId))
            }
        }
        val db = dbHelper.writableDatabase
        return if (itemId == null) db.insertOrThrow("items", null, values)
        else {
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

    fun restoreItem(item: ShoppingItem): Long {
        val values = itemValues(item).apply {
            put("id", item.id)
            put("created_at", System.currentTimeMillis())
        }
        return try {
            dbHelper.writableDatabase.insertOrThrow("items", null, values)
        } catch (_: SQLiteConstraintException) {
            values.remove("id")
            dbHelper.writableDatabase.insertOrThrow("items", null, values)
        }
    }

    fun duplicateItem(id: Long): Long? {
        val item = getItem(id) ?: return null
        val values = itemValues(item).apply {
            put("created_at", System.currentTimeMillis())
            put("sort_order", nextItemSortOrder(item.categoryId))
        }
        return dbHelper.writableDatabase.insert("items", null, values).takeIf { it > 0 }
    }

    fun moveCategory(id: Long, direction: Int) {
        val ordered = loadAll().map { it.category }
        val index = ordered.indexOfFirst { it.id == id }
        val target = index + direction
        if (index !in ordered.indices || target !in ordered.indices) return
        swapSort("categories", ordered[index].id, ordered[index].sortOrder, ordered[target].id, ordered[target].sortOrder)
    }

    fun moveItem(id: Long, direction: Int) {
        val item = getItem(id) ?: return
        val items = loadAll().firstOrNull { it.category.id == item.categoryId }?.items.orEmpty()
        val index = items.indexOfFirst { it.id == id }
        val target = index + direction
        if (index !in items.indices || target !in items.indices) return
        swapSort("items", items[index].id, items[index].sortOrder, items[target].id, items[target].sortOrder)
    }

    fun exportDataJson(): JSONObject {
        val root = JSONObject().put("schema", 2)
        val categoriesJson = JSONArray()
        loadAll().forEach { list ->
            val category = list.category
            val categoryJson = JSONObject()
                .put("id", category.id)
                .put("name", category.name)
                .put("sortOrder", category.sortOrder)
                .put("budgetCents", category.budgetCents)
                .put("collapsed", category.collapsed)
            val itemsJson = JSONArray()
            list.items.forEach { item ->
                itemsJson.put(JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("unitPriceCents", item.unitPriceCents)
                    .put("quantity", item.quantity)
                    .put("totalCents", item.totalCents)
                    .put("purchased", item.purchased)
                    .put("sortOrder", item.sortOrder))
            }
            categoryJson.put("items", itemsJson)
            categoriesJson.put(categoryJson)
        }
        return root.put("categories", categoriesJson)
    }

    fun importDataJson(root: JSONObject) {
        val categories = root.optJSONArray("categories") ?: throw IllegalArgumentException("Backup inválido")
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("items", null, null)
            db.delete("categories", null, null)
            for (i in 0 until categories.length()) {
                val category = categories.getJSONObject(i)
                val categoryValues = ContentValues().apply {
                    put("id", category.optLong("id", i.toLong() + 1))
                    put("name", category.getString("name"))
                    put("sort_order", category.optInt("sortOrder", i))
                    put("budget_cents", category.optLong("budgetCents", 0L))
                    put("collapsed", if (category.optBoolean("collapsed", false)) 1 else 0)
                }
                val categoryId = db.insertOrThrow("categories", null, categoryValues)
                val items = category.optJSONArray("items") ?: JSONArray()
                for (j in 0 until items.length()) {
                    val item = items.getJSONObject(j)
                    val unit = item.getLong("unitPriceCents")
                    val quantity = item.getInt("quantity")
                    val itemValues = ContentValues().apply {
                        put("id", item.optLong("id", 0L).takeIf { it > 0 })
                        put("category_id", categoryId)
                        put("name", item.getString("name"))
                        put("unit_price_cents", unit)
                        put("quantity", quantity)
                        put("total_cents", unit * quantity)
                        put("purchased", if (item.optBoolean("purchased", false)) 1 else 0)
                        put("created_at", System.currentTimeMillis() + j)
                        put("sort_order", item.optInt("sortOrder", j))
                    }
                    if (itemValues.getAsLong("id") == 0L) itemValues.remove("id")
                    db.insertOrThrow("items", null, itemValues)
                }
            }
            if (categories.length() == 0) {
                db.execSQL("INSERT INTO categories(name, sort_order) VALUES('Bicicleta', 0)")
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun itemValues(item: ShoppingItem) = ContentValues().apply {
        put("category_id", item.categoryId)
        put("name", item.name)
        put("unit_price_cents", item.unitPriceCents)
        put("quantity", item.quantity)
        put("total_cents", item.totalCents)
        put("purchased", if (item.purchased) 1 else 0)
        put("sort_order", item.sortOrder)
    }

    private fun swapSort(table: String, id1: Long, order1: Int, id2: Long, order2: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.update(table, ContentValues().apply { put("sort_order", order2) }, "id = ?", arrayOf(id1.toString()))
            db.update(table, ContentValues().apply { put("sort_order", order1) }, "id = ?", arrayOf(id2.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun nextCategorySortOrder(): Int = scalarInt("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories")

    private fun nextItemSortOrder(categoryId: Long): Int = scalarInt(
        "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM items WHERE category_id = ?",
        arrayOf(categoryId.toString())
    )

    private fun scalarInt(sql: String, args: Array<String>? = null): Int {
        dbHelper.readableDatabase.rawQuery(sql, args).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }
}
