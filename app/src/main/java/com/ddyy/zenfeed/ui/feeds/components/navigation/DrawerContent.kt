package com.ddyy.zenfeed.ui.feeds.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddyy.zenfeed.BuildConfig
import com.ddyy.zenfeed.extension.getProxyStatusDescription
import com.ddyy.zenfeed.extension.getThemeModeDescription
import com.ddyy.zenfeed.extension.getThemeModeIcon
import com.ddyy.zenfeed.ui.feeds.components.common.MenuItemCard

/**
 * 抽屉菜单内容组件
 */
@Composable
fun DrawerContent(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLoggingClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    currentThemeMode: String = "system",
    onThemeToggle: () -> Unit = {},
    isProxyEnabled: Boolean = false,
    onProxyToggle: () -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = modifier.widthIn(max = 280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // 抽屉头部 - 扩展到状态栏区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 增加高度以覆盖状态栏区域
                .background(
                    MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp) // 为状态栏留出空间
            ) {
                Text(
                    text = "Zenfeed",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "精选资讯阅读",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 菜单项分组
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 主题切换菜单项
            MenuItemCard(
                icon = getThemeModeIcon(currentThemeMode),
                title = "主题模式",
                subtitle = getThemeModeDescription(currentThemeMode),
                onClick = onThemeToggle
            )

            // 代理切换菜单项
            MenuItemCard(
                icon = Icons.Default.Security,
                title = "代理设置",
                subtitle = getProxyStatusDescription(isProxyEnabled),
                onClick = onProxyToggle
            )

            // 日志记录菜单项
            MenuItemCard(
                icon = Icons.Default.BugReport,
                title = "日志记录",
                subtitle = "记录应用日志，排查问题",
                onClick = onLoggingClick
            )

            // 设置菜单项
            MenuItemCard(
                icon = Icons.Default.Settings,
                title = "设置",
                subtitle = "应用设置和配置",
                onClick = onSettingsClick
            )

            // 关于菜单项
            MenuItemCard(
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "应用信息和版本详情",
                onClick = onAboutClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 底部信息
        Text(
            text = "版本 ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}