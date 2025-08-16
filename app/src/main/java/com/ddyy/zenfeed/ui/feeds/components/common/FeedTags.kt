package com.ddyy.zenfeed.ui.feeds.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.extension.generateTagColors
import com.ddyy.zenfeed.extension.getTagFontSize
import com.ddyy.zenfeed.extension.splitTags
import com.ddyy.zenfeed.extension.withReadTagAlpha

/**
 * 文章标签展示组件
 * @param feed 文章数据
 * @param maxTags 最大显示标签数量
 * @param isDetail 是否为详情页模式（影响样式）
 * @param isRead 是否已读（影响透明度）
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedTags(
    modifier: Modifier = Modifier,
    feed: Feed,
    maxTags: Int = 3,
    isDetail: Boolean = false,
    isRead: Boolean = false,
) {
    val displayTags = remember(feed.labels.tags) {
        feed.labels.tags?.splitTags(maxTags) ?: emptyList()
    }

    if (displayTags.isNotEmpty()) {
        val spacing = if (isDetail) 6.dp else 4.dp
        val verticalSpacing = if (isDetail) 4.dp else 3.dp
        val cornerRadius = if (isDetail) 8.dp else 6.dp
        val horizontalPadding = if (isDetail) 10.dp else 6.dp
        val verticalPadding = if (isDetail) 4.dp else 2.dp

        Spacer(modifier = Modifier.height(if (isDetail) 12.dp else 6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            modifier = modifier
        ) {
            displayTags.forEachIndexed { index, tag ->
                // 根据标签内容生成颜色
                val (backgroundColor, borderColor, textColor) = tag.generateTagColors()

                Box(
                    modifier = Modifier
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        .border(
                            width = 0.5.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                        .graphicsLayer(
                            alpha = 0.99f,
                            renderEffect = null
                        )
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = getTagFontSize(isDetail = isDetail),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = textColor.withReadTagAlpha(isRead),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}