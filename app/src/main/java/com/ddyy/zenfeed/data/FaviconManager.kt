package com.ddyy.zenfeed.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.ddyy.zenfeed.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.ConcurrentHashMap

/**
 * 网站图标获取和缓存管理器
 * 负责获取网站的favicon图标并提供缓存功能
 */
class FaviconManager(private val context: Context) {
    
    // 内存缓存，用于存储已获取的图标
    private val memoryCache = ConcurrentHashMap<String, Bitmap>()
    
    // 正在加载的URL集合，防止重复请求
    private val loadingUrls = ConcurrentHashMap<String, Boolean>()
    
    // 互斥锁，用于保护缓存操作
    private val cacheMutex = Mutex()
    
    // 缓存目录
    private val cacheDir: File by lazy {
        File(context.cacheDir, "favicons").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    // OkHttpClient 用于网络请求，支持代理
    private lateinit var httpClient: OkHttpClient
    private val clientMutex = Mutex()

    private suspend fun ensureHttpClient() {
        if (::httpClient.isInitialized) return
        clientMutex.withLock {
            if (!::httpClient.isInitialized) {
                httpClient = ApiClient.getHttpClient(context)
            }
        }
    }
    
    /**
     * 获取网站图标
     * @param url 网站URL
     * @return Bitmap 图标位图，获取失败返回null
     */
    suspend fun getFavicon(url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrEmpty()) {
            return@withContext null
        }
        
        val domain = extractDomain(url) ?: return@withContext null

        ensureHttpClient()
        
        // 检查是否正在加载
        if (loadingUrls.containsKey(domain)) {
            Log.d("FaviconManager", "图标正在加载中，跳过重复请求: $domain")
            return@withContext null
        }
        
        return@withContext cacheMutex.withLock {
            // 1. 检查内存缓存
            memoryCache[domain]?.let { cachedBitmap ->
                Log.d("FaviconManager", "从内存缓存获取图标: $domain")
                return@withLock cachedBitmap
            }
            
            // 2. 检查磁盘缓存
            val cacheFile = File(cacheDir, "${domain.hashCode()}.png")
            if (cacheFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    if (bitmap != null) {
                        memoryCache[domain] = bitmap
                        Log.d("FaviconManager", "从磁盘缓存获取图标: $domain")
                        return@withLock bitmap
                    }
                } catch (e: Exception) {
                    Log.e("FaviconManager", "从磁盘缓存读取图标失败: $domain", e)
                }
            }
            
            // 标记为正在加载
            loadingUrls[domain] = true
            
            try {
                // 3. 网络获取
                val faviconUrl = getFaviconUrl(url)
                if (faviconUrl != null) {
                    try {
                        val bitmap = downloadFavicon(faviconUrl)
                        if (bitmap != null) {
                            // 缓存到内存
                            memoryCache[domain] = bitmap
                            
                            // 缓存到磁盘
                            try {
                                FileOutputStream(cacheFile).use { outputStream ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                }
                                Log.d("FaviconManager", "图标已缓存到磁盘: $domain")
                            } catch (e: Exception) {
                                Log.e("FaviconManager", "保存图标到磁盘失败: $domain", e)
                            }
                            
                            return@withLock bitmap
                        }
                    } catch (e: Exception) {
                        Log.e("FaviconManager", "下载图标失败: $faviconUrl", e)
                    }
                }
                
                Log.w("FaviconManager", "无法获取图标: $domain")
                return@withLock null
            } finally {
                // 移除加载标记
                loadingUrls.remove(domain)
            }
        }
    }
    
    /**
     * 从URL中提取域名
     */
    private fun extractDomain(url: String): String? {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return null
            
            // 移除www.前缀
            if (host.startsWith("www.")) {
                host.substring(4)
            } else {
                host
            }
        } catch (e: URISyntaxException) {
            Log.e("FaviconManager", "解析URL失败: $url", e)
            null
        }
    }
    
    /**
     * 获取favicon的URL
     * 使用Google favicon服务
     */
    private fun getFaviconUrl(url: String): String? {
        val domain = extractDomain(url) ?: return null
        
        // 使用Google favicon服务
        return "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://$domain&size=64"
    }
    
    /**
     * 下载favicon
     */
    private suspend fun downloadFavicon(faviconUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(faviconUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val inputStream = response.body.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap != null) {
                    Log.d("FaviconManager", "成功下载图标: $faviconUrl")
                    return@withContext bitmap
                }
            }
        } catch (e: IOException) {
            Log.e("FaviconManager", "下载图标时发生IO异常: $faviconUrl", e)
        } catch (e: Exception) {
            Log.e("FaviconManager", "下载图标时发生异常: $faviconUrl", e)
        }
        
        return@withContext null
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        // 清除内存缓存
        memoryCache.clear()
        
        // 清除磁盘缓存
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.e("FaviconManager", "删除缓存文件失败: ${file.name}", e)
                }
            }
        }
        
        Log.d("FaviconManager", "缓存已清除")
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { file -> file.length() } ?: 0L
    }
    
    /**
     * 获取缓存文件数量
     */
    fun getCacheFileCount(): Int {
        return cacheDir.listFiles()?.size ?: 0
    }
    
    /**
     * 批量获取网站图标
     * @param urls 网站URL列表
     * @return Map<域名, Bitmap> 成功获取的图标映射
     */
    suspend fun getFavicons(urls: List<String>): Map<String, Bitmap> = coroutineScope {
        val domains = urls.mapNotNull { extractDomain(it) }.distinct()
        val results = mutableMapOf<String, Bitmap>()
        
        // 并发获取图标
        val deferreds = domains.map { domain ->
            async {
                val favicon = getFavicon("https://$domain")
                if (favicon != null) {
                    domain to favicon
                } else {
                    null
                }
            }
        }
        
        // 等待所有请求完成
        deferreds.awaitAll().filterNotNull().toMap(results)
    }
    
    /**
     * 清理过期缓存（超过30天的文件）
     */
    fun cleanExpiredCache() {
        val thirtyDaysInMs = 30L * 24 * 60 * 60 * 1000
        val currentTime = System.currentTimeMillis()
        
        cacheDir.listFiles()?.forEach { file ->
            if (currentTime - file.lastModified() > thirtyDaysInMs) {
                try {
                    if (file.delete()) {
                        Log.d("FaviconManager", "删除过期缓存文件: ${file.name}")
                    }
                } catch (e: Exception) {
                    Log.e("FaviconManager", "删除过期缓存文件失败: ${file.name}", e)
                }
            }
        }
    }
    
}