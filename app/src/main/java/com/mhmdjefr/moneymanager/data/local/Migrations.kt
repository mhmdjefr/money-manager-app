package com.mhmdjefr.moneymanager.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v4 -> v5: menambahkan tabel budgets (fitur Budget per Kategori)
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `limitAmount` REAL NOT NULL
            )
            """.trimIndent()
        )
    }
}

// v5 -> v6: menambahkan kolom orderIndex ke categories (fitur drag-reorder kategori)
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `categories` ADD COLUMN `orderIndex` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
