package com.downloadmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloadmanager.app.category.CategoryInfo
import com.downloadmanager.app.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<CategoryInfo> = emptyList(),
    val expandedCategoryId: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddCategoryDialog: Boolean = false,
    val showAddSubCategoryDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showDeleteSubCategoryConfirmDialog: Boolean = false,
    val currentCategoryId: String? = null,
    val currentSubCategoryId: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _expandedCategoryId = MutableStateFlow<String?>(null)
    private val _showAddCategoryDialog = MutableStateFlow(false)
    private val _showAddSubCategoryDialog = MutableStateFlow(false)
    private val _showRenameDialog = MutableStateFlow(false)
    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    private val _showDeleteSubCategoryConfirmDialog = MutableStateFlow(false)
    private val _currentCategoryId = MutableStateFlow<String?>(null)
    private val _currentSubCategoryId = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryRepository.getAllCategories(),
        _expandedCategoryId,
        _showAddCategoryDialog,
        _showAddSubCategoryDialog,
        _showRenameDialog,
        _showDeleteConfirmDialog,
        _showDeleteSubCategoryConfirmDialog,
        _currentCategoryId,
        _currentSubCategoryId,
        _errorMessage
    ) { categories, expandedCategoryId, showAddCategoryDialog, showAddSubCategoryDialog, showRenameDialog, showDeleteConfirmDialog, showDeleteSubCategoryConfirmDialog, currentCategoryId, currentSubCategoryId, errorMessage ->
        CategoryUiState(
            categories = categories,
            expandedCategoryId = expandedCategoryId,
            isLoading = false,
            errorMessage = errorMessage,
            showAddCategoryDialog = showAddCategoryDialog,
            showAddSubCategoryDialog = showAddSubCategoryDialog,
            showRenameDialog = showRenameDialog,
            showDeleteConfirmDialog = showDeleteConfirmDialog,
            showDeleteSubCategoryConfirmDialog = showDeleteSubCategoryConfirmDialog,
            currentCategoryId = currentCategoryId,
            currentSubCategoryId = currentSubCategoryId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryUiState()
    )

    fun toggleCategoryExpand(categoryId: String) {
        _expandedCategoryId.value = if (_expandedCategoryId.value == categoryId) {
            null
        } else {
            categoryId
        }
    }

    fun showAddCategoryDialog(show: Boolean) {
        _showAddCategoryDialog.value = show
    }

    fun showAddSubCategoryDialog(show: Boolean, categoryId: String) {
        _showAddSubCategoryDialog.value = show
        _currentCategoryId.value = if (show) categoryId else null
    }

    fun showRenameDialog(show: Boolean, categoryId: String) {
        _showRenameDialog.value = show
        _currentCategoryId.value = if (show) categoryId else null
    }

    fun showDeleteConfirmDialog(show: Boolean, categoryId: String) {
        _showDeleteConfirmDialog.value = show
        _currentCategoryId.value = if (show) categoryId else null
    }

    fun showDeleteSubCategoryConfirmDialog(show: Boolean, subCategoryId: String) {
        _showDeleteSubCategoryConfirmDialog.value = show
        _currentSubCategoryId.value = if (show) subCategoryId else null
    }

    fun addCategory(name: String, iconRes: String?, extensions: List<String>) {
        viewModelScope.launch {
            try {
                categoryRepository.addCategory(name, iconRes, extensions)
                _errorMessage.value = null
                _showAddCategoryDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun removeCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                categoryRepository.removeCategory(categoryId)
                _errorMessage.value = null
                _showDeleteConfirmDialog.value = false
                _currentCategoryId.value = null
                if (_expandedCategoryId.value == categoryId) {
                    _expandedCategoryId.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun addSubCategory(categoryId: String, extension: String) {
        viewModelScope.launch {
            try {
                categoryRepository.addSubCategory(categoryId, extension)
                _errorMessage.value = null
                _showAddSubCategoryDialog.value = false
                _currentCategoryId.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun addSubCategories(categoryId: String, extensions: List<String>) {
        viewModelScope.launch {
            try {
                extensions.forEach { ext ->
                    categoryRepository.addSubCategory(categoryId, ext)
                }
                _errorMessage.value = null
                _showAddSubCategoryDialog.value = false
                _currentCategoryId.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun removeSubCategory(subCategoryId: String) {
        viewModelScope.launch {
            try {
                categoryRepository.removeSubCategory(subCategoryId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            try {
                categoryRepository.renameCategory(categoryId, newName)
                _errorMessage.value = null
                _showRenameDialog.value = false
                _currentCategoryId.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
