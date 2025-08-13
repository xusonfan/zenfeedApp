package com.ddyy.zenfeed.extension

import androidx.compose.ui.graphics.Color

/**
 * String扩展函数 - 提供HTML处理、标签处理等工具方法
 */

/**
 * 清理HTML标签，转换为纯文本
 */
fun String.cleanHtmlTags(): String {
    return if (this.contains('<')) {
        this.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    } else {
        this.trim()
    }
}

/**
 * 分割标签字符串为列表
 * 支持多种分隔符：逗号（中英文）、分号（中英文）
 */
fun String.splitTags(): List<String> {
    return this.takeIf { it.isNotBlank() }
        ?.split(",", "，", ";", "；")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}

/**
 * 限制标签数量并返回列表
 */
fun String.splitTags(maxCount: Int = 3): List<String> {
    return this.splitTags().take(maxCount)
}

/**
 * 获取安全的标题，如果为空则返回默认值
 */
fun String?.orDefaultTitle(): String {
    return this ?: "未知标题"
}

/**
 * 获取安全的来源，如果为空则返回默认值
 */
fun String?.orDefaultSource(): String {
    return this ?: "未知来源"
}

/**
 * 生成主题化的HTML内容，适配深色/浅色模式
 */
fun String.toThemedHtml(isDarkTheme: Boolean): String {
    return if (isDarkTheme) {
        """
        <html>
        <head>
            <style>
                body {
                    background-color: #1E1E1E !important;
                    color: #E0E0E0 !important;
                    font-family: sans-serif;
                    line-height: 1.6;
                    margin: 0;
                    padding: 12px;
                }
                
                /* 链接样式 */
                a {
                    color: #BB86FC !important;
                    text-decoration: underline;
                }
                a:visited {
                    color: #CE93D8 !important;
                }
                
                /* 图片样式 */
                img {
                    max-width: 100%;
                    height: auto;
                    border-radius: 8px;
                    margin: 8px 0;
                }
                
                /* 标题样式 */
                h1, h2, h3, h4, h5, h6 {
                    color: #FFFFFF !important;
                    margin: 16px 0 8px 0;
                }
                
                /* 段落样式 */
                p {
                    color: #E0E0E0 !important;
                    margin: 8px 0;
                    line-height: 1.6;
                }
                
                /* 列表样式 */
                ul, ol {
                    color: #E0E0E0 !important;
                    margin: 8px 0;
                    padding-left: 20px;
                }
                li {
                    color: #E0E0E0 !important;
                    margin: 4px 0;
                }
                
                /* 代码样式 */
                code {
                    background-color: #2D2D2D !important;
                    color: #F8F8F2 !important;
                    padding: 2px 4px;
                    border-radius: 4px;
                    font-family: monospace;
                }
                pre {
                    background-color: #2D2D2D !important;
                    color: #F8F8F2 !important;
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                    margin: 12px 0;
                }
                
                /* 引用样式 */
                blockquote {
                    background-color: #2D2D2D !important;
                    color: #E0E0E0 !important;
                    border-left: 4px solid #BB86FC;
                    margin: 12px 0;
                    padding: 12px 16px;
                    border-radius: 0 8px 8px 0;
                }
                
                /* 表格样式 */
                table {
                    background-color: #2D2D2D !important;
                    color: #E0E0E0 !important;
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                    border-radius: 8px;
                    overflow: hidden;
                    border: 1px solid #444444;
                }
                th, td {
                    color: #E0E0E0 !important;
                    border: 1px solid #444444;
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background-color: #3A3A3A !important;
                    font-weight: bold;
                    color: #FFFFFF !important;
                }
                tr:nth-child(even) {
                    background-color: #252525 !important;
                }
                
                /* 分割线样式 */
                hr {
                    border: none;
                    height: 1px;
                    background-color: #444444;
                    margin: 16px 0;
                }
                
                /* 强调文本样式 */
                strong, b {
                    color: #FFFFFF !important;
                    font-weight: bold;
                }
                em, i {
                    color: #E0E0E0 !important;
                    font-style: italic;
                }
                
                /* 通用容器样式 - 不破坏布局 */
                div {
                    color: #E0E0E0 !important;
                }
                span {
                    color: inherit !important;
                }
                
                /* 清除可能破坏布局的样式 */
                * {
                    text-shadow: none !important;
                    box-shadow: none !important;
                }
                
                /* 处理可能存在的白色背景 */
                [style*="background-color: white"],
                [style*="background-color: #fff"],
                [style*="background-color: #ffffff"],
                [style*="background: white"],
                [style*="background: #fff"],
                [style*="background: #ffffff"] {
                    background-color: #2D2D2D !important;
                }
                
                /* 处理可能存在的黑色文字 */
                [style*="color: black"],
                [style*="color: #000"],
                [style*="color: #000000"] {
                    color: #E0E0E0 !important;
                }
            </style>
        </head>
        <body>
            ${this}
        </body>
        </html>
        """
    } else {
        """
        <html>
        <head>
            <style>
                body {
                    background-color: #FFFFFF;
                    color: #000000;
                    font-family: sans-serif;
                    line-height: 1.6;
                }
                a {
                    color: #6650a4;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                }
                th, td {
                    border: 1px solid #dddddd;
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background-color: #f2f2f2;
                    font-weight: bold;
                }
                tr:nth-child(even) {
                    background-color: #f9f9f9;
                }
                tr:hover {
                    background-color: #f5f5f5;
                }
            </style>
        </head>
        <body>
            ${this}
        </body>
        </html>
        """
    }
}

/**
 * 获取显示内容，优先使用HTML片段，否则使用摘要
 */
fun String?.getDisplayContent(summary: String?): String {
    val content = this?.takeIf { it.isNotBlank() }
        ?: summary?.takeIf { it.isNotBlank() }
        ?: ""
    
    return content.cleanHtmlTags()
}

/**
 * 根据是否已读调整透明度
 */
fun Color.withReadAlpha(isRead: Boolean): Color {
    return this.copy(alpha = if (isRead) 0.6f else 1.0f)
}

/**
 * 根据是否已读调整透明度（摘要用）
 */
fun Color.withReadSummaryAlpha(isRead: Boolean): Color {
    return this.copy(alpha = if (isRead) 0.5f else 1.0f)
}

/**
 * 根据是否已读调整透明度（标签用）
 */
fun Color.withReadTagAlpha(isRead: Boolean): Color {
    return this.copy(alpha = if (isRead) 0.6f else 0.8f)
}