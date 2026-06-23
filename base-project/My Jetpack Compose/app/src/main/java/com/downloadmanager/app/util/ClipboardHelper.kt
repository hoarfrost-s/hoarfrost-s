package com.downloadmanager.app.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val urlPattern = Pattern.compile(
        "^(https?|ftp)://.*$",
        Pattern.CASE_INSENSITIVE
    )

    private val clipboardManager: ClipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    fun getClipboardText(): String? {
        return try {
            if (!clipboardManager.hasPrimaryClip()) {
                return null
            }
            val description = clipboardManager.primaryClipDescription
            if (description == null || !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                return null
            }
            val clip = clipboardManager.primaryClip
            if (clip == null || clip.itemCount == 0) {
                return null
            }
            clip.getItemAt(0).text?.toString()
        } catch (e: Exception) {
            null
        }
    }

    fun isUrl(text: String): Boolean {
        return urlPattern.matcher(text.trim()).matches()
    }

    fun getClipboardUrl(): String? {
        val text = getClipboardText() ?: return null
        return if (isUrl(text)) text.trim() else null
    }

    fun setClipboardText(label: String, text: String) {
        try {
            val clip = ClipData.newPlainText(label, text)
            clipboardManager.setPrimaryClip(clip)
        } catch (e: Exception) {
        }
    }

    fun clearClipboard() {
        try {
            val clip = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(clip)
        } catch (e: Exception) {
        }
    }
}
