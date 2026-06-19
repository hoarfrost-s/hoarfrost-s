package com.downloadmanager.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.downloadmanager.common.UrlParser

class ShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handled by MainActivity's onNewIntent via ACTION_SEND
    }
}