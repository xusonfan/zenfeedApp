package com.ddyy.zenfeed.extension

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * 颜色扩展函数 - 提供标签颜色生成等工具方法
 */

/**
 * 根据标签生成颜色三元组（背景色、边框色、文字色）
 */
@Composable
fun String.generateTagColors(): Triple<Color, Color, Color> {
    val colorIndex = this.hashCode().let { if (it < 0) -it else it } % 6
    return when (colorIndex) {
        0 -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary
        )
        1 -> Triple(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondary
        )
        2 -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.tertiary
        )
        3 -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.error
        )
        4 -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Triple(
            MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.inversePrimary
        )
    }
}

/**
 * 获取主题相关的背景色
 */
fun getThemeBackgroundColor(isDarkTheme: Boolean): Int {
    return if (isDarkTheme) {
        "#1E1E1E".toColorInt()
    } else {
        "#FFFFFF".toColorInt()
    }
}

/**
 * 字符串颜色转Int
 */
fun String.toColorInt(): Int {
    return android.graphics.Color.parseColor(this)
}

/**
 * 获取播客按钮的容器颜色
 */
@Composable
fun getPodcastButtonContainerColor(isCurrentlyPlaying: Boolean): Color {
    return if (isCurrentlyPlaying) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    }
}

/**
 * 获取播客按钮的内容颜色
 */
@Composable
fun getPodcastButtonContentColor(isCurrentlyPlaying: Boolean): Color {
    return if (isCurrentlyPlaying) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
}

/**
 * 获取卡片容器颜色（根据播放状态）
 */
@Composable
fun getCardContainerColor(isCurrentPlaying: Boolean): Color {
    return if (isCurrentPlaying) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
}


/**
 * 获取文字颜色（根据播放状态）
 */
@Composable
fun getTextColor(isCurrentPlaying: Boolean): Color {
    return if (isCurrentPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

/**
 * 获取文字权重（根据播放状态）
 */
fun getFontWeight(isCurrentPlaying: Boolean): FontWeight {
    return if (isCurrentPlaying) {
        FontWeight.Bold
    } else {
        FontWeight.Normal
    }
}