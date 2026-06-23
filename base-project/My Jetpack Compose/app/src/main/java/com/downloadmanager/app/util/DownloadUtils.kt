package com.downloadmanager.app.util

import java.util.UUID

object DownloadUtils {

    fun generateTaskId(): String {
        return UUID.randomUUID().toString()
    }
}
