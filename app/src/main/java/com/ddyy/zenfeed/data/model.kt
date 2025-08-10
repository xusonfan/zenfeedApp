package com.ddyy.zenfeed.data

import com.google.gson.annotations.SerializedName

data class FeedResponse(
    @SerializedName("feeds")
    val feeds: List<Feed>
)

data class Feed(
    @SerializedName("labels")
    val labels: Labels,
    @SerializedName("time")
    val time: String
)

data class FeedRequest(
    @SerializedName("start")
    val start: String,
    @SerializedName("end")
    val end: String,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("query")
    val query: String,
    @SerializedName("summarize")
    val summarize: Boolean
)

data class Labels(
    @SerializedName("category")
    val category: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("link")
    val link: String,
    @SerializedName("podcast_url")
    val podcastUrl: String,
    @SerializedName("pub_time")
    val pubTime: String,
    @SerializedName("source")
    val source: String,
    @SerializedName("summary")
    val summary: String,
    @SerializedName("summary_html_snippet")
    val summaryHtmlSnippet: String,
    @SerializedName("tags")
    val tags: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("type")
    val type: String
)