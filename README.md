# Zenfeed

Zenfeed 是一款现代化的 Android RSS 摘要阅读器，旨在提供一个简洁、美观且功能丰富的资讯浏览体验。用户可以通过一个信息流轻松获取来自不同来源的摘要，并能收听摘要附带的播客内容。

## ✨ 功能特性

*   **信息流列表**: 以卡片式布局清晰地展示摘要列表，支持分类筛选和智能时间显示。
*   **详情页浏览**: 点击任何摘要卡片，即可进入详情页阅读完整的HTML内容，支持左右滑动切换文章。
*   **丰富的详情页操作**: 在文章详情页提供刷新、分享、跳转浏览器等多种便捷操作。
*   **播客后台播放**: 在详情页和列表页，可以播放摘要附带的播客音频，支持后台播放。
*   **增强的通知栏控制**: 优化通知栏媒体控件，支持播放、暂停、上/下一首，点击通知可直接跳转到对应文章。
*   **高级播放功能**: 支持播客列表播放、循环播放、乱序播放、倍速播放（切换时保留速率）、定时停止，以及智能预加载下一首。
*   **音频焦点管理**: 智能处理与其他应用的音频播放冲突，保证播放体验。
*   **文章搜索与筛选**: 支持关键词搜索和按时间范围筛选文章。
*   **表格全屏查看**: 支持将文章中的表格全屏查看，优化阅读体验。
*   **WebView 内置浏览器**: 支持在应用内打开链接，无需跳转到外部浏览器。
*   **代理支持**: 内置 HTTP 代理配置，支持用户名密码认证，确保网络访问畅通。
*   **个性化设置**: 支持自定义 API 服务器地址、主题切换（浅色/深色/跟随系统）、AI模型配置。
*   **阅读状态管理**: 自动标记已读文章，智能回到上次浏览位置。
*   **缓存管理**: 支持手动清理图片和音频缓存。
*   **Favicon支持**: 自动获取并显示网站的 Favicon，增强来源辨识度。
*   **美观的UI**: 采用 Material 3 设计语言，支持 Material You 动态主题，界面简洁、现代，支持深浅主题切换。
*   **便捷的导航**: 支持双击底部标签或单击顶部标题栏快速返回列表顶部，双击返回键退出应用。
*   **日志分享**: 支持通过系统分享功能导出和分享应用日志，方便问题排查。
*   **应用内更新**: 支持检查并下载最新版本。

## 🛠️ 技术栈

*   **语言**: [Kotlin](https://kotlinlang.org/)
*   **UI框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **架构**: MVVM (Model-View-ViewModel)
*   **网络请求**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/)
*   **JSON解析**: [Gson](https://github.com/google/gson)
*   **导航**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
*   **后台播放**: `Service` 和 `MediaSession`
*   **数据存储**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
*   **异步处理**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
*   **主题管理**: 自定义 ThemeController 支持动态主题切换

## 🎯 核心功能详解

### 播客播放系统
- **前台服务**: 使用 `PlayerService` 实现后台播放，支持媒体会话控制
- **预加载技术**: 智能预加载下一首播客，提升播放体验
- **播放模式**: 支持顺序播放、循环播放、乱序播放
- **媒体缓存**: 自动缓存已下载的音频文件，减少重复下载
- **通知栏集成**: 完整的媒体控制通知，支持播放/暂停/上下一首

### 网络代理系统
- **HTTP 代理**: 支持配置 HTTP 代理服务器
- **认证支持**: 支持用户名密码认证的代理
- **全局代理**: 所有网络请求（包括播客下载）都支持代理

### 数据管理
- **本地存储**: 使用 DataStore 存储用户设置和偏好
- **状态管理**: 完整的 ViewModel 状态管理，支持配置更改
- **阅读状态**: 智能跟踪文章阅读状态和浏览位置

### 缓存管理
- **统一管理**: 在设置页面提供缓存管理入口，可分别清理图片和音频缓存。
- **自动缓存**: 播客音频和网站Favicon会自动缓存，提高加载速度。

## 🚀 如何运行

### 环境要求
- Android Studio Arctic Fox (2020.3.1) 或更高版本
- Android SDK API 24 (Android 7.0) 或更高版本
- Kotlin 1.8.0 或更高版本

### 运行步骤
1.  克隆本仓库:
    ```bash
    git clone https://github.com/xusonfan/zenfeedApp.git
    ```
2.  使用 Android Studio 打开项目。
3.  等待 Gradle 同步完成。
4.  连接您的 Android 设备或启动一个模拟器。
5.  点击 "Run" 按钮来构建和运行应用。

### 配置说明
应用首次启动时，可以在设置页面配置：
- **API 服务器地址**: 默认为 `https://zenfeed.xyz/`
- **后端 URL**: 用于数据查询的后端服务地址
- **代理设置**: 如需要，可配置 HTTP 代理以访问服务

## 📱 应用架构

### 目录结构
```
app/src/main/java/com/ddyy/zenfeed/
├── data/                    # 数据层
│   ├── network/            # 网络请求
│   ├── model.kt            # 数据模型
│   ├── FeedRepository.kt   # 数据仓库
│   └── SettingsDataStore.kt # 设置存储
├── service/                # 服务层
│   └── PlayerService.kt    # 播放器服务
├── ui/                     # UI层
│   ├── feeds/              # 摘要相关页面
│   ├── player/             # 播放器相关
│   ├── settings/           # 设置页面
│   ├── webview/            # WebView 页面
│   ├── theme/              # 主题管理
│   └── navigation/         # 导航配置
└── MainActivity.kt         # 主活动
```

### 主要组件
- **FeedsScreen**: 主要的摘要列表页面
- **FeedDetailScreen**: 摘要详情页面，支持滑动切换
- **PlayerService**: 后台播放服务，支持媒体会话
- **SettingsScreen**: 设置页面，支持各种配置
- **WebViewScreen**: 内置浏览器页面
- **ThemeController**: 主题管理控制器

## 🔮 未来展望

*   **离线阅读**: 支持离线下载和阅读摘要内容。
*   **收藏同步**: 支持跨设备的收藏和阅读记录同步。
*   **个性化推荐**: 基于阅读历史的智能内容推荐。
*   **更多播客功能**: 支持音效增强等高级播客功能。
*   **社交分享**: 支持将喜欢的摘要分享到社交媒体。
*   **RSS 源管理**: 支持用户自定义添加和管理 RSS 源。

## 🐛 问题反馈

如果您在使用过程中遇到任何问题，请通过以下方式反馈：
- 在 GitHub 上提交 Issue
- 发送邮件至开发团队

## 📄 许可证

本项目采用 MIT 许可证，详情请查看 [LICENSE](LICENSE) 文件。

---

感谢您的使用！希望 Zenfeed 能为您带来愉快的阅读体验。