package com.ddyy.zenfeed.ui.feeds.components.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels
import com.ddyy.zenfeed.ui.feeds.components.common.FeedItem

@Preview(showBackground = true)
@Composable
fun FeedItemPreview() {
    MaterialTheme {
        Column {
            // 未读文章示例
            FeedItem(
                feed = Feed(
                    labels = Labels(
                        title = "这是一个现代化的示例标题，它展示了全新的设计风格和视觉效果",
                        summary = "这是一个更加精美的摘要展示，采用了现代化的排版和间距设计，提供更好的阅读体验。新的设计包含了渐变背景、圆角卡片和优雅的动画效果，让整个界面看起来更加专业和吸引人。",
                        source = "现代化来源",
                        category = "",
                        content = "",
                        link = "",
                        podcastUrl = "",
                        pubTime = "",
                        summaryHtmlSnippet = "",
                        tags = "新闻,时事",
                        type = ""
                    ),
                    time = "2023-10-27T12:00:00Z",
                    isRead = false
                ),
                onClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 已读文章示例（淡化效果）
            FeedItem(
                feed = Feed(
                    labels = Labels(
                        title = "这是一篇已读文章的标题，显示淡化效果",
                        summary = "这是已读文章的摘要，文字会显示为淡化状态，便于用户区分已读和未读内容。",
                        source = "示例来源",
                        category = "",
                        content = "",
                        link = "",
                        podcastUrl = "",
                        pubTime = "",
                        summaryHtmlSnippet = "",
                        tags = "新闻,时事",
                        type = ""
                    ),
                    time = "2023-10-27T11:00:00Z",
                    isRead = true
                ),
                onClick = {}
            )
        }
    }
}