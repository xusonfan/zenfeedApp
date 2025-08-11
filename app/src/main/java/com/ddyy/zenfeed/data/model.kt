package com.ddyy.zenfeed.data

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
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
                val date = inputFormat.parse(time.replace("Z", "")) ?: return time
                
                val now = Date()
                val diffInMillis = now.time - date.time
                val diffInMinutes = diffInMillis / (1000 * 60)
                val diffInHours = diffInMillis / (1000 * 60 * 60)
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                
                // 获取当前日期和目标日期的日历对象
                val nowCalendar = Calendar.getInstance()
                val dateCalendar = Calendar.getInstance().apply { time = date }
                
                when {
                    // 刚刚 (1分钟内)
                    diffInMinutes < 1 -> "刚刚"
                    
                    // 几分钟前 (1小时内)
                    diffInMinutes < 60 -> "${diffInMinutes.toInt()}分钟前"
                    
                    // 几小时前 (今天内)
                    diffInHours < 24 && nowCalendar.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
                        && nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) -> {
                        "${diffInHours.toInt()}小时前"
                    }
                    
                    // 今天 (显示具体时间)
                    nowCalendar.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
                        && nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) -> {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        "今天 ${timeFormat.format(date)}"
                    }
                    
                    // 昨天
                    diffInDays.toInt() == 1 || (nowCalendar.get(Calendar.DAY_OF_YEAR) - dateCalendar.get(Calendar.DAY_OF_YEAR) == 1
                        && nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR)) -> {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        "昨天 ${timeFormat.format(date)}"
                    }
                    
                    // 本年内的其他日期
                    nowCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) -> {
                        val outputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                        outputFormat.format(date)
                    }
                    
                    // 更早以前 (显示完整日期)
                    else -> {
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        outputFormat.format(date)
                    }
                }
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