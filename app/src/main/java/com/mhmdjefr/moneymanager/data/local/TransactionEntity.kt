package com.mhmdjefr.moneymanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "EXPENSE", "INCOME", atau "TRANSFER"
    val date: Long, // Nyimpen tanggal
    val note: String? = null,
    val accountId: Int, // Akun sumber dana
    val categoryId: Int? = null, // Kategori (kosong kalau TRANSFER)
    val targetAccountId: Int? = null // Akun tujuan (cuma dipake kalau TRANSFER)
)