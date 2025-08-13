package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ddyy.zenfeed.ui.feeds.components.list.ModernErrorScreen

@Preview(showBackground = true)
@Composable
fun ModernErrorScreenPreview() {
    MaterialTheme {
        ModernErrorScreen(
            onRetry = { /* 预览中的重试逻辑 */ }
        )
    }
}