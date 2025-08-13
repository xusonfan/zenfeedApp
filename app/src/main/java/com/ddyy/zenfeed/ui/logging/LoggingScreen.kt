package com.ddyy.zenfeed.ui.logging

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

/**
 * 日志记录页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoggingViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLogging by viewModel.isLogging.collectAsState()
    val logContent by viewModel.logContent.collectAsState()
    
    Scaffold(
        modifier = modifier,
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
                onToggleLogging = { viewModel.toggleLogging() },
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
                    onClick = { viewModel.refreshLogs() },
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
                    onClick = { viewModel.clearLogs() },
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
                    onClick = {
                        shareLogFile(context, viewModel.getLogFilePath())
                    },
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

/**
 * 状态卡片组件
 */
@Composable
fun StatusCard(
    isLogging: Boolean,
    onToggleLogging: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLogging) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLogging) "正在记录日志" else "日志记录已停止",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isLogging) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLogging) {
                        "正在实时收集应用日志信息"
                    } else {
                        "点击开始按钮开始记录日志"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLogging) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 切换按钮
            FilledTonalButton(
                onClick = onToggleLogging,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = if (isLogging) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isLogging) "停止" else "开始",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * 日志内容卡片组件
 */
@Composable
fun LogContentCard(
    logContent: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "日志内容",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 缩放控制按钮
                ZoomControlButtons(
                    modifier = Modifier,
                    compact = true
                )
            }
            
            // 日志内容区域
            if (logContent.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无日志内容\n点击开始按钮开始记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                ZoomableLogContent(
                    logContent = logContent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 分享日志文件
 */
private fun shareLogFile(context: android.content.Context, logFilePath: String) {
    try {
        val logFile = File(logFilePath)
        if (!logFile.exists()) {
            android.widget.Toast.makeText(context, "日志文件不存在", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            logFile
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Zenfeed 应用日志")
            putExtra(Intent.EXTRA_TEXT, "这是 Zenfeed 应用的调试日志文件")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "分享日志文件"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context, 
            "分享失败: ${e.message}", 
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

// 全局缩放状态
private var globalScale = mutableFloatStateOf(1f)

/**
 * 缩放控制按钮组件
 */
@Composable
fun ZoomControlButtons(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val scale by globalScale
    val minScale = 0.5f
    val maxScale = 3f
    
    Row(
        modifier = if (compact) modifier else modifier.fillMaxWidth(),
        horizontalArrangement = if (compact) Arrangement.End else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 缩小按钮
        IconButton(
            onClick = {
                globalScale.floatValue = (globalScale.floatValue - 0.2f).coerceAtLeast(minScale)
            },
            enabled = scale > minScale,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "缩小",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 显示当前缩放比例
        Text(
            text = "${(scale * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = if (compact) 4.dp else 16.dp)
        )
        
        // 放大按钮
        IconButton(
            onClick = {
                globalScale.floatValue = (globalScale.floatValue + 0.2f).coerceAtMost(maxScale)
            },
            enabled = scale < maxScale,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "放大",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 可缩放的日志内容组件
 */
@Composable
fun ZoomableLogContent(
    logContent: String,
    modifier: Modifier = Modifier
) {
    val scale by globalScale
    
    SelectionContainer {
        Text(
            text = formatLogContent(logContent),
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = (12 * scale).sp
            ),
            softWrap = false, // 禁用软换行
            maxLines = Int.MAX_VALUE // 允许无限行数
        )
    }
}

/**
 * 格式化日志内容，为不同级别的日志添加颜色
 */
@Composable
fun formatLogContent(logContent: String): AnnotatedString {
    val defaultColor = MaterialTheme.colorScheme.onSurface
    return remember(logContent, defaultColor) {
        buildAnnotatedString {
            val lines = logContent.split('\n')
            for ((index, line) in lines.withIndex()) {
                if (line.isNotBlank()) {
                    // 解析日志级别
                    val logLevel = extractLogLevel(line)
                    val color = getLogLevelColor(logLevel, defaultColor)
                    
                    withStyle(SpanStyle(color = color)) {
                        append(line)
                    }
                } else {
                    append(line)
                }
                
                if (index < lines.size - 1) {
                    append('\n')
                }
            }
        }
    }
}

/**
 * 从日志行中提取日志级别
 */
private fun extractLogLevel(logLine: String): String {
    // 匹配threadtime格式的日志级别: MM-dd HH:mm:ss.SSS PID TID LEVEL TAG: message
    val regex = Regex("""^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3}\s+\d+\s+\d+\s+([VDIWEF])\s+""")
    val matchResult = regex.find(logLine)
    return matchResult?.groupValues?.get(1) ?: ""
}

/**
 * 根据日志级别获取对应颜色
 */
private fun getLogLevelColor(logLevel: String, defaultColor: Color): Color {
    return when (logLevel) {
        "V" -> Color(0xFF888888) // Verbose - 灰色
        "D" -> Color(0xFF2196F3) // Debug - 蓝色
        "I" -> Color(0xFF4CAF50) // Info - 绿色
        "W" -> Color(0xFFFF9800) // Warning - 橙色
        "E" -> Color(0xFFF44336) // Error - 红色
        else -> defaultColor // 默认颜色
    }
}





