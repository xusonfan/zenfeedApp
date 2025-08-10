package com.ddyy.zenfeed.ui.feeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedItem(feed: Feed, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // 紧凑型现代化卡片设计
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 7.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        interactionSource = interactionSource
    ) {
        // 渐变背景层
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Column {
                // 顶部来源信息区域 - 紧凑设计
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.03f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 紧凑图标容器
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Article,
                                contentDescription = "来源图标",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = feed.labels.source,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 主要内容区域 - 紧凑布局
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // 标题 - 紧凑排版
                    Text(
                        text = feed.labels.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // 动态显示摘要内容 - 优先使用 summaryHtmlSnippet，其次使用 summary
                    val displayContent = if (feed.labels.summaryHtmlSnippet.isNotBlank()) {
                        // 简单清理 HTML 标签并处理常见 HTML 实体
                        feed.labels.summaryHtmlSnippet
                            .replace(Regex("<[^>]*>"), "")
                            .replace("&nbsp;", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .trim()
                    } else {
                        feed.labels.summary.trim()
                    }
                    
                    // 只在有实际内容时显示摘要和间距
                    if (displayContent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = displayContent,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 18.sp,
                                letterSpacing = 0.1.sp
                            ),
                            maxLines = 4, // 增加到4行以显示更多内容
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 底部装饰线 - 小尺寸
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )
                    } else {
                        // 如果没有摘要内容，只显示装饰线，减少间距
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedsList(feeds: List<Feed>, onFeedClick: (Feed) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 180.dp), // 优化最小尺寸
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(feeds) { feed ->
            // 添加进入动画
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = FastOutSlowInEasing
                    )
                ) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                FeedItem(feed = feed, onClick = { onFeedClick(feed) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreen(
    feedsUiState: FeedsUiState,
    onFeedClick: (Feed) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 现代化的顶部应用栏
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Zenfeed",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (feedsUiState) {
            is FeedsUiState.Loading -> ModernLoadingScreen(Modifier.padding(innerPadding))
            is FeedsUiState.Success -> FeedsList(
                feeds = feedsUiState.feeds,
                onFeedClick = onFeedClick,
                modifier = Modifier.padding(innerPadding)
            )
            is FeedsUiState.Error -> ModernErrorScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun ModernLoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 现代化加载动画
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "精彩内容加载中...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请稍候片刻",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ModernErrorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 现代化错误图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "错误图标",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "内容加载失败",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请检查网络连接后重试",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 现代化重试按钮
        FilledTonalButton(
            onClick = { /* TODO: 实现重试逻辑 */ },
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = 120.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "重新加载",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// 保持原有的预览组件，但使用新的设计
@Preview(showBackground = true)
@Composable
fun FeedItemPreview() {
    MaterialTheme {
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
                    tags = "",
                    type = ""
                ),
                time = "2023-10-27T12:00:00Z"
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FeedsScreenSuccessPreview() {
    MaterialTheme {
        FeedsScreen(
            feedsUiState = FeedsUiState.Success(
                List(8) {
                    Feed(
                        labels = Labels(
                            title = "现代化标题 $it - 展示新的设计风格",
                            summary = "这是第 $it 条内容的现代化摘要信息，采用了全新的视觉设计和排版风格，提供更好的用户体验。",
                            source = "精选来源 $it",
                            category = "",
                            content = "",
                            link = "",
                            podcastUrl = "",
                            pubTime = "",
                            summaryHtmlSnippet = "",
                            tags = "",
                            type = ""
                        ),
                        time = "2023-10-27T12:00:00Z"
                    )
                }
            ),
            onFeedClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ModernLoadingScreenPreview() {
    MaterialTheme {
        ModernLoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ModernErrorScreenPreview() {
    MaterialTheme {
        ModernErrorScreen()
    }
}