package com.ddyy.zenfeed.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 主题相关扩展函数 - 提供主题模式图标等工具方法
 */

/**
 * 根据主题模式获取对应的图标
 */
fun getThemeModeIcon(themeMode: String): ImageVector {
    return when (themeMode) {
        "light" -> Icons.Default.LightMode
        "dark" -> Icons.Default.DarkMode
        else -> Icons.Default.AutoMode // 跟随系统使用自动模式图标
    }
}

/**
 * 根据主题模式获取对应的描述文字
 */
fun getThemeModeDescription(themeMode: String): String {
    return when (themeMode) {
        "light" -> "日间模式"
        "dark" -> "夜间模式"
        "system" -> "跟随系统"
        else -> "未知"
    }
}

/**
 * 根据代理状态获取描述文字
 */
fun getProxyStatusDescription(isEnabled: Boolean): String {
    return if (isEnabled) "代理已启用" else "代理已禁用"
}


/**
 * 获取乱序模式图标
 */
fun getShuffleModeIcon(isShuffle: Boolean): ImageVector {
    return if (isShuffle) Icons.Default.ShuffleOn else Icons.Default.Shuffle
}

/**
 * 获取循环模式图标
 */
fun getRepeatModeIcon(isRepeat: Boolean): ImageVector {
    return Icons.AutoMirrored.Filled.PlaylistPlay // 使用播放列表图标表示循环
}


/**
 * 获取当前播放图标
 */
fun getCurrentPlayingIcon(isCurrentPlaying: Boolean): ImageVector {
    return if (isCurrentPlaying) {
        Icons.Default.PlayArrow
    } else {
        Icons.AutoMirrored.Filled.PlaylistPlay
    }
}

/**
 * 获取主题色（根据状态）
 */
@Composable
fun getThemeColorByStatus(isActive: Boolean): Color {
    return if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * 获取标签文字大小
 */
fun getTagFontSize(isDetail: Boolean = false): TextUnit {
    return if (isDetail) {
        11.sp
    } else {
        10.sp
    }
}

/**
 * 获取标签文字透明度
 */
fun getTagTextAlpha(isRead: Boolean, isDetail: Boolean = false): Float {
    return if (isDetail) {
        0.9f
    } else {
        if (isRead) 0.6f else 0.8f
    }
}