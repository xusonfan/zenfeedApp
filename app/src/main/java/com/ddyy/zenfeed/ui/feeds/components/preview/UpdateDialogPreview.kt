package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ddyy.zenfeed.data.model.GithubRelease
import com.ddyy.zenfeed.ui.feeds.components.dialog.UpdateDialog

@Preview(showBackground = true)
@Composable
fun UpdateDialogPreview() {
    MaterialTheme {
        UpdateDialog(
            releaseInfo = GithubRelease(
                tagName = "v1.0.1",
                name = "Release v1.0.1",
                body = "这是一个更新日志的示例。\n\n- 修复了 bug A\n- 优化了功能 B\n- 新增了特性 C",
                assets = listOf(
                    GithubRelease.Asset(
                        browserDownloadUrl = "https://example.com/app-release.apk",
                        name = "app-release.apk",
                        size = 12345678
                    )
                )
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}