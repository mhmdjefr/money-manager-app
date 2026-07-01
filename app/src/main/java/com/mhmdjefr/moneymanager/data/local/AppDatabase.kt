package com.mhmdjefr.moneymanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 6,
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration() // jaring pengaman jika ada versi yang belum tercakup migration
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

            // Seed Kategori - INCOME
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Salary', 'INCOME', 'Work', 0)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Bonus', 'INCOME', 'CardGiftcard', 1)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Investment', 'INCOME', 'TrendingUp', 2)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Gift', 'INCOME', 'Celebration', 3)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Other Income', 'INCOME', 'MoreHoriz', 4)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Freelance', 'INCOME', 'Work', 5)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Rental Income', 'INCOME', 'Home', 6)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Refund', 'INCOME', 'Receipt', 7)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Interest', 'INCOME', 'TrendingUp', 8)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Allowance', 'INCOME', 'Favorite', 9)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Cashback', 'INCOME', 'Redeem', 10)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Dividend', 'INCOME', 'TrendingUp', 11)")

            // Seed Kategori - EXPENSE
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Food', 'EXPENSE', 'Fastfood', 0)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Transport', 'EXPENSE', 'DirectionsCar', 1)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Shopping', 'EXPENSE', 'ShoppingCart', 2)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Bills', 'EXPENSE', 'Receipt', 3)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Education', 'EXPENSE', 'School', 4)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Health', 'EXPENSE', 'LocalHospital', 5)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Entertainment', 'EXPENSE', 'Movie', 6)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Travel', 'EXPENSE', 'Flight', 7)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Housing/Rent', 'EXPENSE', 'Hotel', 8)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Pets', 'EXPENSE', 'Pets', 9)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Groceries', 'EXPENSE', 'LocalGroceryStore', 10)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Subscriptions', 'EXPENSE', 'Subscriptions', 11)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Personal Care', 'EXPENSE', 'Spa', 12)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Insurance', 'EXPENSE', 'Shield', 13)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Donation', 'EXPENSE', 'VolunteerActivism', 14)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Internet/WiFi', 'EXPENSE', 'Wifi', 15)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Fuel/Gas', 'EXPENSE', 'LocalGasStation', 16)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Repair & Maintenance', 'EXPENSE', 'Build', 17)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Taxes', 'EXPENSE', 'AccountBalance', 18)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Childcare', 'EXPENSE', 'ChildCare', 19)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Coffee/Snacks', 'EXPENSE', 'LocalCafe', 20)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Laundry', 'EXPENSE', 'LocalLaundryService', 21)")
            db.execSQL("INSERT INTO categories (name, type, iconName, orderIndex) VALUES ('Parking & Toll', 'EXPENSE', 'LocalParking', 22)")
        }
    }
}
