package com.ddyy.zenfeed.ui.feeds.components.detail

import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.ddyy.zenfeed.extension.getThemeBackgroundColor
import com.ddyy.zenfeed.extension.toThemedHtml

@Composable
fun HtmlText(
    html: String, 
    modifier: Modifier = Modifier,
    onTableClick: (String) -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    // 生成增强的HTML，为表格添加放大按钮
    val enhancedHtml = remember(html, isDarkTheme) {
        addTableZoomButtons(html, isDarkTheme, onTableClick)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // 启用JavaScript支持，以便可以动态设置主题
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                // 根据系统主题设置WebView背景色
                setBackgroundColor(getThemeBackgroundColor(isDarkTheme))

                // 根据主题调整HTML内容
                val themedHtml = enhancedHtml.toThemedHtml(isDarkTheme)

                loadDataWithBaseURL(null, themedHtml, "text/html", "UTF-8", null)
                
                // 添加JavaScript接口处理表格点击
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onTableClick(tableHtml: String) {
                        onTableClick(tableHtml)
                    }
                }, "Android")
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

private fun addTableZoomButtons(html: String, isDarkTheme: Boolean, onTableClick: (String) -> Unit): String {
    // 使用正则表达式查找表格并添加放大按钮
    val buttonColor = if (isDarkTheme) "#4A90E2" else "#2196F3"
    val buttonHoverColor = if (isDarkTheme) "#5BA0F2" else "#1976D2"
    
    return html.replace(
        regex = Regex("<table[^>]*>(.*?)</table>", RegexOption.DOT_MATCHES_ALL)
    ) { matchResult ->
        val tableContent = matchResult.groupValues[1]
        val fullTable = matchResult.value
        
        """
        <div style="position: relative; margin: 16px 0;">
            <button 
                onclick="
                    var tableHtml = this.parentElement.querySelector('table').outerHTML;
                    Android.onTableClick(tableHtml);
                "
                style="
                    position: absolute;
                    top: 8px;
                    right: 8px;
                    background-color: $buttonColor;
                    color: white;
                    border: none;
                    border-radius: 4px;
                    width: 32px;
                    height: 32px;
                    cursor: pointer;
                    z-index: 1000;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    transition: background-color 0.2s;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                "
                onmouseover="this.style.backgroundColor='$buttonHoverColor'"
                onmouseout="this.style.backgroundColor='$buttonColor'"
            >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="white" stroke-width="1.5">
                    <path d="M2 2 L2 5 M2 2 L5 2" />
                    <path d="M14 2 L11 2 M14 2 L14 5" />
                    <path d="M14 14 L14 11 M14 14 L11 14" />
                    <path d="M2 14 L5 14 M2 14 L2 11" />
                </svg>
            </button>
            $fullTable
        </div>
        """.trimIndent().replace(Regex("\\s+"), " ").trim()
    }
}