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
                collapsed INTEGER NOT NULL DEFAULT 0
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
                FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_items_category ON items(category_id)")
        DefaultCatalog.seedMissing(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE categories ADD COLUMN budget_cents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE categories ADD COLUMN collapsed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE items SET sort_order = id")
        }
        if (oldVersion < 3) DefaultCatalog.seedMissing(db)
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE items ADD COLUMN owned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN priority TEXT NOT NULL DEFAULT 'important'")
            db.execSQL("ALTER TABLE items ADD COLUMN actual_unit_price_cents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE items ADD COLUMN store TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE items ADD COLUMN product_url TEXT NOT NULL DEFAULT ''")
        }
    }

    companion object {
        private const val DATABASE_NAME = "lista_nomade.db"
        private const val DATABASE_VERSION = 4

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: AppDatabase(context).also { instance = it }
        }
    }
}
