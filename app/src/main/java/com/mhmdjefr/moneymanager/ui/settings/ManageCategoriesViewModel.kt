package com.mhmdjefr.moneymanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.BudgetEntity
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ManageCategoriesViewModel(private val repository: MoneyRepository) : ViewModel() {
    val categories: Flow<List<CategoryEntity>> = repository.getAllCategories()

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Kalau ID 0 berarti Insert, kalau ada ID berarti Update
            if (category.id == 0) {
                // Kategori baru ditaruh di akhir urutan tipe yang sama
                val existing = repository.getAllCategories().first().filter { it.type == category.type }
                val nextOrderIndex = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
                repository.insertCategory(category.copy(orderIndex = nextOrderIndex))
            } else {
                repository.updateCategory(category)
            }
        }
    }

    fun updateCategoriesOrder(categories: List<CategoryEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            categories.forEachIndexed { index, category ->
                repository.updateCategory(category.copy(orderIndex = index))
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    fun saveBudget(categoryId: Int, limitAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBudgetByCategoryId(categoryId)
            repository.insertBudget(BudgetEntity(categoryId = categoryId, limitAmount = limitAmount))
        }
    }

    fun deleteBudget(categoryId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBudgetByCategoryId(categoryId)
        }
    }
}

class ManageCategoriesViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageCategoriesViewModel::class.java)) return ManageCategoriesViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}