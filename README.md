# Zenfeed

Zenfeed 是一款现代化的 Android RSS 摘要阅读器，旨在提供一个简洁、美观且功能丰富的资讯浏览体验。用户可以通过一个信息流轻松获取来自不同来源的摘要，并能收听摘要附带的播客内容。

## ✨ 功能特性

*   **信息流列表**: 以卡片式布局清晰地展示来自 `https://zenfeed.xyz/` 的摘要列表。
*   **详情页浏览**: 点击任何摘要卡片，即可进入详情页阅读完整的HTML内容。
*   **播客后台播放**: 在详情页，可以播放在后台播放摘要附带的播客音频。
*   **通知栏控制**: 应用进入后台后，可以通过系统通知栏控制播客的播放、暂停、上一首和下一首。
*   **美观的UI**: 采用 Material 3 设计语言，界面简洁、现代。

## 🛠️ 技术栈

*   **语言**: [Kotlin](https://kotlinlang.org/)
*   **UI框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **架构**: MVVM (Model-View-ViewModel)
*   **网络请求**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
*   **JSON解析**: [Gson](https://github.com/google/gson)
*   **导航**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
*   **后台播放**: `Service` 和 `MediaSession`
*   **异步处理**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## 🚀 如何运行

1.  克隆本仓库:
    ```bash
    git clone https://your-repository-url/zenfeed.git
    ```
2.  使用 Android Studio 打开项目。
3.  等待 Gradle 同步完成。
4.  连接您的 Android 设备或启动一个模拟器。
5.  点击 "Run" 按钮来构建和运行应用。

## 🔮 未来展望

*   **全局迷你播放器**: 在应用的任何地方都能看到和控制当前播放的播客。
*   **收藏功能**: 允许用户收藏喜欢的摘要。
*   **主题切换**: 支持浅色和深色主题的切换。
*   **设置页面**: 提供更多自定义选项，例如刷新频率、通知设置等。

---

感谢您的使用！