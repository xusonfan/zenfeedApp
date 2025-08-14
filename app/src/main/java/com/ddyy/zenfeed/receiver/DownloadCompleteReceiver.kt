package com.ddyy.zenfeed.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ddyy.zenfeed.ui.UpdateManager

class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != -1L) {
                Log.d("DownloadCompleteReceiver", "下载完成，ID: $id")
                val updateManager = UpdateManager(context)
                updateManager.installApk(id)
            }
        }
    }
}