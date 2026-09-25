package com.listanomade.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

object DefaultCatalog {
    private data class DefaultItem(
        val name: String,
        val unitPriceCents: Long,
        val quantity: Int = 1,
        val aliases: List<String> = emptyList(),
        val moveExistingHere: Boolean = false,
        val owned: Boolean = false
    )

    private data class DefaultCategory(
        val name: String,
        val items: List<DefaultItem> = emptyList()
    )

    private val categories = listOf(
        DefaultCategory(
            "Bicicleta",
            listOf(
                DefaultItem("Pezinho / descanso", 1_900, aliases = listOf("Pezinho", "Descanso", "Descanso bike")),
                DefaultItem("Suporte impermeável para celular", 2_300, aliases = listOf("Suporte celular", "Suporte para celular")),
                DefaultItem("Sapatas de freio GTS (4 pares)", 1_399, aliases = listOf("Sapatas GTS", "Sapatas de freio")),
                DefaultItem("Kit 2 câmaras", 2_499, aliases = listOf("Câmara de Ar", "Câmara de ar", "2 câmaras", "Câmaras Kenda")),
                DefaultItem("Farol", 3_149, aliases = listOf("Farol bike", "Farol LED")),
                DefaultItem("Cola + 6 remendos", 1_600, aliases = listOf("Kit remendo", "Kit de remendo", "Remendos + cola"))
            )
        ),
        DefaultCategory(
            "Camping",
            listOf(
                DefaultItem("Lona 4 × 3 m", 3_890, aliases = listOf("Lona 4x3", "Lona 4×3")),
                DefaultItem("Fogareiro", 2_799),
                DefaultItem("Saco de dormir", 4_870),
                DefaultItem("Cartucho de gás", 1_400, quantity = 2, aliases = listOf("Gás", "Cartucho gás")),
                DefaultItem("Faca de camping", 1_000, aliases = listOf("Faca")),
                DefaultItem("Lanterna Voxo T9 recarregável", 2_900, aliases = listOf("Lanterna Voxo T9", "Voxo T9")),
                DefaultItem("Barraca Ontrek Iglu 4 pessoas", 8_645, aliases = listOf("Barraca Ontrek", "Ontrek Iglu 4 pessoas"), owned = true)
            )
        ),
        DefaultCategory(
            "Energia",
            listOf(
                DefaultItem(
                    "Bateria externa / Power bank Geonav 10.000 mAh 20 W",
                    13_768,
                    aliases = listOf("Power bank Geonav 10.000 mAh 20 W", "Power bank Geonav", "Geonav 10.000 mAh", "Bateria externa Geonav"),
                    moveExistingHere = true
                ),
                DefaultItem(
                    "Cabo USB-C PD 2 m",
                    1_676,
                    aliases = listOf("Cabo USB-C 2 m", "Cabo USB-C", "Cabo USB C 2m", "Cabo PD"),
                    moveExistingHere = true
                )
            )
        ),
        DefaultCategory(
            "Eletrônicos",
            listOf(
                DefaultItem("Tela Redmi Note 11 Pro+ 5G", 9_527, aliases = listOf("Tela RN11 Pro+", "Tela Redmi Note 11 Pro+"))
            )
        ),
        DefaultCategory(
            "Pesca",
            listOf(
                DefaultItem("Linha de pesca", 1_000, aliases = listOf("Linha")),
                DefaultItem("Anzol", 30, quantity = 10, aliases = listOf("Anzóis")),
                DefaultItem("Chumbada", 50, quantity = 6, aliases = listOf("Chumbadas"))
            )
        ),
        DefaultCategory("Alimentação"),
        DefaultCategory("Ferramentas"),
        DefaultCategory("Viagem"),
        DefaultCategory(
            "Outros",
            listOf(DefaultItem("Máquina de barba", 1_999, aliases = listOf("Máquina barba")))
        )
    )

    fun seedMissing(db: SQLiteDatabase) {
        var nextCategoryOrder = queryMaxOrder(db, "categories", null) + 1
        categories.forEach { defaultCategory ->
            nextCategoryOrder = seedCategory(db, defaultCategory, nextCategoryOrder)
        }
    }

    fun seedVersion6Migration(db: SQLiteDatabase) {
        var nextOrder = queryMaxOrder(db, "categories", null) + 1
        val energy = categories.first { it.name == "Energia" }
        nextOrder = seedCategory(db, energy, nextOrder)
        val camping = categories.first { it.name == "Camping" }
        val extras = camping.items.filter { it.name in VERSION_6_CAMPING_EXTRAS }
        seedCategory(db, DefaultCategory(camping.name, extras), nextOrder)
    }

    private fun seedCategory(db: SQLiteDatabase, defaultCategory: DefaultCategory, nextOrder: Int): Int {
        var nextCategoryOrder = nextOrder
        val categoryId = findCategoryId(db, defaultCategory.name) ?: db.insertOrThrow(
            "categories", null, ContentValues().apply {
                put("name", defaultCategory.name)
                put("sort_order", nextCategoryOrder++)
            }
        )
        var nextItemOrder = queryMaxOrder(db, "items", categoryId) + 1
        defaultCategory.items.forEach { item ->
            val names = listOf(item.name) + item.aliases
            val existing = if (item.moveExistingHere) findItemAnywhere(db, names) else findItem(db, categoryId, names)
            if (existing != null) {
                if (item.moveExistingHere && existing.second != categoryId) {
                    db.update(
                        "items",
                        ContentValues().apply {
                            put("category_id", categoryId)
                            put("sort_order", nextItemOrder++)
                            put("name", item.name)
                        },
                        "id = ?",
                        arrayOf(existing.first.toString())
                    )
                }
            } else {
                db.insertOrThrow(
                    "items", null, ContentValues().apply {
                        put("category_id", categoryId)
                        put("name", item.name)
                        put("unit_price_cents", item.unitPriceCents)
                        put("quantity", item.quantity)
                        put("total_cents", item.unitPriceCents * item.quantity)
                        put("purchased", 0)
                        put("owned", if (item.owned) 1 else 0)
                        put("purchased_quantity", 0)
                        put("created_at", System.currentTimeMillis())
                        put("sort_order", nextItemOrder++)
                    }
                )
            }
        }
        return nextCategoryOrder
    }

    private fun findCategoryId(db: SQLiteDatabase, name: String): Long? {
        db.query("categories", arrayOf("id", "name"), null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) if (cursor.getString(1).equals(name, ignoreCase = true)) return cursor.getLong(0)
        }
        return null
    }

    private fun findItem(db: SQLiteDatabase, categoryId: Long, names: List<String>): Pair<Long, Long>? {
        db.query("items", arrayOf("id", "category_id", "name"), "category_id = ?", arrayOf(categoryId.toString()), null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                if (names.any { cursor.getString(2).equals(it, ignoreCase = true) }) return cursor.getLong(0) to cursor.getLong(1)
            }
        }
        return null
    }

    private fun findItemAnywhere(db: SQLiteDatabase, names: List<String>): Pair<Long, Long>? {
        db.query("items", arrayOf("id", "category_id", "name"), null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                if (names.any { cursor.getString(2).equals(it, ignoreCase = true) }) return cursor.getLong(0) to cursor.getLong(1)
            }
        }
        return null
    }

    private fun queryMaxOrder(db: SQLiteDatabase, table: String, categoryId: Long?): Int {
        val sql = if (categoryId == null) "SELECT COALESCE(MAX(sort_order), -1) FROM $table"
        else "SELECT COALESCE(MAX(sort_order), -1) FROM $table WHERE category_id = ?"
        val args = categoryId?.let { arrayOf(it.toString()) }
        db.rawQuery(sql, args).use { cursor -> return if (cursor.moveToFirst()) cursor.getInt(0) else -1 }
    }

    private val VERSION_6_CAMPING_EXTRAS = setOf(
        "Lanterna Voxo T9 recarregável",
        "Barraca Ontrek Iglu 4 pessoas"
    )
}
