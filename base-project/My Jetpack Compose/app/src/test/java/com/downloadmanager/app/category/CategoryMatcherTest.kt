package com.downloadmanager.app.category

import com.downloadmanager.app.data.entity.CategoryEntity
import com.downloadmanager.app.data.entity.SubCategoryEntity
import com.downloadmanager.app.data.local.CategoryDao
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CategoryMatcherTest {

    private lateinit var categoryDao: CategoryDao
    private lateinit var categoryMatcher: CategoryMatcherImpl

    @Before
    fun setup() {
        categoryDao = mockk()
        categoryMatcher = CategoryMatcherImpl(categoryDao)
    }

    @Test
    fun testMatchMp4() = runTest {
        val categoryEntity = CategoryEntity(
            id = "video",
            name = "视频",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "mp4",
            extension = "mp4",
            categoryId = "video",
            sortOrder = 0,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("video") } returns flowOf(listOf(subCategoryEntity))

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.match("test.mp4")

        assertEquals("video", result.categoryId)
        assertEquals("视频", result.categoryName)
        assertEquals("mp4", result.subCategoryId)
        assertEquals("mp4", result.subCategoryExtension)
    }

    @Test
    fun testMatchMkv() = runTest {
        val categoryEntity = CategoryEntity(
            id = "video",
            name = "视频",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "mkv",
            extension = "mkv",
            categoryId = "video",
            sortOrder = 1,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("video") } returns flowOf(listOf(subCategoryEntity))

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.match("test.mkv")

        assertEquals("video", result.categoryId)
        assertEquals("视频", result.categoryName)
        assertEquals("mkv", result.subCategoryId)
        assertEquals("mkv", result.subCategoryExtension)
    }

    @Test
    fun testMatchApk() = runTest {
        val categoryEntity = CategoryEntity(
            id = "app",
            name = "应用",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "apk",
            extension = "apk",
            categoryId = "app",
            sortOrder = 0,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("app") } returns flowOf(listOf(subCategoryEntity))

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.match("test.apk")

        assertEquals("app", result.categoryId)
        assertEquals("应用", result.categoryName)
        assertEquals("apk", result.subCategoryId)
        assertEquals("apk", result.subCategoryExtension)
    }

    @Test
    fun testMatchZip() = runTest {
        val categoryEntity = CategoryEntity(
            id = "archive",
            name = "压缩包",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "zip",
            extension = "zip",
            categoryId = "archive",
            sortOrder = 0,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("archive") } returns flowOf(listOf(subCategoryEntity))

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.match("test.zip")

        assertEquals("archive", result.categoryId)
        assertEquals("压缩包", result.categoryName)
        assertEquals("zip", result.subCategoryId)
        assertEquals("zip", result.subCategoryExtension)
    }

    @Test
    fun testMatchUnknown() = runTest {
        every { categoryDao.getAllCategories() } returns flowOf(emptyList())

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.match("test.unknown")

        assertEquals("other", result.categoryId)
        assertEquals("其他", result.categoryName)
        assertEquals("other", result.subCategoryId)
        assertEquals("", result.subCategoryExtension)
    }

    @Test
    fun testMatchCaseInsensitive() = runTest {
        val categoryEntity = CategoryEntity(
            id = "video",
            name = "视频",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "mp4",
            extension = "mp4",
            categoryId = "video",
            sortOrder = 0,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("video") } returns flowOf(listOf(subCategoryEntity))

        categoryMatcher.initDefaultCategories()

        val result1 = categoryMatcher.match("test.MP4")
        val result2 = categoryMatcher.match("test.Mp4")
        val result3 = categoryMatcher.match("test.mP4")

        assertEquals("video", result1.categoryId)
        assertEquals("video", result2.categoryId)
        assertEquals("video", result3.categoryId)
    }

    @Test
    fun testMatchWithPath() = runTest {
        val categoryEntity = CategoryEntity(
            id = "video",
            name = "视频",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "mp4",
            extension = "mp4",
            categoryId = "video",
            sortOrder = 0,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("video") } returns flowOf(listOf(subCategoryEntity))

        categoryMatcher.initDefaultCategories()

        val result1 = categoryMatcher.match("/sdcard/Download/test.mp4")
        val result2 = categoryMatcher.match("C:\\Downloads\\test.mp4")

        assertEquals("video", result1.categoryId)
        assertEquals("video", result2.categoryId)
    }

    @Test
    fun testMatchMultipleDots() = runTest {
        val categoryEntity = CategoryEntity(
            id = "video",
            name = "视频",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "mp4",
            extension = "mp4",
            categoryId = "video",
            sortOrder = 0,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("video") } returns flowOf(listOf(subCategoryEntity))

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.match("my.video.file.mp4")

        assertEquals("video", result.categoryId)
        assertEquals("mp4", result.subCategoryExtension)
    }

    @Test
    fun testMatchNoExtension() = runTest {
        every { categoryDao.getAllCategories() } returns flowOf(emptyList())

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.match("noextension")

        assertEquals("other", result.categoryId)
        assertEquals("其他", result.categoryName)
    }

    @Test
    fun testAddAndRemoveCategory() = runTest {
        every { categoryDao.getAllCategories() } returns flowOf(emptyList())
        every { categoryDao.insertCategory(any()) } returns Unit
        every { categoryDao.insertSubCategory(any()) } returns Unit
        every { categoryDao.deleteCategory(any()) } returns Unit

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.addCategory("新分类", listOf("xyz", "abc"))
        assertEquals(1L, result)

        coVerify(exactly = 1) { categoryDao.insertCategory(any()) }
        coVerify(exactly = 2) { categoryDao.insertSubCategory(any()) }

        val matchResult1 = categoryMatcher.match("test.xyz")
        val matchResult2 = categoryMatcher.match("test.abc")
        assertEquals("新分类", matchResult1.categoryName)
        assertEquals("新分类", matchResult2.categoryName)

        every { categoryDao.getSubCategoriesByCategory(any()) } returns flowOf(
            listOf(
                SubCategoryEntity(
                    id = "sub1",
                    extension = "xyz",
                    categoryId = "new_cat",
                    sortOrder = 0,
                    createdAt = 0L
                ),
                SubCategoryEntity(
                    id = "sub2",
                    extension = "abc",
                    categoryId = "new_cat",
                    sortOrder = 1,
                    createdAt = 0L
                )
            )
        )

        categoryMatcher.removeCategory("new_cat")

        coVerify(exactly = 1) { categoryDao.deleteCategory("new_cat") }

        val afterRemove = categoryMatcher.match("test.xyz")
        assertEquals("other", afterRemove.categoryId)
    }

    @Test
    fun testAddAndRemoveSubCategory() = runTest {
        val categoryEntity = CategoryEntity(
            id = "video",
            name = "视频",
            sortOrder = 0,
            isDefault = true,
            createdAt = 0L
        )
        val subCategoryEntity = SubCategoryEntity(
            id = "mp4",
            extension = "mp4",
            categoryId = "video",
            sortOrder = 0,
            createdAt = 0L
        )

        every { categoryDao.getAllCategories() } returns flowOf(listOf(categoryEntity))
        every { categoryDao.getSubCategoriesByCategory("video") } returns flowOf(listOf(subCategoryEntity))
        every { categoryDao.getCategoryById("video") } returns flowOf(categoryEntity)
        every { categoryDao.insertSubCategory(any()) } returns Unit
        every { categoryDao.deleteSubCategory(any()) } returns Unit

        categoryMatcher.initDefaultCategories()

        val result = categoryMatcher.addSubCategory("video", "avi")
        assertEquals(1L, result)

        coVerify(exactly = 1) { categoryDao.insertSubCategory(any()) }

        val matchResult = categoryMatcher.match("test.avi")
        assertEquals("video", matchResult.categoryId)
        assertEquals("avi", matchResult.subCategoryExtension)

        every { categoryDao.getSubCategoryById("any_sub") } returns flowOf(
            SubCategoryEntity(
                id = "any_sub",
                extension = "avi",
                categoryId = "video",
                sortOrder = 1,
                createdAt = 0L
            )
        )

        categoryMatcher.removeSubCategory("any_sub")

        coVerify(exactly = 1) { categoryDao.deleteSubCategory("any_sub") }
    }
}
