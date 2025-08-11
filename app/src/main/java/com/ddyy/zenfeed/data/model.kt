package com.ddyy.zenfeed.data

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FeedResponse(
    @SerializedName("feeds")
    val feeds: List<Feed>
)

data class Feed(
    @SerializedName("labels")
    val labels: Labels,
    @SerializedName("time")
    val time: String,
    // 阅读状态，默认为未读
    val isRead: Boolean = false
) {
    val formattedTime: String
        get() {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(time.replace("Z", ""))
                val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                outputFormat.format(date as Date)
            } catch (e: Exception) {
                time
            }
        }

    val formattedTimeShort: String
        get() {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(time.replace("Z", ""))
                val outputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                outputFormat.format(date as Date)
            } catch (e: Exception) {
                time
            }
        }
}

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
    val category: String?,
    @SerializedName("content")
    val content: String?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("podcast_url")
    val podcastUrl: String?,
    @SerializedName("pub_time")
    val pubTime: String?,
    @SerializedName("source")
    val source: String?,
    @SerializedName("summary")
    val summary: String?,
    @SerializedName("summary_html_snippet")
    val summaryHtmlSnippet: String?,
    @SerializedName("tags")
    val tags: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("type")
    val type: String?
)

/**
 * 播放列表信息
 */
data class PlaylistInfo(
    val currentIndex: Int,
    val totalCount: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val isRepeat: Boolean,
    val isShuffle: Boolean
)