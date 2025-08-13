package com.ddyy.zenfeed.ui.feeds.components.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryTabs(
    pagerState: PagerState,
    categories: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onTabDoubleClick: (Int) -> Unit = {},
    onTimeRangeSelected: (Int) -> Unit,
    selectedTimeRangeHours: Int
) {
    // 使用remember监听categories变化，确保tab栏与数据同步
    val allCategories = remember(categories) { listOf("全部") + categories }

    // 双击检测状态 - 为每个tab维护独立的双击状态
    var lastClickTimes by remember { mutableStateOf(emptyMap<Int, Long>()) }
    val doubleTapThreshold = 300L // 双击时间间隔阈值（毫秒）

    // 时间范围选择菜单状态
    var timeMenuExpanded by remember { mutableStateOf(false) }

    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = modifier,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (pagerState.currentPage < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        allCategories.forEachIndexed { index, category ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    val lastTime = lastClickTimes[index] ?: 0L

                    if (currentTime - lastTime <= doubleTapThreshold) {
                        // 双击事件：滚动到列表顶部
                        onTabDoubleClick(index)
                        lastClickTimes = lastClickTimes + (index to 0L) // 重置时间避免三击
                    } else {
                        // 单击事件：切换tab
                        onTabSelected(index)
                        lastClickTimes = lastClickTimes + (index to currentTime)
                    }
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (category == "全部") {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "选择时间范围",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { timeMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = timeMenuExpanded,
                                    onDismissRequest = { timeMenuExpanded = false }
                                ) {
                                    val timeRanges = listOf(
                                        "12小时内" to 12,
                                        "一天内" to 24,
                                        "三天内" to 72,
                                        "一周内" to 168,
                                        "一个月内" to 720
                                    )
                                    timeRanges.forEach { (text, hours) ->
                                        val isSelected = selectedTimeRangeHours == hours
                                        DropdownMenuItem(
                                            text = { Text(text) },
                                            onClick = {
                                                onTimeRangeSelected(hours)
                                                timeMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}