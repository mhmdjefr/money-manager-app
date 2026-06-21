package com.mhmdjefr.moneymanager.data.local

// Representasi gabungan budget dan spending aktual untuk satu kategori,
// dihitung berdasarkan transaksi bulan aktif. Tidak disimpan ke database;
// dibentuk on-the-fly dari kombinasi BudgetEntity + TransactionEntity.
data class BudgetProgress(
    val categoryId: Int,
    val categoryName: String,
    val iconName: String,
    val limitAmount: Double,
    val spentAmount: Double
) {
    val percentage: Float
        get() = if (limitAmount > 0) (spentAmount / limitAmount).toFloat().coerceAtLeast(0f) else 0f

    val isOverBudget: Boolean
        get() = spentAmount > limitAmount
}
