package com.ddyy.zenfeed.data

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FeedResponse(
    @SerializedName("summary") val summary: String? = null, // LLM生成的内容摘要，当请求中summarize为true时返回
    @SerializedName("feeds") val feeds: List<Feed>, // 符合查询条件的Feed数组
    @SerializedName("count") val count: Int, // 返回的Feed数量
    val error: String? = null // 网络请求错误信息，用于显示toast提示
)

data class Feed(
    @SerializedName("labels") val labels: Labels, // Feed的元数据标签，包含类型、来源、标题等信息
    @SerializedName("time") val time: String, // Feed被系统记录或处理的时间戳(RFC3339格式)
    @SerializedName("score") val score: Float? = null, // 语义搜索的相关性得分，得分越高相关性越强
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
    @SerializedName("query") val query: String? = null, // 用于语义搜索的查询字符串，至少需要5个字符
    @SerializedName("threshold") val threshold: Float? = null, // 语义搜索的相关性阈值，范围[0,1]，默认0.55
    @SerializedName("label_filters") val labelFilters: List<String>? = null, // 标签过滤器数组，格式为"key=value"或"key!=value"
    @SerializedName("summarize") val summarize: Boolean? = null, // 是否对查询结果进行摘要，默认false
    @SerializedName("limit") val limit: Int? = null, // 返回Feed结果的最大数量，范围[1,500]，默认10
    @SerializedName("start") val start: String? = null, // 查询时间范围的开始时间(包含)，RFC3339格式，默认24小时前
    @SerializedName("end") val end: String? = null // 查询时间范围的结束时间(不包含)，RFC3339格式，默认当前时间
)

data class Labels(
    @SerializedName("category") val category: String? = null, // Feed的分类标签
    @SerializedName("content") val content: String? = null, // Feed的详细内容
    @SerializedName("link") val link: String? = null, // Feed的原始链接地址
    @SerializedName("podcast_url") val podcastUrl: String? = null, // 播客音频文件的URL地址
    @SerializedName("pub_time") val pubTime: String? = null, // Feed的发布时间
    @SerializedName("source") val source: String? = null, // Feed的来源名称
    @SerializedName("summary") val summary: String? = null, // Feed的摘要信息
    @SerializedName("summary_html_snippet") val summaryHtmlSnippet: String? = null, // Feed摘要的HTML片段
    @SerializedName("tags") val tags: String? = null, // Feed的标签信息
    @SerializedName("title") val title: String? = null, // Feed的标题
    @SerializedName("type") val type: String? = null // Feed的类型(如rss、github_release等)
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