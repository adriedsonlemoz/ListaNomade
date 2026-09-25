package com.listanomade.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.listanomade.app.model.CategoryList
import com.listanomade.app.model.ShoppingTemplate
import org.json.JSONArray
import org.json.JSONObject

class TemplateRepository(context: Context) {
    private val dbHelper = AppDatabase.getInstance(context)

    fun list(): List<ShoppingTemplate> {
        val result = mutableListOf<ShoppingTemplate>()
        val sql = """
            SELECT t.id, t.name, t.created_at, COUNT(i.id)
            FROM templates t LEFT JOIN template_items i ON i.template_id = t.id
            GROUP BY t.id, t.name, t.created_at
            ORDER BY t.name COLLATE NOCASE
        """.trimIndent()
        dbHelper.readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) result += ShoppingTemplate(c.getLong(0), c.getString(1), c.getInt(3), c.getLong(2))
        }
        return result
    }

    fun saveFromCategory(list: CategoryList, templateName: String): Boolean {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val templateId = db.insertOrThrow(
                "templates", null,
                ContentValues().apply { put("name", templateName.trim()); put("created_at", System.currentTimeMillis()) }
            )
            list.items.forEachIndexed { index, item ->
                db.insertOrThrow(
                    "template_items", null,
                    ContentValues().apply {
                        put("template_id", templateId)
                        put("name", item.name)
                        put("unit_price_cents", item.unitPriceCents)
                        put("quantity", item.quantity)
                        put("priority", item.priority)
                        put("store", item.store)
                        put("product_url", item.productUrl)
                        put("sort_order", index)
                        put("target_date", item.targetDateMillis)
                    }
                )
            }
            db.setTransactionSuccessful()
            true
        } catch (_: SQLiteConstraintException) {
            false
        } finally {
            db.endTransaction()
        }
    }

    fun instantiate(templateId: Long, categoryName: String): Boolean {
        val nextOrder = scalarInt("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories")
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val categoryId = db.insertOrThrow(
                "categories", null,
                ContentValues().apply { put("name", categoryName.trim()); put("sort_order", nextOrder) }
            )
            db.query(
                "template_items",
                arrayOf("name", "unit_price_cents", "quantity", "priority", "store", "product_url", "sort_order", "target_date"),
                "template_id = ?", arrayOf(templateId.toString()), null, null, "sort_order"
            ).use { c ->
                while (c.moveToNext()) {
                    val unit = c.getLong(1)
                    val quantity = c.getInt(2).coerceAtLeast(1)
                    db.insertOrThrow(
                        "items", null,
                        ContentValues().apply {
                            put("category_id", categoryId)
                            put("name", c.getString(0))
                            put("unit_price_cents", unit)
                            put("quantity", quantity)
                            put("total_cents", unit * quantity)
                            put("purchased", 0)
                            put("created_at", System.currentTimeMillis())
                            put("sort_order", c.getInt(6))
                            put("priority", c.getString(3))
                            put("store", c.getString(4).orEmpty())
                            put("product_url", c.getString(5).orEmpty())
                            put("target_date", c.getLong(7))
                        }
                    )
                }
            }
            db.setTransactionSuccessful()
            true
        } catch (_: SQLiteConstraintException) {
            false
        } finally {
            db.endTransaction()
        }
    }

    fun delete(templateId: Long) {
        dbHelper.writableDatabase.delete("templates", "id = ?", arrayOf(templateId.toString()))
    }

    fun exportJson(): JSONArray {
        val result = JSONArray()
        list().forEach { template ->
            val obj = JSONObject().put("name", template.name).put("createdAt", template.createdAt)
            val items = JSONArray()
            dbHelper.readableDatabase.query(
                "template_items",
                arrayOf("name", "unit_price_cents", "quantity", "priority", "store", "product_url", "sort_order", "target_date"),
                "template_id = ?", arrayOf(template.id.toString()), null, null, "sort_order"
            ).use { c ->
                while (c.moveToNext()) items.put(JSONObject()
                    .put("name", c.getString(0)).put("unitPriceCents", c.getLong(1)).put("quantity", c.getInt(2))
                    .put("priority", c.getString(3)).put("store", c.getString(4).orEmpty()).put("productUrl", c.getString(5).orEmpty())
                    .put("sortOrder", c.getInt(6)).put("targetDateMillis", c.getLong(7)))
            }
            result.put(obj.put("items", items))
        }
        return result
    }

    fun replaceFromJson(array: JSONArray?) {
        if (array == null) return
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("templates", null, null)
            for (i in 0 until array.length()) {
                val template = array.getJSONObject(i)
                val templateId = db.insertOrThrow("templates", null, ContentValues().apply {
                    put("name", template.getString("name")); put("created_at", template.optLong("createdAt", System.currentTimeMillis()))
                })
                val items = template.optJSONArray("items") ?: JSONArray()
                for (j in 0 until items.length()) {
                    val item = items.getJSONObject(j)
                    db.insertOrThrow("template_items", null, ContentValues().apply {
                        put("template_id", templateId); put("name", item.getString("name"))
                        put("unit_price_cents", item.optLong("unitPriceCents", 0L)); put("quantity", item.optInt("quantity", 1).coerceAtLeast(1))
                        put("priority", item.optString("priority", "important")); put("store", item.optString("store", ""))
                        put("product_url", item.optString("productUrl", "")); put("sort_order", item.optInt("sortOrder", j))
                        put("target_date", item.optLong("targetDateMillis", 0L))
                    })
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun scalarInt(sql: String): Int {
        dbHelper.readableDatabase.rawQuery(sql, null).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }
}
