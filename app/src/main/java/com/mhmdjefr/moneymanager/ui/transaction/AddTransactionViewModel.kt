package com.mhmdjefr.moneymanager.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AddTransactionViewModel(private val repository: MoneyRepository) : ViewModel() {
    val accounts: Flow<List<AccountEntity>> = repository.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = repository.getAllCategories()

    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertTransaction(transaction) }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteTransaction(transaction) }
    }

    // Solusi Anti-Crash: Memaksa pencarian database berjalan di jalur belakang (IO)
    fun getTransactionById(id: Int): Flow<TransactionEntity?> = flow {
        emit(repository.getTransactionById(id))
    }.flowOn(Dispatchers.IO)
}

class AddTransactionViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) return AddTransactionViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}