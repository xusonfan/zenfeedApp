package com.ddyy.zenfeed.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用于解析 GitHub API "latest release" 响应的数据类。
 * 我们只定义了我们关心的字段。
 */
data class GithubRelease(
    @SerializedName("tag_name")
    val tagName: String, // 例如 "v1.0.1"

    @SerializedName("name")
    val name: String, // 例如 "Release v1.0.1"

    @SerializedName("body")
    val body: String, // 更新日志

    @SerializedName("assets")
    val assets: List<Asset>
) {
    data class Asset(
        @SerializedName("browser_download_url")
        val browserDownloadUrl: String, // APK 的下载链接

        @SerializedName("name")
        val name: String, // 文件名，例如 "app-release.apk"

        @SerializedName("size")
        val size: Long // 文件大小（字节）
    )
}