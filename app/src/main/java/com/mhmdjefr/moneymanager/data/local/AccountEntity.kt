package com.mhmdjefr.moneymanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val initialBalance: Double = 0.0,
    val balance: Double = 0.0,
    val type: String = "REGULAR",
    val includeInTotal: Boolean = true,
    val orderIndex: Int = 0 // Kolom baru buat nyimpen urutan dompet
)