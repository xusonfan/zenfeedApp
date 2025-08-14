package com.ddyy.zenfeed.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class UpdateManager(private val context: Context) {

    fun startDownload(url: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("正在下载更新...")
            .setDescription(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "开始下载更新...", Toast.LENGTH_SHORT).show()
    }

    fun installApk(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val apkUri = downloadManager.getUriForDownloadedFile(downloadId)

        if (apkUri != null) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (localUriIndex != -1) {
                    val localUriString = cursor.getString(localUriIndex)
                    if (localUriString != null) {
                        val localFile = File(Uri.parse(localUriString).path!!)
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            context.applicationContext.packageName + ".fileprovider",
                            localFile
                        )

                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(installIntent)
                        } catch (e: Exception) {
                            Log.e("UpdateManager", "启动安装失败", e)
                            Toast.makeText(context, "安装失败，请在下载文件夹中手动安装", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                cursor.close()
            }
        } else {
            Log.e("UpdateManager", "获取下载文件URI失败")
            Toast.makeText(context, "安装失败，无法找到下载的文件", Toast.LENGTH_LONG).show()
        }
    }
}