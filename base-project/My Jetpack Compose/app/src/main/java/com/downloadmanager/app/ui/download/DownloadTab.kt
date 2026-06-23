package com.downloadmanager.app.ui.download

import com.downloadmanager.app.R
import com.downloadmanager.app.data.entity.DownloadStatus

enum class DownloadTab(
    val titleRes: Int,
    val status: DownloadStatus?
) {
    ALL(R.string.tab_all, null),
    DOWNLOADING(R.string.tab_downloading, DownloadStatus.DOWNLOADING),
    COMPLETED(R.string.tab_completed, DownloadStatus.COMPLETED),
    FAILED(R.string.tab_failed, DownloadStatus.FAILED)
}
