package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ddyy.zenfeed.ui.about.AboutScreen

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen(
            onBack = {}
        )
    }
}