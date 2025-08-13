package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ddyy.zenfeed.ui.feeds.components.list.ModernLoadingScreen

@Preview(showBackground = true)
@Composable
fun ModernLoadingScreenPreview() {
    MaterialTheme {
        ModernLoadingScreen()
    }
}