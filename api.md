# Zenfeed Query API 使用教程

Zenfeed Query API 允许用户通过多种条件检索存储的 Feed 数据。本教程将详细介绍如何使用此 API。

## 接口说明

### 请求

*   **方法**: `POST`
*   **URL**: `/query`
*   **Content-Type**: `application/json`

### 请求体 (JSON)

```json
{
  "query": "string",
  "threshold": 0.55,
  "label_filters": ["string"],
  "summarize": false,
  "limit": 10,
  "start": "2006-01-02T15:04:05Z07:00",
  "end": "2006-01-02T15:04:05Z07:00"
}
```

**字段说明:**

*   `query` (string, 可选):
    *   用于语义搜索的查询字符串。
    *   如果提供，必须至少包含 5 个字符。
    *   如果为空或未提供，则不进行语义搜索，仅根据其他条件（如标签、时间）过滤。
*   `threshold` (float32, 可选, 默认值: `0.55`):
    *   语义搜索的相关性阈值。
    *   取值范围: `[0, 1]`。
    *   仅当 `query` 字段非空时有效。
*   `label_filters` ([]string, 可选):
    *   一个字符串数组，用于根据 Feed 的标签进行过滤。
    *   每个过滤器的格式为:
        *   `"key=value"`: 匹配标签 `key` 的值为 `value` 的 Feed。
        *   `"key!=value"`: 匹配标签 `key` 的值不为 `value` 的 Feed。
    *   常用的 `key` 包括:
        *   `source`: Feed 来源
        *   `title`: Feed 标题
        *   `你在 rewrite 阶段自定义创建的`：比如 category
    *   可以指定多个过滤器，它们之间是 "AND" 关系。
*   `summarize` (bool, 可选, 默认值: `false`):
    *   是否对查询结果进行摘要。
    *   如果为 `true`，系统将调用配置的 LLM (Large Language Model) 对返回的 Feed 内容进行总结。
*   `limit` (int, 可选, 默认值: `10`):
    *   返回 Feed 结果的最大数量。
    *   取值范围: `[1, 500]`。
*   `start` (string, 可选, 默认值: 24小时前):
    *   查询的时间范围的开始时间（包含）。
    *   格式为 RFC3339 (例如: `"2023-10-26T10:00:00Z"`)。
*   `end` (string, 可选, 默认值: 当前时间):
    *   查询的时间范围的结束时间（不包含）。
    *   格式为 RFC3339 (例如: `"2023-10-27T10:00:00Z"`)。
    *   `end` 时间必须晚于 `start` 时间。

### 响应体 (JSON)

```json
{
  "summary": "string",
  "feeds": [
    {
      "labels": {
        "type": "rss",
        "source": "Example News",
        "title": "Breaking News: AI Revolutionizes Everything",
        "link": "http://example.com/news/123",
        "pub_time": "2023-10-26T09:30:00Z",
        "content": "Detailed content of the news article..."
      },
      "time": "2023-10-26T10:15:30+08:00",
      "score": 0.85
    }
  ],
  "count": 1
}
```

**字段说明:**

*   `summary` (string, 可选):
    *   如果请求中的 `summarize` 为 `true` 且成功生成摘要，此字段将包含 LLM 生成的内容摘要。
    *   如果生成摘要失败，可能包含错误信息。
*   `feeds` ([]object, 必须):
    *   一个对象数组，每个对象代表一个符合查询条件的 Feed。
    *   **Feed 对象结构**:
        *   `labels` (object): Feed 的元数据标签，键值对形式。
            *   `type` (string): Feed 类型。
            *   `source` (string): Feed 来源。
            *   `title` (string): Feed 标题。
            *   `link` (string): Feed 原始链接。
            *   `pub_time` (string): Feed 发布时间。
            *   `content` (string): Feed 内容。
            *   ... (其他自定义标签)
        *   `time` (string): Feed 被系统记录或处理的时间戳 (RFC3339 格式，通常为服务器本地时区)。
        *   `score` (float32, 可选):
            *   当请求中提供了 `query` (进行了语义搜索) 时，此字段表示该 Feed 与查询的相关性得分。
            *   得分越高，相关性越强。
*   `count` (int, 必须):
    *   返回的 `feeds` 数组中的 Feed 数量。

## `curl` 示例

以下示例假设 Zenfeed 服务运行在 `http://localhost:1300`。

### 1. 基本查询 (获取最近10条记录)

获取最近（默认24小时内）的最多10条 Feed。

```bash
curl -X POST http://localhost:1300/query \
-H "Content-Type: application/json" \
-d '{}'
```

### 2. 语义搜索

查询与 "人工智能最新进展" 相关的 Feed，并设置相关性阈值为 `0.7`。

```bash
curl -X POST http://localhost:1300/query \
-H "Content-Type: application/json" \
-d '{
  "query": "人工智能最新进展",
  "threshold": 0.7
}'
```

### 3. 带标签过滤的查询

查询类型为 "rss" 且来源不是 "SpecificSource" 的 Feed。

```bash
curl -X POST http://localhost:1300/query \
-H "Content-Type: application/json" \
-d '{
  "label_filters": [
    "type=rss",
    "source!=SpecificSource"
  ]
}'
```

### 4. 带时间范围的查询

查询 2023年10月25日 00:00:00 UTC 到 2023年10月26日 00:00:00 UTC 之间的 Feed。

```bash
curl -X POST http://localhost:1300/query \
-H "Content-Type: application/json" \
-d '{
  "start": "2023-10-25T00:00:00Z",
  "end": "2023-10-26T00:00:00Z"
}'
```

### 5. 组合查询示例

查询过去3天内，与 "开源项目" 相关的 Feed，类型为 "github_release"，并获取摘要，最多返回20条。

```bash
# 假设今天是 2023-10-28
curl -X POST http://localhost:1300/query \
-H "Content-Type: application/json" \
-d '{
  "query": "最近的热门开源项目", # 尽可能详细，获得最佳搜索效果
  "threshold": 0.6,
  "label_filters": ["source=github_trending"],
  "summarize": true,
  "limit": 20,
  "start": "2023-10-25T00:00:00Z", # 手动计算或动态生成
  "end": "2023-10-28T00:00:00Z"   # 手动计算或动态生成
}'
```