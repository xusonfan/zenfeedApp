package com.ddyy.zenfeed.data.network

import com.ddyy.zenfeed.data.FeedRequest
import com.ddyy.zenfeed.data.FeedResponse
import com.ddyy.zenfeed.data.model.GithubRelease
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @POST("api/query")
    suspend fun getFeeds(
        @Query("backendUrl") backendUrl: String,
        @Body body: FeedRequest
    ): FeedResponse

    // 获取 GitHub 最新 Release
    @GET
    suspend fun getLatestRelease(@Url url: String): GithubRelease
}