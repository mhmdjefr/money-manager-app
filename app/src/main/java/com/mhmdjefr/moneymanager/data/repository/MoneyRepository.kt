package com.mhmdjefr.moneymanager.data.repository

import com.mhmdjefr.moneymanager.data.local.AccountEntity
import com.mhmdjefr.moneymanager.data.local.BudgetEntity
import com.mhmdjefr.moneymanager.data.local.MoneyDao
import com.mhmdjefr.moneymanager.data.local.TransactionEntity
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MoneyRepository(private val dao: MoneyDao) {

    fun getTotalIncome() = dao.getTotalIncome()
    fun getTotalExpense() = dao.getTotalExpense()
    fun getTotalBalance() = dao.getTotalBalance()
    fun getAllTransactions() = dao.getAllTransactions()
    fun getTransactionsByDateRange(startDate: Long, endDate: Long) = dao.getTransactionsByDateRange(startDate, endDate)
    fun getTransactionById(id: Int): TransactionEntity? = dao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) { dao.insertTransaction(transaction) }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) { dao.deleteTransaction(transaction) }
    }

    fun getAllAccounts() = dao.getAllAccounts()

    suspend fun insertAccount(account: AccountEntity) {
        withContext(Dispatchers.IO) { dao.insertAccount(account) }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        withContext(Dispatchers.IO) { dao.deleteAccount(account) }
    }

    fun getAllCategories() = dao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) { dao.insertCategory(category) }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) { dao.deleteCategory(category) }
    }

    suspend fun updateCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) { dao.updateCategory(category) }
    }

    // --- BUDGETS ---
    fun getAllBudgets() = dao.getAllBudgets()

    suspend fun insertBudget(budget: BudgetEntity) {
        withContext(Dispatchers.IO) { dao.insertBudget(budget) }
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        withContext(Dispatchers.IO) { dao.deleteBudget(budget) }
    }

    suspend fun deleteBudgetByCategoryId(categoryId: Int) {
        withContext(Dispatchers.IO) { dao.deleteBudgetByCategoryId(categoryId) }
    }

    // Hapus seluruh transactions, accounts, categories, dan budgets.
    // Re-seed (akun default "Cash" + kategori default) ditangani di DashboardViewModel,
    // karena di sana juga perlu reset SharedPreferences (username/avatar).
    suspend fun resetAllData() {
        withContext(Dispatchers.IO) {
            dao.deleteAllTransactions()
            dao.deleteAllAccounts()
            dao.deleteAllCategories()
            dao.deleteAllBudgets()
        }
    }
}
