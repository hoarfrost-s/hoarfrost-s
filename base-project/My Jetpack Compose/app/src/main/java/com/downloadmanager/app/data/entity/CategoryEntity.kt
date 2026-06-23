package com.downloadmanager.app.data.entity

data class CategoryEntity(
    val id: String,
    val name: String,
    val iconRes: String? = null,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long
)
