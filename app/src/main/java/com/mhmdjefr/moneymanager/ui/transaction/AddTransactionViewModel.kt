package com.mhmdjefr.moneymanager.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTransactionViewModel(private val repository: MoneyRepository) : ViewModel() {

    // Aliran data dompet *realtime* untuk dibaca oleh dropdown di UI
    val accountList: Flow<List<AccountEntity>> = repository.getAllAccounts()

    suspend fun getTransaction(id: Int): TransactionEntity? {
        return withContext(Dispatchers.IO) {
            repository.getTransactionById(id)
        }
    }

    // Sekarang fungsi ini nerima parameter ID dompet asal dan dompet tujuan asli dari UI
    fun saveTransaction(
        id: Int?,
        type: String,
        nominal: Double,
        categoryValue: String,
        note: String,
        accountId: Int,
        targetAccountId: Int? = null
    ) {
        viewModelScope.launch {
            val isTransfer = type.equals("Transfer", ignoreCase = true)

            val existingTx = id?.let { withContext(Dispatchers.IO) { repository.getTransactionById(it) } }
            val txDate = existingTx?.date ?: System.currentTimeMillis()

            val transaction = TransactionEntity(
                id = id ?: 0,
                amount = nominal,
                type = type.uppercase(),
                date = txDate,
                note = if (isTransfer) note else "[$categoryValue] $note",
                accountId = accountId, // Menyimpan ID dompet asli yang lo pilih
                categoryId = if (!isTransfer) 1 else null,
                targetAccountId = if (isTransfer) targetAccountId else null // Menyimpan ID dompet tujuan kalau Transfer
            )
            repository.insertTransaction(transaction)
        }
    }
}

class AddTransactionViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddTransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}