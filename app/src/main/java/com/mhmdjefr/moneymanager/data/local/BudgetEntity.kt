package com.mhmdjefr.moneymanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Budget bersifat recurring (berlaku tiap bulan secara otomatis), bukan per
// periode spesifik. Satu kategori hanya boleh punya satu budget aktif.
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val limitAmount: Double
)
