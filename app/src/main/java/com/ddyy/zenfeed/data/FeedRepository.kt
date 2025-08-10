package com.ddyy.zenfeed.data

import android.content.Context
import com.ddyy.zenfeed.data.network.ApiClient
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Feed数据仓库
 * 负责从API获取Feed数据，支持动态配置API地址和后端URL
 */
class FeedRepository(private val context: Context) {
    
    private val settingsDataStore = SettingsDataStore(context)

    /**
     * 获取Feed列表
     * @return Feed响应结果
     */
    suspend fun getFeeds(): Result<FeedResponse> {
        return try {
            val now = Date()
            val start = Date(now.time - 24 * 60 * 60 * 1000) // 24 hours ago
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val requestBody = FeedRequest(
                start = dateFormat.format(start),
                end = dateFormat.format(now),
                limit = 500,
                query = "",
                summarize = false
            )
            
            // 使用动态API服务和后端URL
            val apiService = ApiClient.getApiService(context)
            val backendUrl = settingsDataStore.backendUrl.first()
            val response = apiService.getFeeds(
                backendUrl = backendUrl,
                body = requestBody
            )
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "获取摘要失败", e)
            Result.failure(e)
        }
    }
}