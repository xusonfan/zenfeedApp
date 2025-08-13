package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ddyy.zenfeed.ui.logging.LogContentCard
import com.ddyy.zenfeed.ui.logging.StatusCard

@Preview(showBackground = true)
@Composable
fun LoggingScreenPreview() {
    MaterialTheme {
        // 为预览创建模拟状态的简化版本
        LoggingScreenContent(
            isLogging = false,
            logContent = "2025-01-01 12:00:00.000  1234  1234 I LoggingForegroundService: 开始记录日志\n" +
                    "2025-01-01 12:00:01.000  1234  1234 D FeedsScreen: 加载RSS订阅\n" +
                    "2025-01-01 12:00:02.000  1234  1234 W ApiClient: 网络请求超时\n" +
                    "2025-01-01 12:00:03.000  1234  1234 E PlayerService: 播放失败",
            zoomLevel = 1.0f,
            onStartLogging = {},
            onStopLogging = {},
            onClearLogs = {},
            onRefreshLogs = {},
            onShareLogs = {},
            onZoomIn = {},
            onZoomOut = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoggingScreenContent(
    isLogging: Boolean,
    logContent: String,
    zoomLevel: Float,
    onStartLogging: () -> Unit,
    onStopLogging: () -> Unit,
    onClearLogs: () -> Unit,
    onRefreshLogs: () -> Unit,
    onShareLogs: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "日志记录",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 状态卡片
            StatusCard(
                isLogging = isLogging,
                onToggleLogging = if (isLogging) onStopLogging else onStartLogging,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 刷新按钮
                OutlinedButton(
                    onClick = onRefreshLogs,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新")
                }

                // 清空按钮
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }

                // 分享按钮
                OutlinedButton(
                    onClick = onShareLogs,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("分享")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 日志内容卡片
            LogContentCard(
                logContent = logContent,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}