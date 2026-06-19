package com.downloadmanager.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.FileCategory
import com.downloadmanager.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(FileCategory.VIDEO)
    val selectedCategory: StateFlow<FileCategory> = _selectedCategory.asStateFlow()

    val categoryCounts = categoryRepository.getCategoryCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val tasksByCategory: StateFlow<List<DownloadTask>> = _selectedCategory.let { flow ->
        // Simplified: return all tasks for now
        categoryRepository.getTasksByCategory(_selectedCategory.value)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun selectCategory(category: FileCategory) {
        _selectedCategory.value = category
    }
}