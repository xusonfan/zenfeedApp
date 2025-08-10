package com.ddyy.zenfeed.data

import com.ddyy.zenfeed.data.network.ApiClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class FeedRepository {

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
            val response = ApiClient.apiService.getFeeds(
                backendUrl = "http://zenfeed:1300",
                body = requestBody
            )
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "获取摘要失败", e)
            Result.failure(e)
        }
    }
}