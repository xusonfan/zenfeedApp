package com.ddyy.zenfeed.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 自定义请求日志拦截器
 * 只记录请求信息，不记录响应内容
 */
class RequestLoggingInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "RequestLogging"
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // 记录请求方法、URL和头部
        val requestBuilder = StringBuilder()
        requestBuilder.append("--> ${request.method} ${request.url}\n")
        
        // 记录请求头部
        val headers = request.headers
        for (i in 0 until headers.size) {
            requestBuilder.append("${headers.name(i)}: ${headers.value(i)}\n")
        }
        
        // 记录请求体
        val requestBody = request.body
        if (requestBody != null) {
            val contentType = requestBody.contentType()
            if (contentType != null) {
                requestBuilder.append("Content-Type: $contentType\n")
            }
            
            val contentLength = requestBody.contentLength()
            if (contentLength != -1L) {
                requestBuilder.append("Content-Length: $contentLength\n")
            }
            
            requestBuilder.append("\n")
            
            // 读取请求体内容
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            
            // 检查是否是文本内容
            if (isTextContent(contentType)) {
                requestBuilder.append(buffer.readString(charset))
                requestBuilder.append("\n")
            } else {
                requestBuilder.append("(二进制内容，${buffer.size} 字节)\n")
            }
        }
        
        requestBuilder.append("--> END ${request.method}")
        
        // 打印请求日志
        Log.d(TAG, requestBuilder.toString())
        
        // 继续请求链，不记录响应
        return chain.proceed(request)
    }
    
    /**
     * 检查是否是文本内容
     */
    private fun isTextContent(contentType: okhttp3.MediaType?): Boolean {
        if (contentType == null) return false
        
        val type = contentType.type
        val subtype = contentType.subtype
        
        return (type == "text" || 
                subtype == "json" || 
                subtype == "xml" || 
                subtype == "plain" || 
                subtype == "html" || 
                subtype.contains("javascript"))
    }
}