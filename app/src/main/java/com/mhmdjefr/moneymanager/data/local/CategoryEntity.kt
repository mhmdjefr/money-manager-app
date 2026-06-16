package com.mhmdjefr.moneymanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // INCOME atau EXPENSE
    val iconName: String // Kita simpen nama string ikonnya, misal "Fastfood", "Work"
)