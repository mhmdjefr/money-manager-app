package com.mhmdjefr.moneymanager.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class AddTransactionViewModel(private val repository: MoneyRepository) : ViewModel() {
    val accounts: Flow<List<AccountEntity>> = repository.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = repository.getAllCategories()

    fun saveTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.insertTransaction(transaction) }
    }

    // Dibungkus dengan flow { emit(...) } agar selaras dengan .collectAsState() di UI
    fun getTransactionById(id: Int): Flow<TransactionEntity?> = flow {
        emit(repository.getTransactionById(id))
    }
}

class AddTransactionViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) return AddTransactionViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}