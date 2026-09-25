package com.listanomade.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.listanomade.app.model.Category
import com.listanomade.app.model.CategoryList
import com.listanomade.app.model.ShoppingItem
import org.json.JSONArray
import org.json.JSONObject

class ShoppingRepository(private val context: Context) {
    private val dbHelper = AppDatabase.getInstance(context)

    fun loadAll(includeArchived: Boolean = false): List<CategoryList> {
        val db = dbHelper.readableDatabase
        val categories = mutableListOf<Category>()
        val selection = if (includeArchived) null else "archived = 0"
        db.query(
            "categories", CATEGORY_COLUMNS, selection, null, null, null,
            "sort_order, name COLLATE NOCASE"
        ).use { c -> while (c.moveToNext()) categories += categoryFromCursor(c) }

        val ids = categories.map { it.id }.toSet()
        val itemsByCategory = mutableMapOf<Long, MutableList<ShoppingItem>>()
        db.query("items", ITEM_COLUMNS, null, null, null, null, "category_id, sort_order, created_at").use { c ->
            while (c.moveToNext()) {
                val item = itemFromCursor(c)
                if (item.categoryId in ids) itemsByCategory.getOrPut(item.categoryId) { mutableListOf() }.add(item)
            }
        }
        return categories.map { CategoryList(it, itemsByCategory[it.id].orEmpty()) }
    }

    fun loadArchived(): List<CategoryList> = loadAll(true).filter { it.category.archived }
    fun getCategories(includeArchived: Boolean = false): List<Category> = loadAll(includeArchived).map { it.category }

    fun getItem(id: Long): ShoppingItem? {
        dbHelper.readableDatabase.query("items", ITEM_COLUMNS, "id = ?", arrayOf(id.toString()), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) itemFromCursor(c) else null
        }
    }

    fun addCategory(name: String): Boolean = try {
        dbHelper.writableDatabase.insertOrThrow(
            "categories", null,
            ContentValues().apply { put("name", name.trim()); put("sort_order", nextCategorySortOrder()) }
        )
        true
    } catch (_: SQLiteConstraintException) { false }

    fun renameCategory(id: Long, name: String): Boolean = try {
        dbHelper.writableDatabase.update(
            "categories", ContentValues().apply { put("name", name.trim()) }, "id = ?", arrayOf(id.toString())
        ) > 0
    } catch (_: SQLiteConstraintException) { false }

    fun setCategoryBudget(id: Long, cents: Long) = updateCategory(id, "budget_cents", cents.coerceAtLeast(0L))
    fun setCategoryCollapsed(id: Long, collapsed: Boolean) = updateCategory(id, "collapsed", if (collapsed) 1 else 0)
    fun setCategoryArchived(id: Long, archived: Boolean) = updateCategory(id, "archived", if (archived) 1 else 0)
    fun deleteCategory(id: Long) { dbHelper.writableDatabase.delete("categories", "id = ?", arrayOf(id.toString())) }

    fun saveItem(
        itemId: Long?,
        categoryId: Long,
        name: String,
        unitPriceCents: Long,
        quantity: Int,
        purchased: Boolean,
        owned: Boolean,
        purchasedQuantity: Int,
        priority: String,
        actualUnitPriceCents: Long,
        store: String,
        productUrl: String,
        targetDateMillis: Long
    ): Long {
        val before = itemId?.let(::getItem)
        val safeQuantity = quantity.coerceAtLeast(1)
        val bought = when {
            owned -> 0
            purchased -> safeQuantity
            else -> purchasedQuantity.coerceIn(0, safeQuantity)
        }
        val full = !owned && bought >= safeQuantity
        val values = ContentValues().apply {
            put("category_id", categoryId)
            put("name", name.trim())
            put("unit_price_cents", unitPriceCents.coerceAtLeast(0L))
            put("quantity", safeQuantity)
            put("total_cents", unitPriceCents.coerceAtLeast(0L) * safeQuantity)
            put("purchased", if (full) 1 else 0)
            put("purchased_quantity", bought)
            put("owned", if (owned) 1 else 0)
            put("priority", sanitizePriority(priority))
            put("actual_unit_price_cents", if (bought > 0 && !owned) actualUnitPriceCents.coerceAtLeast(0L) else 0L)
            put("store", store.trim())
            put("product_url", productUrl.trim())
            put("target_date", targetDateMillis.coerceAtLeast(0L))
            if (itemId == null) {
                put("created_at", System.currentTimeMillis())
                put("sort_order", nextItemSortOrder(categoryId))
            }
        }
        val db = dbHelper.writableDatabase
        val id = if (itemId == null) db.insertOrThrow("items", null, values) else {
            db.update("items", values, "id = ?", arrayOf(itemId.toString())); itemId
        }
        val after = getItem(id)
        if (after != null) PriceHistoryRepository(context).recordIfChanged(before, after)
        return id
    }

    fun addImportedItems(categoryId: Long, items: List<BulkImportParser.ParsedItem>) {
        items.forEach { parsed ->
            saveItem(null, categoryId, parsed.name, parsed.unitPriceCents, parsed.quantity, false, false, 0,
                ShoppingItem.PRIORITY_IMPORTANT, 0L, "", "", 0L)
        }
    }

    fun setPurchased(id: Long, purchased: Boolean) {
        val item = getItem(id) ?: return
        val values = ContentValues().apply {
            put("purchased", if (purchased) 1 else 0)
            put("purchased_quantity", if (purchased) item.quantity else 0)
            if (purchased) put("owned", 0) else put("actual_unit_price_cents", 0)
        }
        dbHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun setOwned(id: Long, owned: Boolean) {
        val values = ContentValues().apply {
            put("owned", if (owned) 1 else 0)
            if (owned) {
                put("purchased", 0); put("purchased_quantity", 0); put("actual_unit_price_cents", 0)
            }
        }
        dbHelper.writableDatabase.update("items", values, "id = ?", arrayOf(id.toString()))
    }

    fun setPending(id: Long) {
        dbHelper.writableDatabase.update(
            "items",
            ContentValues().apply {
                put("purchased", 0); put("purchased_quantity", 0); put("owned", 0); put("actual_unit_price_cents", 0)
            },
            "id = ?", arrayOf(id.toString())
        )
    }

    fun deleteItem(id: Long) { dbHelper.writableDatabase.delete("items", "id = ?", arrayOf(id.toString())) }

    fun restoreItem(item: ShoppingItem): Long {
        val values = itemValues(item).apply { put("id", item.id); put("created_at", System.currentTimeMillis()) }
        return try { dbHelper.writableDatabase.insertOrThrow("items", null, values) }
        catch (_: SQLiteConstraintException) {
            values.remove("id"); dbHelper.writableDatabase.insertOrThrow("items", null, values)
        }
    }

    fun duplicateItem(id: Long): Long? {
        val item = getItem(id) ?: return null
        val copy = item.copy(purchased = false, owned = false, purchasedQuantity = 0, actualUnitPriceCents = 0L)
        val values = itemValues(copy).apply {
            put("created_at", System.currentTimeMillis()); put("sort_order", nextItemSortOrder(item.categoryId))
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
        val items = loadAll(true).firstOrNull { it.category.id == item.categoryId }?.items.orEmpty()
        val index = items.indexOfFirst { it.id == id }
        val target = index + direction
        if (index !in items.indices || target !in items.indices) return
        swapSort("items", items[index].id, items[index].sortOrder, items[target].id, items[target].sortOrder)
    }

    fun stores(): List<String> = loadAll().flatMap { it.items }.map { it.store.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }

    fun exportDataJson(): JSONObject {
        val root = JSONObject().put("schema", 4)
        val categoriesJson = JSONArray()
        loadAll(true).forEach { list ->
            val category = list.category
            val categoryJson = JSONObject()
                .put("id", category.id).put("name", category.name).put("sortOrder", category.sortOrder)
                .put("budgetCents", category.budgetCents).put("collapsed", category.collapsed).put("archived", category.archived)
            val itemsJson = JSONArray()
            list.items.forEach { item ->
                itemsJson.put(JSONObject()
                    .put("id", item.id).put("name", item.name).put("unitPriceCents", item.unitPriceCents)
                    .put("quantity", item.quantity).put("totalCents", item.totalCents).put("purchased", item.fullyPurchased)
                    .put("purchasedQuantity", item.boughtQuantity).put("owned", item.owned).put("priority", item.priority)
                    .put("actualUnitPriceCents", item.actualUnitPriceCents).put("store", item.store).put("productUrl", item.productUrl)
                    .put("targetDateMillis", item.targetDateMillis).put("sortOrder", item.sortOrder).put("priceHistory", historyJson(item.id)))
            }
            categoryJson.put("items", itemsJson); categoriesJson.put(categoryJson)
        }
        return root.put("categories", categoriesJson)
    }

    fun importDataJson(root: JSONObject) {
        val categories = root.optJSONArray("categories") ?: throw IllegalArgumentException("Backup inválido")
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("categories", null, null)
            for (i in 0 until categories.length()) {
                val category = categories.getJSONObject(i)
                val categoryId = db.insertOrThrow("categories", null, ContentValues().apply {
                    category.optLong("id", i.toLong() + 1).takeIf { it > 0 }?.let { put("id", it) }
                    put("name", category.getString("name")); put("sort_order", category.optInt("sortOrder", i))
                    put("budget_cents", category.optLong("budgetCents", 0L)); put("collapsed", boolInt(category.optBoolean("collapsed", false)))
                    put("archived", boolInt(category.optBoolean("archived", false)))
                })
                val items = category.optJSONArray("items") ?: JSONArray()
                for (j in 0 until items.length()) insertImportedItem(db, categoryId, items.getJSONObject(j), j)
            }
            if (categories.length() == 0) db.execSQL("INSERT INTO categories(name, sort_order) VALUES('Bicicleta', 0)")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun insertImportedItem(db: SQLiteDatabase, categoryId: Long, item: JSONObject, order: Int) {
        val unit = item.optLong("unitPriceCents", 0L).coerceAtLeast(0L)
        val quantity = item.optInt("quantity", 1).coerceAtLeast(1)
        val purchasedQuantity = item.optInt("purchasedQuantity", if (item.optBoolean("purchased", false)) quantity else 0).coerceIn(0, quantity)
        val itemId = db.insertOrThrow("items", null, ContentValues().apply {
            item.optLong("id", 0L).takeIf { it > 0 }?.let { put("id", it) }
            put("category_id", categoryId); put("name", item.getString("name")); put("unit_price_cents", unit)
            put("quantity", quantity); put("total_cents", unit * quantity); put("purchased", boolInt(purchasedQuantity >= quantity))
            put("purchased_quantity", purchasedQuantity); put("owned", boolInt(item.optBoolean("owned", false)))
            put("priority", sanitizePriority(item.optString("priority", ShoppingItem.PRIORITY_IMPORTANT)))
            put("actual_unit_price_cents", item.optLong("actualUnitPriceCents", 0L).coerceAtLeast(0L))
            put("store", item.optString("store", "")); put("product_url", item.optString("productUrl", ""))
            put("target_date", item.optLong("targetDateMillis", 0L).coerceAtLeast(0L)); put("created_at", System.currentTimeMillis() + order)
            put("sort_order", item.optInt("sortOrder", order))
        })
        val history = item.optJSONArray("priceHistory") ?: return
        for (h in 0 until history.length()) {
            val entry = history.getJSONObject(h)
            db.insert("price_history", null, ContentValues().apply {
                put("item_id", itemId); put("expected_unit_price_cents", entry.optLong("expected", unit))
                put("actual_unit_price_cents", entry.optLong("actual", 0L)); put("changed_at", entry.optLong("changedAt", System.currentTimeMillis()))
            })
        }
    }

    private fun historyJson(itemId: Long): JSONArray {
        val array = JSONArray()
        dbHelper.readableDatabase.query(
            "price_history", arrayOf("expected_unit_price_cents", "actual_unit_price_cents", "changed_at"),
            "item_id = ?", arrayOf(itemId.toString()), null, null, "changed_at"
        ).use { c -> while (c.moveToNext()) array.put(JSONObject().put("expected", c.getLong(0)).put("actual", c.getLong(1)).put("changedAt", c.getLong(2))) }
        return array
    }

    private fun categoryFromCursor(c: Cursor) = Category(c.getLong(0), c.getString(1), c.getInt(2), c.getLong(3), c.getInt(4) == 1, c.getInt(5) == 1)

    private fun itemFromCursor(c: Cursor) = ShoppingItem(
        id = c.getLong(0), categoryId = c.getLong(1), name = c.getString(2), unitPriceCents = c.getLong(3), quantity = c.getInt(4),
        totalCents = c.getLong(5), purchased = c.getInt(6) == 1, sortOrder = c.getInt(7), owned = c.getInt(8) == 1,
        priority = sanitizePriority(c.getString(9)), actualUnitPriceCents = c.getLong(10), store = c.getString(11).orEmpty(),
        productUrl = c.getString(12).orEmpty(), purchasedQuantity = c.getInt(13), targetDateMillis = c.getLong(14)
    )

    private fun itemValues(item: ShoppingItem) = ContentValues().apply {
        put("category_id", item.categoryId); put("name", item.name); put("unit_price_cents", item.unitPriceCents); put("quantity", item.quantity)
        put("total_cents", item.totalCents); put("purchased", boolInt(item.fullyPurchased)); put("sort_order", item.sortOrder); put("owned", boolInt(item.owned))
        put("priority", sanitizePriority(item.priority)); put("actual_unit_price_cents", item.actualUnitPriceCents); put("store", item.store)
        put("product_url", item.productUrl); put("purchased_quantity", item.boughtQuantity); put("target_date", item.targetDateMillis)
    }

    private fun sanitizePriority(value: String): String = when (value) {
        ShoppingItem.PRIORITY_ESSENTIAL, ShoppingItem.PRIORITY_OPTIONAL -> value
        else -> ShoppingItem.PRIORITY_IMPORTANT
    }

    private fun updateCategory(id: Long, field: String, value: Any) {
        val values = ContentValues()
        when (value) { is Long -> values.put(field, value); is Int -> values.put(field, value); else -> values.put(field, value.toString()) }
        dbHelper.writableDatabase.update("categories", values, "id = ?", arrayOf(id.toString()))
    }

    private fun swapSort(table: String, id1: Long, order1: Int, id2: Long, order2: Int) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.update(table, ContentValues().apply { put("sort_order", order2) }, "id = ?", arrayOf(id1.toString()))
            db.update(table, ContentValues().apply { put("sort_order", order1) }, "id = ?", arrayOf(id2.toString()))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun nextCategorySortOrder(): Int = scalarInt("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories")
    private fun nextItemSortOrder(categoryId: Long): Int = scalarInt("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM items WHERE category_id = ?", arrayOf(categoryId.toString()))
    private fun scalarInt(sql: String, args: Array<String>? = null): Int {
        dbHelper.readableDatabase.rawQuery(sql, args).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }
    private fun boolInt(value: Boolean) = if (value) 1 else 0

    companion object {
        private val CATEGORY_COLUMNS = arrayOf("id", "name", "sort_order", "budget_cents", "collapsed", "archived")
        private val ITEM_COLUMNS = arrayOf(
            "id", "category_id", "name", "unit_price_cents", "quantity", "total_cents", "purchased", "sort_order", "owned", "priority",
            "actual_unit_price_cents", "store", "product_url", "purchased_quantity", "target_date"
        )
    }
}
