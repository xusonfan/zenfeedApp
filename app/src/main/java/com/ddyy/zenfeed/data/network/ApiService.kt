package com.ddyy.zenfeed.data.network

import com.ddyy.zenfeed.data.FeedRequest
import com.ddyy.zenfeed.data.FeedResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/query")
    suspend fun getFeeds(
        @Query("backendUrl") backendUrl: String,
        @Body body: FeedRequest
    ): FeedResponse
}