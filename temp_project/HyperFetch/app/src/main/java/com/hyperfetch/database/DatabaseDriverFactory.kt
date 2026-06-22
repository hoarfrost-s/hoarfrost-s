package com.hyperfetch.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * SQLDelight 数据库驱动工厂
 */
class DatabaseDriverFactory(private val context: Context) {

    fun create(): SqlDriver {
        return AndroidSqliteDriver(
            schema = HyperFetchDatabase.Schema,
            context = context,
            name = "hyperfetch_db"
        )
    }
}
