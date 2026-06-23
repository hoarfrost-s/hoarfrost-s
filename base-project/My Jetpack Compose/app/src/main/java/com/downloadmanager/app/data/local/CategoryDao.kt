package com.downloadmanager.app.data.local

import com.downloadmanager.app.data.entity.CategoryEntity
import com.downloadmanager.app.data.entity.CategoryMatchResult
import com.downloadmanager.app.data.entity.SubCategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryDao {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoryById(id: String): Flow<CategoryEntity?>
    fun getCategoryByName(name: String): Flow<CategoryEntity?>
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(id: String)
    fun getSubCategoriesByCategory(categoryId: String): Flow<List<SubCategoryEntity>>
    fun getSubCategoryById(id: String): Flow<SubCategoryEntity?>
    suspend fun insertSubCategory(subCategory: SubCategoryEntity)
    suspend fun deleteSubCategory(id: String)
    fun matchByExtension(extension: String): Flow<CategoryMatchResult?>
    suspend fun insertDefaultCategories(createdAt: Long)
    suspend fun insertDefaultSubCategories(createdAt: Long)
}
