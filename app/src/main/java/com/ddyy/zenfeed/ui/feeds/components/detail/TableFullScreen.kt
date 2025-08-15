package com.ddyy.zenfeed.ui.feeds.components.detail

import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ddyy.zenfeed.extension.getThemeBackgroundColor
import com.ddyy.zenfeed.extension.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableFullScreen(
    tableHtml: String,
    title: String,
    onBack: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    
    // 进入时设置为横屏
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            // 退出时恢复竖屏
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // 拦截系统返回手势
    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { keyEvent ->
            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP && 
                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                onBack()
                true
            } else {
                false
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("表格全屏显示") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    setBackgroundColor(getThemeBackgroundColor(isDarkTheme))
                    
                    // 为表格添加样式，使其更适合全屏显示
                    val styledHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body {
                                    margin: 0;
                                    padding: 16px;
                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                    background-color: ${if (isDarkTheme) "#1E1E1E" else "#FFFFFF"};
                                    color: ${if (isDarkTheme) "#E0E0E0" else "#000000"};
                                }
                                table {
                                    width: 100%;
                                    border-collapse: collapse;
                                    margin: 0;
                                }
                                th, td {
                                    border: 1px solid ${if (isDarkTheme) "#444444" else "#CCCCCC"};
                                    padding: 8px;
                                    text-align: left;
                                    word-wrap: break-word;
                                    min-width: 80px;
                                }
                                th {
                                    background-color: ${if (isDarkTheme) "#2D2D2D" else "#F5F5F5"};
                                    font-weight: bold;
                                }
                                img {
                                    max-width: 100%;
                                    height: auto;
                                }
                            </style>
                        </head>
                        <body>
                            $tableHtml
                        </body>
                        </html>
                    """.trimIndent()
                    
                    loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.setBackgroundColor(
                    if (isDarkTheme)
                        "#1E1E1E".toColorInt()
                    else
                        "#FFFFFF".toColorInt()
                )
            }
        )
    }
}