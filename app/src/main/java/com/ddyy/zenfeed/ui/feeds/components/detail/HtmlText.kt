package com.ddyy.zenfeed.ui.feeds.components.detail

import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.ddyy.zenfeed.extension.getThemeBackgroundColor
import com.ddyy.zenfeed.extension.toThemedHtml

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val isDarkTheme = isSystemInDarkTheme()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // 启用JavaScript支持，以便可以动态设置主题
                settings.javaScriptEnabled = true

                // 根据系统主题设置WebView背景色
                setBackgroundColor(getThemeBackgroundColor(isDarkTheme))

                // 根据主题调整HTML内容
                val themedHtml = html.toThemedHtml(isDarkTheme)

                loadDataWithBaseURL(null, themedHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // 当主题变化时更新WebView
            webView.setBackgroundColor(
                if (isDarkTheme)
                    "#1E1E1E".toColorInt()
                else
                    "#FFFFFF".toColorInt()
            )
        }
    )
}