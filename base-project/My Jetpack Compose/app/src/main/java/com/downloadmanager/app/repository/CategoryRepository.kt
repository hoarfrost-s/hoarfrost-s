package com.downloadmanager.app.repository

import com.downloadmanager.app.category.CategoryInfo
import com.downloadmanager.app.category.CategoryMatcher
import com.downloadmanager.app.category.SubCategoryInfo
import com.downloadmanager.app.data.entity.CategoryMatchResult
import com.downloadmanager.app.data.local.CategoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryMatcher: CategoryMatcher,
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<CategoryInfo>> {
        return categoryMatcher.getAllCategories()
    }

    fun getSubCategories(categoryId: String): Flow<List<SubCategoryInfo>> {
        return categoryMatcher.getSubCategories(categoryId)
    }

    suspend fun addCategory(name: String, iconRes: String?, extensions: List<String>) {
        categoryMatcher.addCategory(name, iconRes, extensions)
    }

    suspend fun removeCategory(categoryId: String) {
        categoryMatcher.removeCategory(categoryId)
    }

    suspend fun addSubCategory(categoryId: String, extension: String) {
        categoryMatcher.addSubCategory(categoryId, extension)
    }

    suspend fun removeSubCategory(subCategoryId: String) {
        categoryMatcher.removeSubCategory(subCategoryId)
    }

    suspend fun renameCategory(categoryId: String, newName: String) = withContext(Dispatchers.IO) {
        try {
            val category = categoryDao.getCategoryById(categoryId).first() ?: return@withContext
            val updatedCategory = category.copy(
                name = newName,
                sortOrder = category.sortOrder
            )
            categoryDao.updateCategory(updatedCategory)
        } catch (e: Exception) {
        }
    }

    fun matchCategory(fileName: String): CategoryMatchResult {
        val result = categoryMatcher.match(fileName)
        return CategoryMatchResult(
            categoryId = result.categoryId,
            categoryName = result.categoryName,
            subId = result.subCategoryId,
            extension = result.subCategoryExtension
        )
    }

    suspend fun initDefaultCategories() {
        categoryMatcher.initDefaultCategories()
    }
}
