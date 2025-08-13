package com.ddyy.zenfeed.ui.feeds.components.dialog

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun UpdateDialog(
    releaseInfo: com.ddyy.zenfeed.data.model.GithubRelease,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val apkAsset = releaseInfo.assets.find { it.name.endsWith(".apk") }
    if (apkAsset == null) {
        Log.e("UpdateDialog", "在 Release 中未找到 APK 文件")
        onDismiss()
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 ${releaseInfo.tagName}") },
        text = {
            Column {
                Text("更新日志:", fontWeight = FontWeight.Bold)
                Text(releaseInfo.body)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(apkAsset.browserDownloadUrl) }) {
                Text("立即更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}