package com.mhmdjefr.moneymanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, CategoryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moneyDao(): MoneyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_manager_db"
                )
                    .addCallback(AppDatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Eksekusi Raw SQL sinkron murni. 100% aman dari coroutine deadlock.
            // 1 = True untuk nilai Boolean di SQLite

            // Seed Dompet
            db.execSQL("INSERT INTO accounts (name, initialBalance, balance, type, includeInTotal, orderIndex) VALUES ('Cash', 0.0, 0.0, 'REGULAR', 1, 0)")
            db.execSQL("INSERT INTO accounts (name, initialBalance, balance, type, includeInTotal, orderIndex) VALUES ('BCA', 0.0, 0.0, 'REGULAR', 1, 1)")
            db.execSQL("INSERT INTO accounts (name, initialBalance, balance, type, includeInTotal, orderIndex) VALUES ('GoPay', 0.0, 0.0, 'REGULAR', 1, 2)")

            // Seed Kategori - INCOME
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Salary', 'INCOME', 'Work')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Bonus', 'INCOME', 'CardGiftcard')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Investment', 'INCOME', 'TrendingUp')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Gift', 'INCOME', 'Celebration')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Other Income', 'INCOME', 'MoreHoriz')")

            // Seed Kategori - EXPENSE
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Food', 'EXPENSE', 'Fastfood')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Transport', 'EXPENSE', 'DirectionsCar')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Shopping', 'EXPENSE', 'ShoppingCart')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Bills', 'EXPENSE', 'Receipt')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Education', 'EXPENSE', 'School')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Health', 'EXPENSE', 'LocalHospital')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Entertainment', 'EXPENSE', 'Movie')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Travel', 'EXPENSE', 'Flight')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Housing/Rent', 'EXPENSE', 'Hotel')")
            db.execSQL("INSERT INTO categories (name, type, iconName) VALUES ('Pets', 'EXPENSE', 'Pets')")
        }
    }
}
