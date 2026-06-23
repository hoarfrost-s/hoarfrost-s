package com.downloadmanager.app.category

data class CategoryInfo(
    val id: String,
    val name: String,
    val iconRes: String? = null,
    val sortOrder: Int,
    val isDefault: Boolean,
    val subCategories: List<SubCategoryInfo> = emptyList()
)
