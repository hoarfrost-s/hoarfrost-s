package com.downloadmanager.app.data.entity

data class SubCategoryEntity(
    val id: String,
    val extension: String,
    val categoryId: String,
    val sortOrder: Int = 0,
    val createdAt: Long
)
