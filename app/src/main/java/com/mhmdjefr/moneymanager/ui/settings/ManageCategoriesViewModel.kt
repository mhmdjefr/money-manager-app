package com.mhmdjefr.moneymanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mhmdjefr.moneymanager.data.local.CategoryEntity
import com.mhmdjefr.moneymanager.data.repository.MoneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ManageCategoriesViewModel(private val repository: MoneyRepository) : ViewModel() {
    val categories: Flow<List<CategoryEntity>> = repository.getAllCategories()

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Kalau ID 0 berarti Insert, kalau ada ID berarti Update
            if (category.id == 0) {
                repository.insertCategory(category)
            } else {
                repository.updateCategory(category)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }
}

class ManageCategoriesViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageCategoriesViewModel::class.java)) return ManageCategoriesViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}