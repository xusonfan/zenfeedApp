package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ddyy.zenfeed.ui.logging.StatusCard

@Preview(showBackground = true)
@Composable
fun StatusCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                isLogging = true,
                onToggleLogging = {}
            )
            StatusCard(
                isLogging = false,
                onToggleLogging = {}
            )
        }
    }
}