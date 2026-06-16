package com.mhmdjefr.moneymanager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth

    private val _isBalanceVisible = MutableStateFlow(true)
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible

    fun toggleBalanceVisibility() {
        _isBalanceVisible.value = !_isBalanceVisible.value
    }

    val accountList: Flow<List<AccountEntity>> = repository.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = repository.getAllTransactions()

    val totalBalance: Flow<Double> = combine(accountList, allTransactions) { accounts, transactions ->
        accounts.filter { it.includeInTotal }.sumOf { account ->
            val income = transactions.filter { it.accountId == account.id && it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.accountId == account.id && it.type == "EXPENSE" }.sumOf { it.amount }
            val transferOut = transactions.filter { it.accountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }
            val transferIn = transactions.filter { it.targetAccountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }
            account.initialBalance + income - expense - transferOut + transferIn
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTransactions: Flow<List<TransactionEntity>> = _currentMonth.flatMapLatest { calendar ->
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        val end = calendar.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)
        repository.getTransactionsByDateRange(start.timeInMillis, end.timeInMillis)
    }

    fun previousMonth() {
        val newCal = _currentMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, -1)
        _currentMonth.value = newCal
    }

    fun nextMonth() {
        val newCal = _currentMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, 1)
        _currentMonth.value = newCal
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    fun saveAccount(id: Int, name: String, initialBalance: Double, type: String, includeInTotal: Boolean, orderIndex: Int) {
        viewModelScope.launch {
            repository.insertAccount(AccountEntity(id, name, initialBalance, 0.0, type, includeInTotal, orderIndex))
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    fun updateAccountsOrder(updatedList: List<AccountEntity>) {
        viewModelScope.launch {
            updatedList.forEachIndexed { index, account ->
                if (account.orderIndex != index) {
                    repository.insertAccount(account.copy(orderIndex = index))
                }
            }
        }
    }
}

class DashboardViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) return DashboardViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}