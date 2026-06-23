package com.downloadmanager.app.category

import kotlinx.coroutines.flow.Flow

interface CategoryMatcher {
    fun match(fileName: String): CategoryMatchResult
    suspend fun addCategory(categoryName: String, iconRes: String?, extensions: List<String>): Long
    suspend fun removeCategory(categoryId: String)
    suspend fun addSubCategory(categoryId: String, extension: String): Long
    suspend fun removeSubCategory(subCategoryId: String)
    fun getAllCategories(): Flow<List<CategoryInfo>>
    fun getSubCategories(categoryId: String): Flow<List<SubCategoryInfo>>
    suspend fun initDefaultCategories()
}
