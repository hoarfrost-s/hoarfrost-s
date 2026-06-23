package com.downloadmanager.app.category

import com.downloadmanager.app.data.entity.CategoryEntity
import com.downloadmanager.app.data.entity.SubCategoryEntity
import com.downloadmanager.app.data.local.CategoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class CategoryMatcherImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryMatcher {

    private val extensionCache = ConcurrentHashMap<String, CategoryMatchResult>()

    private val otherCategoryId = "other"
    private val otherCategoryName = "其他"
    private val otherSubCategoryId = "other"
    private val otherExtension = ""

    override fun match(fileName: String): CategoryMatchResult {
        val extension = extractExtension(fileName)
        if (extension.isEmpty()) {
            return otherResult()
        }
        return extensionCache[extension] ?: otherResult()
    }

    override suspend fun addCategory(categoryName: String, iconRes: String?, extensions: List<String>): Long {
        val categoryId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val category = CategoryEntity(
            id = categoryId,
            name = categoryName,
            iconRes = iconRes,
            sortOrder = 0,
            isDefault = false,
            createdAt = now
        )
        categoryDao.insertCategory(category)

        extensions.forEachIndexed { index, ext ->
            val subCategoryId = UUID.randomUUID().toString()
            val subCategory = SubCategoryEntity(
                id = subCategoryId,
                extension = ext.lowercase(),
                categoryId = categoryId,
                sortOrder = index,
                createdAt = now
            )
            categoryDao.insertSubCategory(subCategory)
            extensionCache[ext.lowercase()] = CategoryMatchResult(
                categoryId = categoryId,
                categoryName = categoryName,
                subCategoryId = subCategoryId,
                subCategoryExtension = ext.lowercase()
            )
        }

        return 1L
    }

    override suspend fun removeCategory(categoryId: String) {
        val subCategories = categoryDao.getSubCategoriesByCategory(categoryId).first()
        subCategories.forEach { subCategory ->
            extensionCache.remove(subCategory.extension)
        }
        categoryDao.deleteCategory(categoryId)
    }

    override suspend fun addSubCategory(categoryId: String, extension: String): Long {
        val category = categoryDao.getCategoryById(categoryId).first()
            ?: return 0L

        val subCategoryId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val ext = extension.lowercase()

        val subCategory = SubCategoryEntity(
            id = subCategoryId,
            extension = ext,
            categoryId = categoryId,
            sortOrder = 0,
            createdAt = now
        )
        categoryDao.insertSubCategory(subCategory)

        extensionCache[ext] = CategoryMatchResult(
            categoryId = categoryId,
            categoryName = category.name,
            subCategoryId = subCategoryId,
            subCategoryExtension = ext
        )

        return 1L
    }

    override suspend fun removeSubCategory(subCategoryId: String) {
        val subCategory = categoryDao.getSubCategoryById(subCategoryId).first()
        if (subCategory != null) {
            extensionCache.remove(subCategory.extension)
        }
        categoryDao.deleteSubCategory(subCategoryId)
    }

    override fun getAllCategories(): Flow<List<CategoryInfo>> {
        return categoryDao.getAllCategories().flatMapLatest { categories ->
            if (categories.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val subCategoryFlows = categories.map { category ->
                    categoryDao.getSubCategoriesByCategory(category.id).map { subCategories ->
                        category to subCategories
                    }
                }
                combine(subCategoryFlows) { array ->
                    array.map { (category, subCategories) ->
                        CategoryInfo(
                            id = category.id,
                            name = category.name,
                            iconRes = category.iconRes,
                            sortOrder = category.sortOrder,
                            isDefault = category.isDefault,
                            subCategories = subCategories.map { subCategory ->
                                SubCategoryInfo(
                                    id = subCategory.id,
                                    extension = subCategory.extension,
                                    categoryId = subCategory.categoryId,
                                    sortOrder = subCategory.sortOrder
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override fun getSubCategories(categoryId: String): Flow<List<SubCategoryInfo>> {
        return categoryDao.getSubCategoriesByCategory(categoryId).map { subCategories ->
            subCategories.map { subCategory ->
                SubCategoryInfo(
                    id = subCategory.id,
                    extension = subCategory.extension,
                    categoryId = subCategory.categoryId,
                    sortOrder = subCategory.sortOrder
                )
            }
        }
    }

    override suspend fun initDefaultCategories() {
        val categories = categoryDao.getAllCategories().first()
        if (categories.isNotEmpty()) {
            loadCacheFromDb()
            return
        }

        val now = System.currentTimeMillis()
        categoryDao.insertDefaultCategories(now)
        categoryDao.insertDefaultSubCategories(now)
        loadCacheFromDb()
    }

    private suspend fun loadCacheFromDb() {
        val categories = categoryDao.getAllCategories().first()
        categories.forEach { category ->
            val subCategories = categoryDao.getSubCategoriesByCategory(category.id).first()
            subCategories.forEach { subCategory ->
                extensionCache[subCategory.extension] = CategoryMatchResult(
                    categoryId = category.id,
                    categoryName = category.name,
                    subCategoryId = subCategory.id,
                    subCategoryExtension = subCategory.extension
                )
            }
        }
    }

    private fun extractExtension(fileName: String): String {
        val name = fileName.substringAfterLast('/').substringAfterLast('\\')
        if (name.isEmpty() || !name.contains('.')) {
            return ""
        }
        val extension = name.substringAfterLast('.')
        return extension.lowercase()
    }

    private fun otherResult(): CategoryMatchResult {
        return CategoryMatchResult(
            categoryId = otherCategoryId,
            categoryName = otherCategoryName,
            subCategoryId = otherSubCategoryId,
            subCategoryExtension = otherExtension
        )
    }
}
