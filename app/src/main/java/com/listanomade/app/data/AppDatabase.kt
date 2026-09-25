package com.listanomade.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                sort_order INTEGER NOT NULL DEFAULT 0,
                budget_cents INTEGER NOT NULL DEFAULT 0,
                collapsed INTEGER NOT NULL DEFAULT 0,
                archived INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                unit_price_cents INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                total_cents INTEGER NOT NULL,
                purchased INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                owned INTEGER NOT NULL DEFAULT 0,
                priority TEXT NOT NULL DEFAULT 'important',
                actual_unit_price_cents INTEGER NOT NULL DEFAULT 0,
                store TEXT NOT NULL DEFAULT '',
                product_url TEXT NOT NULL DEFAULT '',
                purchased_quantity INTEGER NOT NULL DEFAULT 0,
                target_date INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        createAdvancedTables(db)
        db.execSQL("CREATE INDEX idx_items_category ON items(category_id)")
        DefaultCatalog.seedMissing(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val needsLegacyCatalogSeed = oldVersion < 3
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE categories ADD COLUMN budget_cents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE categories ADD COLUMN collapsed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE items SET sort_order = id")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE items ADD COLUMN owned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN priority TEXT NOT NULL DEFAULT 'important'")
            db.execSQL("ALTER TABLE items ADD COLUMN actual_unit_price_cents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN store TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE items ADD COLUMN product_url TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE categories ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN purchased_quantity INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN target_date INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE items SET purchased_quantity = quantity WHERE purchased = 1")
            createAdvancedTables(db)
            if (needsLegacyCatalogSeed) DefaultCatalog.seedMissing(db) else DefaultCatalog.seedVersion6Migration(db)
        }
    }

    private fun createAdvancedTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS price_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL,
                expected_unit_price_cents INTEGER NOT NULL,
                actual_unit_price_cents INTEGER NOT NULL DEFAULT 0,
                changed_at INTEGER NOT NULL,
                FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS template_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                template_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                unit_price_cents INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                priority TEXT NOT NULL DEFAULT 'important',
                store TEXT NOT NULL DEFAULT '',
                product_url TEXT NOT NULL DEFAULT '',
                sort_order INTEGER NOT NULL DEFAULT 0,
                target_date INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(template_id) REFERENCES templates(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_price_history_item ON price_history(item_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_template_items_template ON template_items(template_id)")
    }

    companion object {
        private const val DATABASE_NAME = "lista_nomade.db"
        private const val DATABASE_VERSION = 5

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: AppDatabase(context).also { instance = it }
        }
    }
}
