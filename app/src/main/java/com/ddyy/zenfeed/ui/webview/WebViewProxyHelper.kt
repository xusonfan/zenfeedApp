package com.ddyy.zenfeed.ui.webview

import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ddyy.zenfeed.data.network.ApiClient
import okhttp3.Request
import java.io.ByteArrayInputStream

/**
 * 支持代理的 WebViewClient
 * 拦截所有网络请求并通过应用的代理配置进行转发
 */
class ProxyWebViewClient(private val context: Context) : WebViewClient() {
    
    companion object {
        private const val TAG = "ProxyWebViewClient"
    }
    
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        request?.let { req ->
            try {
                val url = req.url.toString()
                Log.d(TAG, "拦截请求: $url")
                
                // 使用应用的 OkHttp 客户端（已配置代理）进行网络请求
                val httpClient = ApiClient.getHttpClient(context)
                
                // 构建 OkHttp 请求
                val requestBuilder = Request.Builder().url(url)
                
                // 复制请求头
                req.requestHeaders?.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }
                
                // 设置 User-Agent
                requestBuilder.addHeader("User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                
                val httpRequest = requestBuilder.build()
                
                // 执行网络请求
                try {
                    val response = httpClient.newCall(httpRequest).execute()
                    
                    if (response.isSuccessful) {
                        val contentType = response.header("Content-Type") ?: "text/html"
                        val encoding = extractEncoding(contentType)
                        val mimeType = extractMimeType(contentType)
                        
                        val responseHeaders = mutableMapOf<String, String>()
                        response.headers.forEach { (name, value) ->
                            responseHeaders[name] = value
                        }
                        
                        val inputStream = response.body?.byteStream()
                            ?: ByteArrayInputStream(ByteArray(0))
                        
                        Log.d(TAG, "代理请求成功: $url, 状态码: ${response.code}")
                        
                        return WebResourceResponse(mimeType, encoding, response.code, "OK", responseHeaders, inputStream)
                    } else {
                        Log.w(TAG, "代理请求失败: $url, 状态码: ${response.code}")
                        return WebResourceResponse("text/html", "utf-8", response.code, "Error", null,
                            ByteArrayInputStream("请求失败".toByteArray()))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "执行代理请求时发生错误: $url", e)
                    return WebResourceResponse("text/html", "utf-8", 500, "Error", null,
                        ByteArrayInputStream("网络错误".toByteArray()))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "处理拦截请求时发生错误", e)
            }
        }
        
        // 如果无法处理，返回 null 让 WebView 使用默认行为
        return super.shouldInterceptRequest(view, request)
    }
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "页面开始加载: $url")
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d(TAG, "页面加载完成: $url")
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // 在当前 WebView 中加载所有 URL
        return false
    }
    
    /**
     * 从 Content-Type 中提取 MIME 类型
     */
    private fun extractMimeType(contentType: String): String {
        return contentType.split(";")[0].trim()
    }
    
    /**
     * 从 Content-Type 中提取编码
     */
    private fun extractEncoding(contentType: String): String {
        val charsetIndex = contentType.indexOf("charset=")
        return if (charsetIndex != -1) {
            contentType.substring(charsetIndex + 8).split(";")[0].trim()
        } else {
            "utf-8"
        }
    }
}

/**
 * WebView 代理设置帮助类
 */
object WebViewProxyHelper {
    
    private const val TAG = "WebViewProxyHelper"

    /**
     * 创建支持代理的 WebViewClient
     * @param context 上下文
     * @return 配置了代理的 WebViewClient
     */
    fun createProxyWebViewClient(context: Context): WebViewClient {
        Log.d(TAG, "创建支持代理的 WebViewClient")
        return ProxyWebViewClient(context)
    }
}