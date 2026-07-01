package com.mhmdjefr.moneymanager.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.BudgetEntity
import com.mhmdjefr.moneymanager.data.local.BudgetProgress
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.Calendar

class DashboardViewModel(private val repository: MoneyRepository) : ViewModel() {

    // Semua Flow "sumber" di-stateIn agar di-SHARE antar seluruh Composable yang
    // collect (Dashboard, Wallet, Stats, dst), bukan dihitung ulang dari nol untuk
    // setiap subscriber baru. WhileSubscribed(5000) menjaga Flow tetap hidup 5 detik
    // setelah subscriber terakhir hilang -- supaya pindah-pindah screen cepat tidak
    // memicu re-query database berulang kali.
    private val sharingConfig = SharingStarted.WhileSubscribed(5000)

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    private val _isBalanceVisible = MutableStateFlow(true)
    val isBalanceVisible: StateFlow<Boolean> = _isBalanceVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedWalletId = MutableStateFlow<Int?>(null)
    val selectedWalletId: StateFlow<Int?> = _selectedWalletId.asStateFlow()
    fun setSelectedWalletId(id: Int?) { _selectedWalletId.value = id }

    // Satu sumber data akun (sebelumnya ada accountList + accounts yang duplikat
    // dan masing-masing query database sendiri-sendiri).
    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, sharingConfig, emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, sharingConfig, emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, sharingConfig, emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.getAllBudgets()
        .stateIn(viewModelScope, sharingConfig, emptyList())

    val totalBalance: StateFlow<Double> = combine(
        accounts,
        allTransactions
    ) { accountsList, transactions ->
        var netWorth = 0.0
        val incomeByAccount = transactions.filter { it.type == "INCOME" }.groupBy { it.accountId }
        val expenseByAccount = transactions.filter { it.type == "EXPENSE" }.groupBy { it.accountId }
        val transferOutByAccount = transactions.filter { it.type == "TRANSFER" }.groupBy { it.accountId }
        val transferInByAccount = transactions.filter { it.type == "TRANSFER" }.groupBy { it.targetAccountId }

        accountsList.forEach { account ->
            if (account.includeInTotal) {
                val income = incomeByAccount[account.id]?.sumOf { it.amount } ?: 0.0
                val expense = expenseByAccount[account.id]?.sumOf { it.amount } ?: 0.0
                val transferOut = transferOutByAccount[account.id]?.sumOf { it.amount } ?: 0.0
                val transferIn = transferInByAccount[account.id]?.sumOf { it.amount } ?: 0.0
                netWorth += account.initialBalance + income - expense - transferOut + transferIn
            }
        }
        netWorth
    }.stateIn(viewModelScope, sharingConfig, 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTransactions: StateFlow<List<TransactionEntity>> = _currentMonth.flatMapLatest { calendar ->
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
    }.stateIn(viewModelScope, sharingConfig, emptyList())

    // Gabungan budget + spending aktual bulan berjalan, dihitung berdasarkan
    // note transaksi yang diawali "[NamaKategori]" (pola yang sama dipakai di Dashboard/Stats).
    val budgetProgressList: StateFlow<List<BudgetProgress>> = combine(
        allBudgets,
        allCategories,
        monthlyTransactions
    ) { budgets, categories, transactions ->
        if (budgets.isEmpty()) return@combine emptyList()

        val expenseByCategory = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { tx -> tx.note?.substringBefore("]")?.replace("[", "")?.trim() }

        budgets.mapNotNull { budget ->
            val category = categories.find { it.id == budget.categoryId } ?: return@mapNotNull null
            val spent = expenseByCategory[category.name]?.sumOf { it.amount } ?: 0.0

            BudgetProgress(
                categoryId = category.id,
                categoryName = category.name,
                iconName = category.iconName,
                limitAmount = budget.limitAmount,
                spentAmount = spent
            )
        }
    }.stateIn(viewModelScope, sharingConfig, emptyList())

    // Transaksi bulan SEBELUMNYA dari currentMonth, dipakai untuk perbandingan tren di Stats.
    @OptIn(ExperimentalCoroutinesApi::class)
    val previousMonthTransactions: StateFlow<List<TransactionEntity>> = _currentMonth.flatMapLatest { calendar ->
        val prevCal = calendar.clone() as Calendar
        prevCal.add(Calendar.MONTH, -1)

        val start = prevCal.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)

        val end = prevCal.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)

        repository.getTransactionsByDateRange(start.timeInMillis, end.timeInMillis)
    }.stateIn(viewModelScope, sharingConfig, emptyList())

    // Search lintas SEMUA transaksi (tidak terbatas bulan aktif), berbeda dari
    // monthlyTransactions yang dipakai untuk ringkasan income/expense bulan berjalan.
    val searchResults: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _searchQuery,
        _selectedWalletId
    ) { transactions, query, walletId ->
        if (query.isBlank()) {
            emptyList()
        } else {
            transactions.filter { tx ->
                val categoryName = tx.note?.substringBefore("]")?.replace("[", "") ?: ""
                val realNote = tx.note?.substringAfter("]") ?: ""
                val matchesQuery = categoryName.contains(query, ignoreCase = true) || realNote.contains(query, ignoreCase = true)
                val matchesWallet = walletId == null || tx.accountId == walletId || tx.targetAccountId == walletId
                matchesQuery && matchesWallet
            }.sortedByDescending { it.date }
        }
    }.stateIn(viewModelScope, sharingConfig, emptyList())

    // --- EXPORT CSV (Profile + Accounts + Categories + Transactions) ---
    fun exportToCsv(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val accountsList = repository.getAllAccounts().first()
                val categoriesList = repository.getAllCategories().first()
                val txList = repository.getAllTransactions().first()

                val prefs = context.getSharedPreferences("money_prefs", Context.MODE_PRIVATE)
                val userName = prefs.getString("user_name", "User") ?: "User"
                val userAvatar = prefs.getString("user_avatar", "Person") ?: "Person"

                val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                outputStream?.writer()?.use { writer ->

                    writer.write("# PROFILE\n")
                    writer.write("Name,Avatar\n")
                    writer.write("\"${userName}\",${userAvatar}\n")
                    writer.write("\n")

                    writer.write("# ACCOUNTS\n")
                    writer.write("ID,Name,InitialBalance,Type,IncludeInTotal,OrderIndex\n")
                    accountsList.forEach { acc ->
                        writer.write("${acc.id},\"${acc.name}\",${acc.initialBalance},${acc.type},${acc.includeInTotal},${acc.orderIndex}\n")
                    }
                    writer.write("\n")

                    writer.write("# CATEGORIES\n")
                    writer.write("ID,Name,Type,IconName\n")
                    categoriesList.forEach { cat ->
                        writer.write("${cat.id},\"${cat.name}\",${cat.type},${cat.iconName}\n")
                    }
                    writer.write("\n")

                    writer.write("# TRANSACTIONS\n")
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

    // --- IMPORT CSV (Profile + Accounts + Categories + Transactions, anti-duplicate) ---
    fun importFromCsv(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()

                val sections = mutableMapOf<String, MutableList<String>>()
                var currentSection: String? = null

                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("# ")) {
                        currentSection = trimmed.removePrefix("# ").trim()
                        sections[currentSection] = mutableListOf()
                    } else if (trimmed.isNotBlank() && currentSection != null) {
                        sections[currentSection]?.add(line)
                    }
                }

                val isLegacyFormat = sections.isEmpty()

                sections["PROFILE"]?.let { rows ->
                    if (rows.size >= 2) {
                        val data = parseCsvLine(rows[1])
                        if (data.size >= 2) {
                            val prefs = context.getSharedPreferences("money_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("user_name", data[0])
                                .putString("user_avatar", data[1])
                                .apply()
                        }
                    }
                }

                sections["ACCOUNTS"]?.let { rows ->
                    val existingAccounts = repository.getAllAccounts().first()

                    for (i in 1 until rows.size) {
                        val row = parseCsvLine(rows[i])
                        if (row.size >= 6) {
                            val name = row[1]
                            val initialBalance = row[2].toDoubleOrNull() ?: 0.0
                            val type = row[3]
                            val includeInTotal = row[4].toBooleanStrictOrNull() ?: true
                            val orderIndex = row[5].toIntOrNull() ?: 0

                            val existing = existingAccounts.find { it.name == name && it.type == type }
                            if (existing != null) {
                                repository.insertAccount(
                                    existing.copy(
                                        initialBalance = initialBalance,
                                        includeInTotal = includeInTotal,
                                        orderIndex = orderIndex
                                    )
                                )
                            } else {
                                repository.insertAccount(
                                    AccountEntity(
                                        id = 0,
                                        name = name,
                                        initialBalance = initialBalance,
                                        type = type,
                                        includeInTotal = includeInTotal,
                                        orderIndex = orderIndex
                                    )
                                )
                            }
                        }
                    }
                }

                sections["CATEGORIES"]?.let { rows ->
                    val existingCategories = repository.getAllCategories().first()

                    for (i in 1 until rows.size) {
                        val row = parseCsvLine(rows[i])
                        if (row.size >= 4) {
                            val name = row[1]
                            val type = row[2]
                            val iconName = row[3]

                            val existing = existingCategories.find { it.name == name && it.type == type }
                            if (existing != null) {
                                repository.updateCategory(existing.copy(iconName = iconName))
                            } else {
                                repository.insertCategory(
                                    CategoryEntity(name = name, type = type, iconName = iconName)
                                )
                            }
                        }
                    }
                }

                val transactionRows = sections["TRANSACTIONS"] ?: if (isLegacyFormat) lines else null
                transactionRows?.let { rows ->
                    val existingTxs = repository.getAllTransactions().first()
                    val existingSignatures = existingTxs.map {
                        "${it.accountId}_${it.amount}_${it.type}_${it.date}_${it.note ?: ""}"
                    }.toSet()

                    for (i in 1 until rows.size) {
                        val row = parseCsvLine(rows[i])
                        if (row.size >= 5) {
                            val accountId = row[1].toIntOrNull() ?: continue
                            val amount = row[2].toDoubleOrNull() ?: 0.0
                            val type = row[3]
                            val date = row[4].toLongOrNull() ?: Calendar.getInstance().timeInMillis
                            val note = row.getOrNull(5) ?: ""
                            val targetAccountId = row.getOrNull(6)?.toIntOrNull()

                            val signature = "${accountId}_${amount}_${type}_${date}_${note}"

                            if (!existingSignatures.contains(signature)) {
                                repository.insertTransaction(
                                    TransactionEntity(
                                        id = 0,
                                        accountId = accountId,
                                        amount = amount,
                                        type = type,
                                        date = date,
                                        note = note,
                                        targetAccountId = targetAccountId
                                    )
                                )
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

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> insideQuotes = !insideQuotes
                c == ',' && !insideQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
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

    fun saveBudget(categoryId: Int, limitAmount: Double) {
        viewModelScope.launch {
            repository.deleteBudgetByCategoryId(categoryId)
            repository.insertBudget(BudgetEntity(categoryId = categoryId, limitAmount = limitAmount))
        }
    }

    fun deleteBudget(categoryId: Int) {
        viewModelScope.launch { repository.deleteBudgetByCategoryId(categoryId) }
    }

    fun resetApplicationData(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.resetAllData()

            repository.insertAccount(
                AccountEntity(name = "Cash", initialBalance = 0.0, type = "REGULAR", includeInTotal = true, orderIndex = 0)
            )

            repository.insertCategory(CategoryEntity(name = "Salary", type = "INCOME", iconName = "Work", orderIndex = 0))
            repository.insertCategory(CategoryEntity(name = "Bonus", type = "INCOME", iconName = "CardGiftcard", orderIndex = 1))
            repository.insertCategory(CategoryEntity(name = "Investment", type = "INCOME", iconName = "TrendingUp", orderIndex = 2))
            repository.insertCategory(CategoryEntity(name = "Gift", type = "INCOME", iconName = "Celebration", orderIndex = 3))
            repository.insertCategory(CategoryEntity(name = "Other Income", type = "INCOME", iconName = "MoreHoriz", orderIndex = 4))
            repository.insertCategory(CategoryEntity(name = "Freelance", type = "INCOME", iconName = "Work", orderIndex = 5))
            repository.insertCategory(CategoryEntity(name = "Rental Income", type = "INCOME", iconName = "Home", orderIndex = 6))
            repository.insertCategory(CategoryEntity(name = "Refund", type = "INCOME", iconName = "Receipt", orderIndex = 7))
            repository.insertCategory(CategoryEntity(name = "Interest", type = "INCOME", iconName = "TrendingUp", orderIndex = 8))
            repository.insertCategory(CategoryEntity(name = "Allowance", type = "INCOME", iconName = "Favorite", orderIndex = 9))
            repository.insertCategory(CategoryEntity(name = "Cashback", type = "INCOME", iconName = "Redeem", orderIndex = 10))
            repository.insertCategory(CategoryEntity(name = "Dividend", type = "INCOME", iconName = "TrendingUp", orderIndex = 11))

            repository.insertCategory(CategoryEntity(name = "Food", type = "EXPENSE", iconName = "Fastfood", orderIndex = 0))
            repository.insertCategory(CategoryEntity(name = "Transport", type = "EXPENSE", iconName = "DirectionsCar", orderIndex = 1))
            repository.insertCategory(CategoryEntity(name = "Shopping", type = "EXPENSE", iconName = "ShoppingCart", orderIndex = 2))
            repository.insertCategory(CategoryEntity(name = "Bills", type = "EXPENSE", iconName = "Receipt", orderIndex = 3))
            repository.insertCategory(CategoryEntity(name = "Education", type = "EXPENSE", iconName = "School", orderIndex = 4))
            repository.insertCategory(CategoryEntity(name = "Health", type = "EXPENSE", iconName = "LocalHospital", orderIndex = 5))
            repository.insertCategory(CategoryEntity(name = "Entertainment", type = "EXPENSE", iconName = "Movie", orderIndex = 6))
            repository.insertCategory(CategoryEntity(name = "Travel", type = "EXPENSE", iconName = "Flight", orderIndex = 7))
            repository.insertCategory(CategoryEntity(name = "Housing/Rent", type = "EXPENSE", iconName = "Hotel", orderIndex = 8))
            repository.insertCategory(CategoryEntity(name = "Pets", type = "EXPENSE", iconName = "Pets", orderIndex = 9))
            repository.insertCategory(CategoryEntity(name = "Groceries", type = "EXPENSE", iconName = "LocalGroceryStore", orderIndex = 10))
            repository.insertCategory(CategoryEntity(name = "Subscriptions", type = "EXPENSE", iconName = "Subscriptions", orderIndex = 11))
            repository.insertCategory(CategoryEntity(name = "Personal Care", type = "EXPENSE", iconName = "Spa", orderIndex = 12))
            repository.insertCategory(CategoryEntity(name = "Insurance", type = "EXPENSE", iconName = "Shield", orderIndex = 13))
            repository.insertCategory(CategoryEntity(name = "Donation", type = "EXPENSE", iconName = "VolunteerActivism", orderIndex = 14))
            repository.insertCategory(CategoryEntity(name = "Internet/WiFi", type = "EXPENSE", iconName = "Wifi", orderIndex = 15))
            repository.insertCategory(CategoryEntity(name = "Fuel/Gas", type = "EXPENSE", iconName = "LocalGasStation", orderIndex = 16))
            repository.insertCategory(CategoryEntity(name = "Repair & Maintenance", type = "EXPENSE", iconName = "Build", orderIndex = 17))
            repository.insertCategory(CategoryEntity(name = "Taxes", type = "EXPENSE", iconName = "AccountBalance", orderIndex = 18))
            repository.insertCategory(CategoryEntity(name = "Childcare", type = "EXPENSE", iconName = "ChildCare", orderIndex = 19))
            repository.insertCategory(CategoryEntity(name = "Coffee/Snacks", type = "EXPENSE", iconName = "LocalCafe", orderIndex = 20))
            repository.insertCategory(CategoryEntity(name = "Laundry", type = "EXPENSE", iconName = "LocalLaundryService", orderIndex = 21))
            repository.insertCategory(CategoryEntity(name = "Parking & Toll", type = "EXPENSE", iconName = "LocalParking", orderIndex = 22))

            val prefs = context.getSharedPreferences("money_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_name", "User")
                .putString("user_avatar", "Person")
                .apply()

            withContext(Dispatchers.Main) { onComplete() }
        }
    }
}

class DashboardViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) return DashboardViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
