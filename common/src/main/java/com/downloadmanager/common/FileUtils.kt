package com.downloadmanager.common

import java.io.File

object FileUtils {
    fun ensureDir(dir: File): File {
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTempDir(baseDir: File, taskId: String): File {
        return ensureDir(File(baseDir, ".tmp_$taskId"))
    }

    fun mergeFiles(segmentFiles: List<File>, outputFile: File) {
        outputFile.outputStream().buffered().use { out ->
            segmentFiles.forEach { segmentFile ->
                if (segmentFile.exists()) {
                    segmentFile.inputStream().buffered().use { input ->
                        input.copyTo(out)
                    }
                }
            }
        }
        // Clean up temp files
        segmentFiles.forEach { it.delete() }
        segmentFiles.firstOrNull()?.parentFile?.delete()
    }
}