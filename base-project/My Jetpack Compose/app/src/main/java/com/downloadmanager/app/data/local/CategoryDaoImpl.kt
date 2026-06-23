package com.downloadmanager.app.data.local

import com.downloadmanager.app.data.DownloadDatabase
import com.downloadmanager.app.data.entity.CategoryEntity
import com.downloadmanager.app.data.entity.CategoryMatchResult
import com.downloadmanager.app.data.entity.SubCategoryEntity
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryDaoImpl(
    private val database: DownloadDatabase
) : CategoryDao {

    private val queries get() = database.categoryQueries

    override fun getAllCategories(): Flow<List<CategoryEntity>> {
        return queries.getAllCategories()
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getCategoryById(id: String): Flow<CategoryEntity?> {
        return queries.getCategoryById(id)
            .asFlow()
            .mapToOneOrNull()
            .map { it?.toEntity() }
    }

    override fun getCategoryByName(name: String): Flow<CategoryEntity?> {
        return queries.getCategoryByName(name)
            .asFlow()
            .mapToOneOrNull()
            .map { it?.toEntity() }
    }

    override suspend fun insertCategory(category: CategoryEntity) {
        queries.insertCategory(
            id = category.id,
            name = category.name,
            icon_res = category.iconRes,
            sort_order = category.sortOrder.toLong(),
            is_default = if (category.isDefault) 1L else 0L,
            created_at = category.createdAt
        )
    }

    override suspend fun updateCategory(category: CategoryEntity) {
        queries.updateCategory(
            name = category.name,
            icon_res = category.iconRes,
            sort_order = category.sortOrder.toLong(),
            id = category.id
        )
    }

    override suspend fun deleteCategory(id: String) {
        queries.deleteCategory(id)
    }

    override fun getSubCategoriesByCategory(categoryId: String): Flow<List<SubCategoryEntity>> {
        return queries.getSubCategoriesByCategory(categoryId)
            .asFlow()
            .mapToList()
            .map { list -> list.map { it.toEntity() } }
    }

    override fun getSubCategoryById(id: String): Flow<SubCategoryEntity?> {
        return queries.getSubCategoryById(id)
            .asFlow()
            .mapToOneOrNull()
            .map { it?.toEntity() }
    }

    override suspend fun insertSubCategory(subCategory: SubCategoryEntity) {
        queries.insertSubCategory(
            id = subCategory.id,
            extension = subCategory.extension,
            category_id = subCategory.categoryId,
            sort_order = subCategory.sortOrder.toLong(),
            created_at = subCategory.createdAt
        )
    }

    override suspend fun deleteSubCategory(id: String) {
        queries.deleteSubCategory(id)
    }

    override fun matchByExtension(extension: String): Flow<CategoryMatchResult?> {
        return queries.matchByExtension(extension)
            .asFlow()
            .mapToOneOrNull()
            .map { result ->
                result?.let {
                    CategoryMatchResult(
                        categoryId = it.category_id,
                        categoryName = it.category_name,
                        subId = it.sub_id,
                        extension = it.extension
                    )
                }
            }
    }

    override suspend fun insertDefaultCategories(createdAt: Long) {
        queries.insertDefaultCategories(
            createdAt, createdAt, createdAt, createdAt, createdAt, createdAt
        )
    }

    override suspend fun insertDefaultSubCategories(createdAt: Long) {
        queries.insertDefaultSubCategories(
            createdAt, createdAt, createdAt, createdAt, createdAt, createdAt, createdAt, createdAt, createdAt,
            createdAt, createdAt, createdAt, createdAt, createdAt, createdAt, createdAt,
            createdAt, createdAt, createdAt, createdAt, createdAt, createdAt, createdAt,
            createdAt, createdAt, createdAt,
            createdAt, createdAt, createdAt, createdAt, createdAt, createdAt, createdAt, createdAt
        )
    }

    private fun com.downloadmanager.app.data.Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            iconRes = icon_res,
            sortOrder = sort_order.toInt(),
            isDefault = is_default == 1L,
            createdAt = created_at
        )
    }

    private fun com.downloadmanager.app.data.Sub_category.toEntity(): SubCategoryEntity {
        return SubCategoryEntity(
            id = id,
            extension = extension,
            categoryId = category_id,
            sortOrder = sort_order.toInt(),
            createdAt = created_at
        )
    }
}
