package com.mhmdjefr.moneymanager.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.Calendar

class DashboardViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    private val _isBalanceVisible = MutableStateFlow(true)
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // DEKLARASI WAJIB DI ATAS (Biar bisa dibaca sama totalBalance di bawahnya)
    val accountList: Flow<List<AccountEntity>> = repository.getAllAccounts()

    val accounts: Flow<List<AccountEntity>> = repository.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = repository.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = repository.getAllCategories()

    // Kalkulasi Total Balance (Net Worth) yang 100% sinkron dengan layar Wallet
    val totalBalance: Flow<Double> = combine(
        accountList,
        allTransactions
    ) { accounts, transactions ->
        var netWorth = 0.0
        accounts.forEach { account ->
            if (account.includeInTotal) {
                val income = transactions.filter { it.accountId == account.id && it.type == "INCOME" }.sumOf { it.amount }
                val expense = transactions.filter { it.accountId == account.id && it.type == "EXPENSE" }.sumOf { it.amount }
                val transferOut = transactions.filter { it.accountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }
                val transferIn = transactions.filter { it.targetAccountId == account.id && it.type == "TRANSFER" }.sumOf { it.amount }

                val currentBalance = account.initialBalance + income - expense - transferOut + transferIn
                netWorth += currentBalance
            }
        }
        netWorth
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTransactions: Flow<List<TransactionEntity>> = _currentMonth.flatMapLatest { calendar ->
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)

        repository.getTransactionsByDateRange(start.timeInMillis, end.timeInMillis)
    }

    val filteredMonthlyTransactions: Flow<List<TransactionEntity>> = combine(
        monthlyTransactions,
        _searchQuery
    ) { transactions, query ->
        if (query.isBlank()) {
            transactions
        } else {
            transactions.filter { tx ->
                val categoryName = tx.note?.substringBefore("]")?.replace("[", "") ?: ""
                val realNote = tx.note?.substringAfter("]") ?: ""
                categoryName.contains(query, ignoreCase = true) || realNote.contains(query, ignoreCase = true)
            }
        }
    }

    // --- LOGIKA EXPORT CSV ---
    fun exportToCsv(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val txList = repository.getAllTransactions().first()
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                outputStream?.writer()?.use { writer ->
                    writer.write("ID,Account_ID,Amount,Type,Date,Note,Target_Account_ID\n")
                    txList.forEach { tx ->
                        writer.write("${tx.id},${tx.accountId},${tx.amount},${tx.type},${tx.date},\"${tx.note ?: ""}\",${tx.targetAccountId ?: ""}\n")
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    // --- LOGIKA IMPORT CSV (ANTI-DUPLIKAT) ---
    fun importFromCsv(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val existingTxs = repository.getAllTransactions().first()
                val existingSignatures = existingTxs.map {
                    "${it.accountId}_${it.amount}_${it.type}_${it.date}_${it.note ?: ""}"
                }.toSet()

                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()

                if (lines.isNotEmpty()) {
                    for (i in 1 until lines.size) {
                        val row = lines[i].split(",")
                        if (row.size >= 5) {
                            val accountId = row[1].toIntOrNull() ?: continue
                            val amount = row[2].toDoubleOrNull() ?: 0.0
                            val type = row[3]
                            val date = row[4].toLongOrNull() ?: Calendar.getInstance().timeInMillis
                            val note = row.getOrNull(5)?.replace("\"", "") ?: ""
                            val targetAccountId = row.getOrNull(6)?.toIntOrNull()

                            val signature = "${accountId}_${amount}_${type}_${date}_${note}"

                            if (!existingSignatures.contains(signature)) {
                                val importedTx = TransactionEntity(
                                    id = 0,
                                    accountId = accountId,
                                    amount = amount,
                                    type = type,
                                    date = date,
                                    note = note,
                                    targetAccountId = targetAccountId
                                )
                                repository.insertTransaction(importedTx)
                            }
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun saveAccount(id: Int, name: String, initialBalance: Double, type: String, includeInTotal: Boolean, orderIndex: Int) {
        viewModelScope.launch {
            repository.insertAccount(AccountEntity(id = id, name = name, initialBalance = initialBalance, type = type, includeInTotal = includeInTotal, orderIndex = orderIndex))
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch { repository.deleteAccount(account) }
    }

    fun updateAccountsOrder(accounts: List<AccountEntity>) {
        viewModelScope.launch {
            accounts.forEachIndexed { index, account ->
                repository.insertAccount(account.copy(orderIndex = index))
            }
        }
    }

    fun nextMonth() {
        val next = _currentMonth.value.clone() as Calendar
        next.add(Calendar.MONTH, 1)
        _currentMonth.value = next
    }

    fun previousMonth() {
        val prev = _currentMonth.value.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        _currentMonth.value = prev
    }

    fun toggleBalanceVisibility() { _isBalanceVisible.value = !_isBalanceVisible.value }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun saveCategory(name: String, type: String, iconName: String) { viewModelScope.launch { repository.insertCategory(CategoryEntity(name = name, type = type, iconName = iconName)) } }
    fun deleteCategory(category: CategoryEntity) { viewModelScope.launch { repository.deleteCategory(category) } }

    fun resetApplicationData() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }
}

class DashboardViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) return DashboardViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}