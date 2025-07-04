# Java Web 爬虫系统 API 文档

## 文档信息
- **版本**: 1.6.0
- **基础 URL**: http://localhost:8080
- **内容类型**: 除非特别说明，所有请求和响应均使用 `application/json`
- **字符编码**: UTF-8
- **认证方式**: 基于会话(Session)的认证

## 目录
1. [用户认证模块](#1-用户认证模块-apiuser)
   1. [用户注册](#11-用户注册)
   2. [用户登录](#12-用户登录)
   3. [获取当前登录用户信息](#13-获取当前登录用户信息)
2. [爬虫核心模块](#2-爬虫核心模块-api)
   1. [爬取单个新闻URL](#21-爬取单个新闻url)
   2. [二级爬取（从入口页爬取多篇新闻）](#22-二级爬取从入口页爬取多篇新闻)
   3. [按关键词爬取新闻](#23-按关键词爬取新闻)
   4. [导出新闻为文件（支持高级排版）](#24-导出新闻为文件支持高级排版)
   5. [获取用户爬取历史](#25-获取用户爬取历史)
      1. [删除单个历史记录](#251-删除单个历史记录)
      2. [批量删除历史记录](#252-批量删除历史记录)
      3. [清空历史记录](#253-清空历史记录)
   7. [数据分析功能](#27-数据分析功能)
      1. [词云数据生成](#271-词云数据生成)
      2. [热词排行榜](#272-热词排行榜)
      3. [关键词时间趋势分析](#273-关键词时间趋势分析)
      4. [内容来源分布分析](#274-内容来源分布分析)
   8. [API接入状态](#28-api接入状态)

## 1. 用户认证模块 (`/api/user`)

### 1.1 用户注册

**接口名称**: Register User

**路径**: `/api/user/register`

**请求方法**: `POST`

**功能描述**: 创建一个新用户账户。用户名在系统中必须是唯一的。成功注册后，用户可以使用凭据登录系统。

**认证要求**: 无需认证

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 限制条件 |
|--------|------|------|------|----------|
| username | String | 是 | 用户的登录名 | 长度建议在3-20个字符之间，只能包含字母、数字和下划线 |
| password | String | 是 | 用户的登录密码 | 长度建议在6-20个字符之间，建议包含字母、数字和特殊字符 |

**请求示例**:
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**响应**:

**成功响应** (200 OK):
```
Content-Type: text/plain

注册成功！
```

**错误响应** (400 Bad Request):
```
Content-Type: text/plain

用户名 'testuser' 已被注册！
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | 用户名已存在、用户名或密码不符合要求 |
| 500 Internal Server Error | 服务器内部错误 | 数据库操作失败 |

**安全性考虑**:
- 密码在传输过程中应使用HTTPS加密
- 密码在存储时应进行加密处理，不应明文存储

### 1.2 用户登录

**接口名称**: Login User

**路径**: `/api/user/login`

**请求方法**: `POST`

**功能描述**: 用户使用用户名和密码进行登录认证。成功后，服务器会建立一个会话(Session)，后续需要认证的请求会自动携带此会话信息。

**认证要求**: 无需认证

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 |
|--------|------|------|------|
| username | String | 是 | 已注册的用户名 |
| password | String | 是 | 对应的密码 |

**请求示例**:
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "message": "登录成功！",
  "username": "testuser"
}
```

**错误响应** (401 Unauthorized):
```
Content-Type: text/plain

用户名或密码错误
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 401 Unauthorized | 认证失败 | 用户名不存在或密码错误 |
| 500 Internal Server Error | 服务器内部错误 | 数据库操作失败 |

**安全性考虑**:
- 登录失败不应明确指出是用户名错误还是密码错误，以防止用户枚举攻击
- 应考虑实施登录尝试次数限制，防止暴力破解

### 1.3 获取当前登录用户信息

**接口名称**: Get Current User

**路径**: `/api/user/current`

**请求方法**: `GET`

**功能描述**: 检查当前用户的登录状态。如果已登录，返回用户名；否则返回未登录状态。此接口通常在页面加载时调用，用于确定用户是否已登录。

**认证要求**: 无需认证（但返回结果依赖于认证状态）

**请求参数**: 无

**响应**:

**成功响应** (200 OK): 用户已登录
```json
{
  "username": "testuser"
}
```

**错误响应** (401 Unauthorized): 用户未登录
```
Content-Type: text/plain

用户未登录
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |

**安全性考虑**:
- 此接口不应返回敏感的用户信息，仅返回必要的身份标识

## 2. 爬虫核心模块 (`/api`)

### 2.1 爬取单个新闻URL

**接口名称**: Crawl Single News URL

**路径**: `/api/crawl/single`

**请求方法**: `POST`

**功能描述**: 【基础】根据URL爬取单个新闻，同时记录爬取历史。接收一个新浪新闻的URL，爬取其内容，将数据存入数据库，并记录本次操作到用户的爬取历史中。该接口支持爬取新浪新闻网站上的新闻文章，并提取标题、来源、发布时间、正文内容等信息。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 格式要求 |
|--------|------|------|------|----------|
| url | String | 是 | 新浪新闻详情页URL | 必须是有效的新浪新闻URL格式，例如：https://news.sina.com.cn/c/yyyy-mm-dd/doc-[id].shtml |

**请求示例**:
```json
{
  "url": "https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml"
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "id": 1,
  "url": "https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml",
  "title": "5月份国民经济运行总体平稳、稳中有进",
  "source": "国家统计局网站",
  "publishTime": "2025-06-16T10:01:00",
  "content": "<p>...新闻正文HTML内容...</p>",
  "keywords": "制造业,5月份经济数据出炉",
  "fetchTime": "2025-06-16T22:10:00"
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | 未提供URL或URL为空、URL格式不正确 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 爬取过程中发生IO异常、网络连接问题、数据库错误 |

**技术说明**:
- 系统会自动解析HTML内容，提取新闻的标题、来源、发布时间和正文
- 爬取的内容会被保存到数据库中，同时会在用户的爬取历史中记录此次操作
- 如果同一URL已被爬取过，系统会返回数据库中已有的记录，而不会重复爬取

### 2.2 二级爬取（从入口页爬取多篇新闻）

**接口名称**: Crawl Multiple News from Entry Page

**路径**: `/api/crawl/from-index`

**请求方法**: `POST`

**功能描述**: 【高级】从新闻入口页爬取多篇新闻，同时记录爬取历史。接收一个新闻入口页的URL，爬取该页面上的所有新闻，并将数据存入数据库，同时记录本次操作到用户的爬取历史中。该接口支持爬取新浪新闻网站上的新闻入口页，并提取所有新闻的标题、来源、发布时间、正文内容等信息。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 格式要求 |
|--------|------|------|------|----------|
| url | String | 是 | 新闻入口页的URL | 必须是有效的新浪新闻入口页URL格式，例如：https://news.sina.com.cn/c/yyyy-mm-dd/ |

**请求示例**:
```json
{
  "url": "https://news.sina.com.cn/c/2025-06-16/"
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "message": "二级爬取任务完成",
  "crawledCount": 15,
  "entryUrl": "https://news.sina.com.cn/c/2025-06-16/"
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | url参数值无效（非有效的新浪新闻入口页URL） |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 爬取过程中发生IO异常、网络连接问题、数据库错误 |

**技术说明**:
- 系统会自动解析HTML内容，提取所有新闻的标题、来源、发布时间和正文
- 爬取的内容会被保存到数据库中，同时会在用户的爬取历史中记录此次操作
- 如果同一URL已被爬取过，系统会返回数据库中已有的记录，而不会重复爬取

### 2.3 按关键词爬取新闻

**接口名称**: Crawl News by Keyword

**路径**: `/api/crawl/by-keyword`

**请求方法**: `POST`

**功能描述**: 【新增功能】根据关键词从指定入口页面进行爬取。接收一个关键词和可选的入口页URL（默认为新浪新闻首页），爬取入口页面上所有标题含关键词的链接，并将数据存入数据库，同时记录本次操作到用户的爬取历史中。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 格式要求 |
|--------|------|------|------|----------|
| keyword | String | 是 | 要搜索的关键词 | 不能为空 |
| url | String | 否 | 新闻入口页的URL | 默认为"https://news.sina.com.cn/" |

**请求示例**:
```json
{
  "keyword": "经济",
  "url": "https://news.sina.com.cn/"
}
```

或仅提供关键词：
```json
{
  "keyword": "经济"
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "message": "关键词爬取任务完成",
  "crawledCount": 12,
  "keyword": "经济",
  "entryUrl": "https://news.sina.com.cn/"
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | 关键词为空 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 爬取过程中发生IO异常、网络连接问题、数据库错误 |

**技术说明**:
- 系统会遍历入口页面所有标题含关键词的链接并逐一爬取
- 爬取的内容会被保存到数据库中，同时会在用户的爬取历史中记录此次操作
- 爬取历史记录使用与二级爬取相同的`INDEX_CRAWL`类型，但在params中添加了keyword字段
- 操作可能需要较长时间，取决于匹配关键词的新闻数量

### 2.4 导出新闻为文件（支持高级排版）

**接口名称**: Export News as File

**路径**: `/api/export`

**请求方法**: `GET`和`POST`（两种方式都支持）

**功能描述**: 【高级】导出新闻为Word或PDF，支持基础和高级字体自定义。根据提供的新闻URL和指定的格式（Word或PDF），爬取新闻内容并生成对应格式的文件供用户下载。系统会自动排版文档，包括标题、副标题、正文、图片等元素，并提供专业的排版格式。用户可以自定义各级标题和正文的字体大小和行间距，实现个性化的文档排版效果。支持选择不同字体并可调整页面宽度。

**认证要求**: 需要认证（用户必须登录）

#### GET方法请求参数 (Query Parameters):
| 参数名 | 类型 | 必须 | 描述 | 可选值/默认值 | 限制条件 |
|--------|------|------|------|--------------|----------|
| url | String | 是 | 要导出的新闻的URL | 有效的新浪新闻URL | - |
| format | String | 是 | 导出的文件格式 | `word` 或 `pdf` | - |
| lineSpacing | Float | 否 | 文档行间距倍数 | 默认值: 1.5 | 范围: 1.0-3.0 |
| textFontSize | Integer | 否 | 基础正文字体大小 | 默认值: 14 | 范围: 8-32 |
| titleFontSize | Integer | 否 | 主标题字体大小 | 默认值: textFontSize+8 | - |
| h1FontSize | Integer | 否 | 一级标题字体大小 | 默认值: textFontSize+6 | - |
| h2FontSize | Integer | 否 | 二级标题字体大小 | 默认值: textFontSize+4 | - |
| h3FontSize | Integer | 否 | 三级标题字体大小 | 默认值: textFontSize+2 | - |
| captionFontSize | Integer | 否 | 图片注释和元数据字体大小 | 默认值: max(textFontSize-2, 10) | - |
| footerFontSize | Integer | 否 | 页脚字体大小 | 默认值: max(textFontSize-4, 8) | - |
| fontFamily | String | 否 | 文档使用的字体 | 默认值: 根据系统配置 | 推荐使用常见字体名称 |
| pageWidth | Integer | 否 | 页面宽度（像素） | 默认值: 800 | 范围: 500-1200 |
| skipHistoryRecord | Boolean | 否 | 是否跳过记录导出历史 | 默认值: false | - |

#### POST方法请求体参数 (JSON Body):
| 参数名 | 类型 | 必须 | 描述 | 可选值/默认值 | 限制条件 |
|--------|------|------|------|--------------|----------|
| url | String | 是 | 要导出的新闻的URL | 有效的新浪新闻URL | - |
| format | String | 是 | 导出的文件格式 | `word` 或 `pdf` | - |
| lineSpacing | Float | 否 | 文档行间距倍数 | 默认值: 1.5 | 范围: 1.0-3.0 |
| textFontSize | Integer | 否 | 基础正文字体大小 | 默认值: 14 | 范围: 8-32 |
| titleFontSize | Integer | 否 | 主标题字体大小 | 默认值: textFontSize+8 | - |
| h1FontSize | Integer | 否 | 一级标题字体大小 | 默认值: textFontSize+6 | - |
| h2FontSize | Integer | 否 | 二级标题字体大小 | 默认值: textFontSize+4 | - |
| h3FontSize | Integer | 否 | 三级标题字体大小 | 默认值: textFontSize+2 | - |
| captionFontSize | Integer | 否 | 图片注释和元数据字体大小 | 默认值: max(textFontSize-2, 10) | - |
| footerFontSize | Integer | 否 | 页脚字体大小 | 默认值: max(textFontSize-4, 8) | - |
| fontFamily | String | 否 | 文档使用的字体 | 默认值: 根据系统配置 | 推荐使用常见字体名称 |
| pageWidth | Integer | 否 | 页面宽度（像素） | 默认值: 800 | 范围: 500-1200 |
| skipHistoryRecord | Boolean | 否 | 是否跳过记录导出历史 | 默认值: false | - |

**GET请求示例**:
```
GET /api/export?url=https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml&format=pdf
```

```
GET /api/export?url=https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml&format=word&textFontSize=16&lineSpacing=2.0
```

```
GET /api/export?url=https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml&format=pdf&textFontSize=14&titleFontSize=26&h1FontSize=22&lineSpacing=1.8&fontFamily=Microsoft%20YaHei&skipHistoryRecord=true
```

**POST请求示例**:
```json
{
  "url": "https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml",
  "format": "pdf",
  "textFontSize": 14,
  "lineSpacing": 1.8,
  "fontFamily": "Microsoft YaHei",
  "pageWidth": 800,
  "skipHistoryRecord": true
}
```

**响应**:

**成功响应** (200 OK):

当 `format=pdf`:
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="5月份国民经济运行总体平稳、稳中有进.pdf"
Content-Length: [文件大小]
Cache-Control: must-revalidate, post-check=0, pre-check=0

[二进制文件内容]
```

当 `format=word`:
```
Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document
Content-Disposition: attachment; filename="5月份国民经济运行总体平稳、稳中有进.docx"
Content-Length: [文件大小]
Cache-Control: must-revalidate, post-check=0, pre-check=0

[二进制文件内容]
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | format参数值无效（非word或pdf）、URL格式不正确、textFontSize或lineSpacing超出有效范围 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 文件生成过程中发生错误、爬取新闻内容失败、字体处理问题 |

**技术说明**:
- **文档排版定制**:
  - 用户可通过`textFontSize`参数控制基础正文字体大小（范围8-32）
  - 用户可通过`lineSpacing`参数控制行间距倍数（范围1.0-3.0）
  - 用户可对各级标题、图片注释和页脚等元素分别指定字体大小：
    - `titleFontSize`: 主标题字体大小，默认为 textFontSize+8
    - `h1FontSize`: 一级标题字体大小，默认为 textFontSize+6 
    - `h2FontSize`: 二级标题字体大小，默认为 textFontSize+4
    - `h3FontSize`: 三级标题字体大小，默认为 textFontSize+2
    - `captionFontSize`: 图片注释和元数据字体大小，默认为 max(textFontSize-2, 10)
    - `footerFontSize`: 页脚字体大小，默认为 max(textFontSize-4, 8)
  - 所有字体大小参数都为可选参数，如不提供则根据基础正文字体大小自动计算
  - 段落间距也会根据字体大小和行间距自动调整

- **字体支持**:
  - 允许用户通过`fontFamily`参数选择字体
  - 支持常见中文字体，如"微软雅黑"、"宋体"、"黑体"、"楷体"等
  - 支持常见英文字体，如"Arial"、"Times New Roman"等
  - 确保中文字符正确显示

- **文档格式化**:
  - 文档包含标准化的标题、副标题(来源和发布时间)、正文段落和页脚
  - 标题使用居中、粗体、大号字体样式
  - 段落使用适当的缩进和行间距，增强可读性
  - 页脚包含生成信息

- **文本处理**:
  - 自动识别并格式化正文中的段落文本
  - 支持标题(h1-h6)的不同级别样式
  - 支持有序列表(ol)和无序列表(ul)的格式化

- **图片处理**:
  - 自动从原始网页下载并嵌入文章中的图片
  - 图片居中显示，并保持适当的尺寸比例
  - 支持图片说明文字的展示
  - 当图片无法下载时提供替代文本

- **文件格式说明**:
  - Word文档使用DOCX格式（Office Open XML），兼容Microsoft Word 2007及以上版本
  - PDF文档使用标准PDF格式，可在任何PDF阅读器中查看
  - 文件名使用新闻标题，自动处理特殊字符

### 2.5 获取用户爬取历史

**接口名称**: Get User Crawl History

**路径**: `/api/history`

**请求方法**: `GET`

**功能描述**: 【基础】获取当前登录用户的爬取历史记录，按时间倒序排列。这些记录包括用户爬取过的所有新闻URL、标题和爬取时间等信息。每条历史记录包含爬取的URL、标题和时间。

**认证要求**: 需要认证（用户必须登录）

**请求参数**: 无

**响应**:

**成功响应** (200 OK):
```json
[
  {
    "id": 6,
    "userId": 1,
    "crawlType": "INDEX_CRAWL",
    "url": "https://news.sina.com.cn/",
    "title": "使用关键词"经济"从 https://news.sina.com.cn/ 爬取了 12 条新闻",
    "params": {
      "keyword": "经济",
      "totalCount": 12,
      "sampleUrls": [
        "https://news.sina.com.cn/c/2023-10-20/doc-econ1.shtml",
        "https://news.sina.com.cn/c/2023-10-20/doc-econ2.shtml",
        "https://news.sina.com.cn/c/2023-10-20/doc-econ3.shtml"
      ],
      "crawlTime": "2023-10-20T14:30:00"
    },
    "crawlTime": "2023-10-20T14:30:00"
  },
  {
    "id": 5,
    "userId": 1,
    "crawlType": "SCHEDULED_CRAWL",
    "url": "https://news.sina.com.cn/",
    "title": "[定时任务] 从新浪新闻首页爬取了 32 条新闻",
    "params": {
      "totalCount": 32,
      "sampleUrls": [
        "https://news.sina.com.cn/c/2023-09-15/doc-id1.shtml",
        "https://news.sina.com.cn/c/2023-09-15/doc-id2.shtml",
        "https://news.sina.com.cn/c/2023-09-15/doc-id3.shtml"
      ],
      "executionTime": "2023-09-15T08:00:00",
      "isScheduled": true
    },
    "crawlTime": "2023-09-15T08:00:00"
  },
  {
    "id": 4,
    "userId": 1,
    "crawlType": "INDEX_CRAWL",
    "url": "https://news.sina.com.cn/c/2025-06-16/",
    "title": "从入口页 https://news.sina.com.cn/c/2025-06-16/ 爬取了 15 条新闻",
    "params": {
      "totalCount": 15,
      "sampleUrls": [
        "https://news.sina.com.cn/c/2025-06-16/doc-id1.shtml",
        "https://news.sina.com.cn/c/2025-06-16/doc-id2.shtml",
        "https://news.sina.com.cn/c/2025-06-16/doc-id3.shtml"
      ],
      "crawlTime": "2025-06-17T09:30:00"
    },
    "crawlTime": "2025-06-17T09:30:00"
  },
  {
    "id": 3,
    "userId": 1,
    "crawlType": "SINGLE_URL",
    "url": "https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml",
    "title": "导出为PDF: 5月份国民经济运行总体平稳、稳中有进",
    "params": null,
    "crawlTime": "2025-06-16T23:15:00"
  },
  {
    "id": 2,
    "userId": 1,
    "crawlType": "SINGLE_URL",
    "url": "https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml",
    "title": "5月份国民经济运行总体平稳、稳中有进",
    "params": null,
    "crawlTime": "2025-06-16T22:10:00"
  },
  {
    "id": 1,
    "userId": 1,
    "crawlType": "SINGLE_URL",
    "url": "https://news.sina.com.cn/c/2025-06-15/doc-infacvcp0828397.shtml",
    "title": "重磅官宣！歼-10CE又要出国了",
    "params": null,
    "crawlTime": "2025-06-16T21:30:00"
  }
]
```

如果用户没有任何历史记录，会返回一个空数组 `[]`。

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 查询数据库时发生错误 |

**响应字段说明**:
| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | Integer | 历史记录的唯一标识符 |
| userId | Integer | 用户ID |
| crawlType | String | 爬取类型，如"SINGLE_URL"表示单个URL爬取，"INDEX_CRAWL"表示从入口页爬取多篇新闻，"SCHEDULED_CRAWL"表示定时爬取 |
| url | String | 爬取的新闻URL（对于二级爬取和定时爬取，这里是入口页URL） |
| title | String | 爬取的内容标题或操作描述 |
| params | Object | 爬取参数，对于二级爬取和定时爬取，包含totalCount（爬取总数）、sampleUrls（爬取URL样本）等 |
| crawlTime | String | 爬取时间，ISO 8601格式 |

**技术说明**:
- 历史记录按照`crawlTime`字段倒序排列，最新的记录排在最前面
- 系统会自动关联用户ID，只返回当前登录用户的历史记录
- 根据`crawlType`字段区分不同类型的爬取操作：
  - `SINGLE_URL`: 单个URL爬取，params字段通常为null
  - `INDEX_CRAWL`: 二级爬取（从入口页爬取多篇新闻），params字段包含爬取统计和样本URL
  - `SCHEDULED_CRAWL`: 定时爬取，由系统自动执行，userId通常为系统管理员ID
- 特殊爬取类型的扩展：
  - 关键词爬取：使用`INDEX_CRAWL`类型，但在params字段中添加了`keyword`参数
  - 这种设计保持了数据库结构的稳定性，同时提供了扩展灵活性
- 导出操作会记录到历史中，并在title字段前标注导出格式

### 2.5.1 删除单个历史记录

**接口名称**: Delete Single History Record

**路径**: `/api/history/{id}`

**请求方法**: `DELETE`

**功能描述**: 【基础】删除指定ID的历史记录。该接口会验证当前用户是否有权限删除指定的历史记录（只能删除自己的记录）。

**认证要求**: 需要认证（用户必须登录）

**路径参数**:
| 参数名 | 类型 | 必须 | 描述 |
|--------|------|------|------|
| id | Long | 是 | 要删除的历史记录ID |

**响应**:

**成功响应** (200 OK):
```json
{
  "success": true,
  "message": "历史记录已删除"
}
```

**错误响应** (403 Forbidden):
```json
{
  "success": false,
  "message": "无法删除该历史记录，记录不存在或不属于当前用户"
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 403 Forbidden | 禁止访问 | 尝试删除不属于当前用户的历史记录 |
| 500 Internal Server Error | 服务器内部错误 | 删除操作失败 |

**技术说明**:
- 系统会验证历史记录的所有者，确保用户只能删除自己的历史记录
- 如果记录不存在或不属于当前用户，将返回403状态码
- 删除操作是永久性的，无法恢复

### 2.5.2 批量删除历史记录

**接口名称**: Batch Delete History Records

**路径**: `/api/history/batch`

**请求方法**: `DELETE`

**功能描述**: 【高级】批量删除多条历史记录。该接口接收一个历史记录ID列表，并删除其中属于当前用户的记录。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 |
|--------|------|------|------|
| ids | Array[Long] | 是 | 要删除的历史记录ID列表 |

**请求示例**:
```json
{
  "ids": [1, 2, 3, 4, 5]
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "success": true,
  "message": "成功删除 3 条历史记录",
  "deletedCount": 3,
  "totalRequested": 5
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | 未提供ID列表或ID列表为空 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 批量删除操作失败 |

**技术说明**:
- 系统会过滤ID列表，只删除属于当前用户的历史记录
- 响应中会返回成功删除的记录数量和请求的总记录数量
- 即使某些记录不存在或不属于当前用户，操作也会继续处理其他有效记录
- 批量删除使用事务处理，确保操作的原子性

### 2.5.3 清空历史记录

**接口名称**: Clear All History Records

**路径**: `/api/history/all`

**请求方法**: `DELETE`

**功能描述**: 【高级】清空当前用户的所有历史记录。该接口会删除当前用户的所有爬取历史记录。

**认证要求**: 需要认证（用户必须登录）

**请求参数**: 无

**响应**:

**成功响应** (200 OK):
```json
{
  "success": true,
  "message": "已清空所有历史记录，共删除 15 条",
  "deletedCount": 15
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 清空操作失败 |

**技术说明**:
- 该操作会删除当前用户的所有历史记录，不可恢复
- 操作使用事务处理，确保要么全部删除成功，要么全部保留
- 响应中会返回成功删除的记录数量

### 2.5.4 获取爬取历史关联的新闻数据

**接口名称**: Get News Data by History ID

**路径**: `/api/history/{historyId}/news`

**请求方法**: `GET`

**功能描述**: 【新增功能】根据爬取历史ID获取关联的所有新闻数据。该接口会验证当前用户是否有权限查看指定的历史记录（只能查看自己的记录），并返回与该爬取历史关联的所有新闻数据。

**认证要求**: 需要认证（用户必须登录）

**路径参数**:
| 参数名 | 类型 | 必须 | 描述 |
|--------|------|------|------|
| historyId | Long | 是 | 要查询的历史记录ID |

**响应**:

**成功响应** (200 OK):
```json
[
  {
    "id": 101,
    "url": "https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126726.shtml",
    "title": "5月份国民经济运行总体平稳、稳中有进",
    "source": "国家统计局网站",
    "publishTime": "2025-06-16T10:01:00",
    "content": "<p>...新闻正文HTML内容...</p>",
    "keywords": "制造业,5月份经济数据出炉",
    "fetchTime": "2025-06-16T22:10:00"
  },
  {
    "id": 102,
    "url": "https://news.sina.com.cn/c/2025-06-16/doc-infafpnq1126727.shtml",
    "title": "我国工业经济延续回升向好态势",
    "source": "经济日报",
    "publishTime": "2025-06-16T09:30:00",
    "content": "<p>...新闻正文HTML内容...</p>",
    "keywords": "工业经济,回升",
    "fetchTime": "2025-06-16T22:12:00"
  }
]
```

如果没有关联的新闻数据，会返回一个空数组 `[]`。

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 403 Forbidden | 禁止访问 | 尝试查看不属于当前用户的历史记录 |
| 500 Internal Server Error | 服务器内部错误 | 查询数据库时发生错误 |

**技术说明**:
- 系统会验证历史记录的所有者，确保用户只能查看自己的历史记录
- 该接口主要用于查看批量爬取（二级爬取或关键词爬取）的详细结果
- 返回的新闻数据包含完整的内容，包括标题、来源、发布时间、正文等
- 新闻数据按照ID排序

## 2.7 数据分析功能

此模块提供了对爬取的新闻数据进行分析的API接口，包括词云数据生成、热词排行榜、关键词时间趋势分析和内容来源分布分析等功能。

### 2.7.1 词云数据生成

**接口名称**: Generate Word Cloud Data

**路径**: `/api/analysis/word-cloud`

**请求方法**: `POST`

**功能描述**: 【数据分析】从指定爬取历史关联的新闻数据中提取关键词，生成词云数据。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 限制条件 |
|--------|------|------|------|----------|
| historyId | Long | 是 | 关联的爬取历史记录ID，表示对哪次爬取的数据进行分析 | 必须是有效的历史记录ID |
| limit | Integer | 否 | 返回的词语数量限制 | 默认值：50，范围1-100 |

**请求示例**:
```json
{
    "historyId": 12345,
    "limit": 50
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "wordCloud": [
    {
      "text": "经济",
      "weight": 95
    },
    {
      "text": "发展",
      "weight": 82
    },
    {
      "text": "科技",
      "weight": 78
    },
    // ... 更多词语及其权重
  ],
  "total": 50
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | historyId参数无效或缺失 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 数据库查询失败、分词处理错误 |

**技术说明**:
- 系统默认从新闻数据的`keywords`字段提取关键词
- 对关键词进行统计，生成适合词云展示的数据格式
- 返回的词云数据按权重（出现频率）降序排序
- 返回的词云数据格式适用于常见的词云可视化库，如`wordcloud2.js`

### 2.7.2 热词排行榜

**接口名称**: Get Hot Words Ranking

**路径**: `/api/analysis/hot-words`

**请求方法**: `POST`

**功能描述**: 【数据分析】从指定爬取历史关联的新闻数据中提取关键词，生成热词排行榜。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 默认值 | 可选值 |
|--------|------|------|------|---------|--------|
| historyId | Long | 是 | 关联的爬取历史记录ID，表示对哪次爬取的数据进行分析 | 必须是有效的历史记录ID |
| limit | Integer | 否 | 返回结果数量限制 | 20 | 1-100 |

**请求示例**:
```json
{
    "historyId": 12345,
    "limit": 50
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "hotWords": [
    {
      "word": "经济",
      "count": 158
    },
    {
      "word": "科技",
      "count": 124
    },
    {
      "word": "创新",
      "count": 95
    },
    // ... 更多热词及其出现次数
  ],
  "total": 50
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | historyId参数无效或缺失 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 数据库查询失败 |

**技术说明**:
- 系统从新闻数据中提取关键词并进行统计
- 统计每个关键词的出现次数，按出现次数降序排列
- 返回的热词数据可用于生成排行榜、柱状图等可视化效果

### 2.7.3 关键词时间趋势分析

**接口名称**: Get Keyword Time Trend

**路径**: `/api/analysis/time-trend`

**请求方法**: `POST`

**功能描述**: 【数据分析】分析指定关键词在爬取历史关联的新闻数据中的出现频率变化，生成时间趋势数据。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 默认值 | 可选值 |
|--------|------|------|------|---------|--------|
| historyId | Long | 是 | 关联的爬取历史记录ID | - | 必须是有效的历史记录ID |
| keyword | String | 是 | 要分析的关键词 | - | 任何非空字符串 |
| timeUnit | String | 否 | 时间单位 | "day" | "day", "hour6", "hour12" |

**请求示例**:
```json
{
  "historyId": 12345,
  "keyword": "科技",
  "timeUnit": "day"
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "keyword": "科技",
  "timeUnit": "day",
  "trendData": [
    {
      "timePoint": "2023-10-15",
      "count": 12
    },
    {
      "timePoint": "2023-10-16",
      "count": 18
    },
    {
      "timePoint": "2023-10-17",
      "count": 15
    },
    // ... 更多时间点及对应的计数
  ]
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | 关键词为空、historyId参数无效或缺失 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 数据库查询失败 |

**技术说明**:
- 系统根据指定的关键词，查询包含该关键词的新闻记录
- 根据指定的时间单位（日、6小时、12小时）将数据按时间分组
- 统计每个时间点中包含关键词的新闻数量
- 返回的时间趋势数据可用于生成折线图、面积图等时序可视化效果

### 2.7.4 内容来源分布分析

**接口名称**: Get Source Distribution

**路径**: `/api/analysis/source-distribution`

**请求方法**: `POST`

**功能描述**: 【数据分析】分析爬取历史关联的新闻数据中的来源分布情况。

**认证要求**: 需要认证（用户必须登录）

**请求头**:
| 名称 | 必须 | 描述 |
|------|------|------|
| Content-Type | 是 | 必须为 `application/json` |

**请求体参数**:
| 参数名 | 类型 | 必须 | 描述 | 默认值 | 可选值 |
|--------|------|------|------|---------|--------|
| historyId | Long | 是 | 关联的爬取历史记录ID | - | 必须是有效的历史记录ID |

**请求示例**:
```json
{
  "historyId": 12345
}
```

**响应**:

**成功响应** (200 OK):
```json
{
  "sourceDistribution": [
    {
      "source": "新浪新闻",
      "count": 328
    },
    {
      "source": "人民日报",
      "count": 156
    },
    {
      "source": "央视网",
      "count": 98
    },
    // ... 更多来源及其计数
  ],
  "total": 3
}
```

**可能的错误码**:
| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | historyId参数无效或缺失 |
| 401 Unauthorized | 未认证 | 用户未登录或会话已过期 |
| 500 Internal Server Error | 服务器内部错误 | 数据库查询失败 |

**技术说明**:
- 系统从新闻数据的`source`字段提取来源信息
- 统计每个来源的新闻数量，按数量降序排列
- 返回的来源分布数据可用于生成饼图、柱状图等可视化效果

## 2.8 API接入状态

本节列出了API的前端接入状态，帮助开发者了解哪些功能已经在前端实现，哪些还需要进一步开发。

| API功能 | 接口路径 | 前端接入状态 | 说明 |
|---------|----------|--------------|------|
| 用户注册 | `/api/user/register` | ✅ 已接入 | 在register.html中实现 |
| 用户登录 | `/api/user/login` | ✅ 已接入 | 在login.html中实现 |
| 获取当前用户信息 | `/api/user/current` | ✅ 已接入 | 首页顶部用户状态栏显示 |
| 爬取单个新闻URL | `/api/crawl/single` | ✅ 已接入 | 首页搜索框功能 |
| 二级爬取 | `/api/crawl/from-index` | ✅ 已接入 | 首页的爬取类型选择器 |
| 关键词爬取 | `/api/crawl/by-keyword` | ✅ 已接入 | 首页的爬取类型选择器 |
| 导出新闻为文件 | `/api/export` | ✅ 已接入 | 在结果页面的导出按钮实现 |
| 获取爬取历史 | `/api/history` | ✅ 已接入 | 侧边栏的历史记录列表 |
| 删除历史记录 | `/api/history/{id}` | ✅ 已接入 | 历史记录项的删除按钮 |
| 批量删除历史 | `/api/history/batch` | ❌ 未接入 | 需要实现批量选择和删除功能 |
| 清空历史记录 | `/api/history/all` | ✅ 已接入 | 历史记录顶部的清空按钮 |
| 获取历史记录关联新闻 | `/api/history/{historyId}/news` | ❌ 未接入 | 需在历史记录详情页面添加 |
| 词云数据生成 | `/api/analysis/word-cloud` | ❌ 未接入 | 需要添加数据分析页面 |
| 热词排行榜 | `/api/analysis/hot-words` | ❌ 未接入 | 需要添加数据分析页面 |
| 关键词时间趋势 | `/api/analysis/time-trend` | ❌ 未接入 | 需要添加数据分析页面 |
| 内容来源分布 | `/api/analysis/source-distribution` | ❌ 未接入 | 需要添加数据分析页面 |

### 待开发功能建议

1. **数据分析页面**
   - 添加词云可视化
   - 添加热词排行榜
   - 添加关键词时间趋势图
   - 添加来源分布饼图
   - 提供历史记录选择器，允许用户选择要分析的爬取历史

2. **高级历史记录管理**
   - 添加批量选择和删除功能
   - 按爬取类型筛选历史记录
   - 添加历史记录详情页面，显示关联的新闻数据
   - 提供历史记录导出功能

## 错误处理

所有API可能返回的通用错误码：

| 状态码 | 描述 | 可能原因 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | 缺少必要参数、参数格式不正确 |
| 401 Unauthorized | 未授权 | 用户未登录、会话已过期 |
| 403 Forbidden | 禁止访问 | 用户无权限访问该资源 |
| 404 Not Found | 资源不存在 | 请求的URL不存在 |
| 405 Method Not Allowed | 方法不允许 | 使用了不支持的HTTP方法 |
| 500 Internal Server Error | 服务器内部错误 | 服务器运行时异常、数据库错误 |

## 安全性说明

1. **认证机制**：系统使用基于会话(Session)的认证机制，用户登录后服务器会创建会话并返回会话ID（通常存储在Cookie中）。
2. **数据传输**：建议在生产环境中使用HTTPS加密传输，防止数据被窃听。
3. **输入验证**：所有API都会对输入参数进行验证，防止注入攻击和其他安全问题。
4. **错误信息**：错误信息设计为提供足够的信息以便调试，但不会泄露系统内部细节。
5. **访问控制**：系统确保用户只能访问自己的数据，不能访问或修改其他用户的数据。
6. **文件安全**：生成的文件不包含执行代码，并进行了安全的文件名处理。

## 数据模型

### NewsData
| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | Integer | 新闻数据的唯一标识符 |
| url | String | 新闻的原始URL |
| title | String | 新闻标题 |
| source | String | 新闻来源 |
| publishTime | String | 新闻发布时间，ISO 8601格式 |
| content | String | 新闻正文内容，HTML格式 |
| keywords | String | 新闻关键词，逗号分隔 |
| fetchTime | String | 爬取时间，ISO 8601格式 |
| crawlHistoryId | Integer | 关联的爬取历史记录ID |

### CrawlHistory
| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | Integer | 历史记录的唯一标识符 |
| userId | Integer | 用户ID |
| crawlType | String | 爬取类型 |
| url | String | 爬取的URL |
| title | String | 爬取的内容标题或操作描述 |
| params | Object | 爬取参数 |
| crawlTime | String | 爬取时间，ISO 8601格式 |
| newsData | Array[NewsData] | 关联的新闻数据（一对多关系） |

### User
| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | Integer | 用户的唯一标识符 |
| username | String | 用户名 |
| password | String | 密码（加密存储） |

## 版本历史

| 版本 | 日期 | 描述 |
|------|------|------|
| 2.0.0 | 2024-06-18 | 更新数据分析API文档，修改为基于历史记录ID的数据分析方式；调整API接入状态，移除未实现的定时任务相关API |
| 1.9.0 | 2023-12-20 | 添加数据分析功能，支持词云、热词排行榜、时间趋势和来源分布分析 |
| 1.8.0 | 2023-12-10 | 增强导出功能，添加字体选择、页面宽度设置、跳过历史记录选项，支持POST方法，美化导出表单UI |
| 1.7.0 | 2023-11-15 | 添加历史记录管理功能，支持删除单条、批量删除和清空历史记录 |
| 1.6.0 | 2023-10-20 | 添加按关键词爬取功能，支持从指定入口页爬取含关键词的新闻 |
| 1.5.0 | 2023-09-15 | 添加定时爬取功能，支持每日自动爬取新浪新闻首页 |
| 1.4.0 | 2023-08-10 | 添加二级爬取功能，支持从新闻列表页自动爬取多篇新闻 |
| 1.3.0 | 2023-07-15 | 增强文档排版功能，添加对不同级别标题的字体大小独立控制 |
| 1.2.0 | 2023-06-30 | 添加字号和行间距自定义功能，增强文档排版的灵活性 |
| 1.1.0 | 2023-06-15 | 改进文件导出功能，增强Word和PDF文档的排版和图片处理 |
| 1.0.0 | 2023-06-01 | 初始版本 |

## 技术说明
- 系统使用Spring Boot框架构建，采用标准的MVC架构
- 数据持久化使用JPA实现，支持MySQL 8.0数据库
- 身份认证使用Spring Security实现，采用基于会话的认证机制
- 爬虫功能使用Jsoup库实现，支持自适应多种新闻页面格式

### 实体关系说明
1. **用户(User)与爬取历史(CrawlHistory)**: 一对多关系，一个用户可以有多条爬取历史记录。
2. **爬取历史(CrawlHistory)与新闻数据(NewsData)**: 一对多关系，一条爬取历史可以关联多条新闻数据。
   - 对于单个URL爬取，一条爬取历史记录对应一条新闻数据
   - 对于二级爬取和关键词爬取，一条爬取历史记录可能对应多条新闻数据
   - 通过这种关联，系统能够追踪每篇新闻是在哪次爬取操作中获取的

### 爬取历史与新闻数据关联的优势
1. **数据溯源**: 通过关联关系，系统可以准确追踪每篇新闻的来源操作，便于分析和管理
2. **完整性保证**: 强制外键约束确保数据引用的完整性，防止出现"悬空"引用
3. **查询效率**: 通过关联查询，可以高效地获取特定爬取操作的所有新闻数据
4. **分析功能支持**: 为数据分析功能提供更丰富的上下文信息，便于实现更复杂的分析逻辑

### 数据库设计说明
系统使用的三个主要表及其关系如下：
- `t_user`: 存储用户信息，主键是`id`
- `t_crawl_history`: 存储爬取历史记录，主键是`id`，外键`user_id`关联到`t_user.id`
- `t_news_data`: 存储新闻数据，主键是`id`，外键`crawl_history_id`关联到`t_crawl_history.id`

这种设计实现了完整的数据关联链路：用户->爬取历史->新闻数据，便于追踪和分析。

## 3. API 测试指南 - Postman

本章节旨在指导用户如何使用 Postman 工具测试数据分析相关的 API，并处理认证。

**重要提示**：在进行数据分析 API 调用之前，请确保您已经成功运行了爬虫，并且数据库中有一些新闻数据和 `historyId`。您可以先通过 `/api/history/recent` 接口获取有效的 `historyId`。

### 3.1 获取认证会话（登录）

*   **请求类型**: `POST`
*   **请求 URL**: `http://localhost:8080/api/user/login` 
*   **Headers**:
    *   `Content-Type`: `application/x-www-form-urlencoded`
*   **Body**: 选择 `x-www-form-urlencoded`，并添加以下键值对：
    *   `username`: 您的用户名 (例如: `testuser`)
    *   `password`: 您的密码 (例如: `testpassword`)

如果登录成功，您会收到一个包含 `JSESSIONID` cookie 的响应

### 3.2 测试数据分析 API

一旦您成功登录并获得了 `JSESSIONID`，您就可以测试数据分析 API 了。Postman 会自动管理会话 cookie。

#### 3.2.1 词云数据 (`/api/analysis/word-cloud`)

*   **请求类型**: `POST`
*   **请求 URL**: `http://localhost:8080/api/analysis/word-cloud`
*   **Headers**:
    *   `Content-Type`: `application/json`
*   **Body**: 选择 `raw` 和 `JSON`，然后输入以下 JSON 格式的请求体：

    ```json
    {
        "limit": 50,
        "historyId": 12345
    }
    ```
    *   `limit`: 返回词语的数量限制。
    *   `historyId`: 这是**必填**参数，用于指定要分析的新闻数据所属的抓取历史ID。您需要一个有效的 `historyId` 才能成功获取数据。

#### 3.2.2 热词排行榜 (`/api/analysis/hot-words`)

*   **请求类型**: `POST`
*   **请求 URL**: `http://localhost:8080/api/analysis/hot-words`
*   **Headers**:
    *   `Content-Type`: `application/json`
*   **Body**: 选择 `raw` 和 `JSON`，然后输入以下 JSON 格式的请求体：

    ```json
    {
        "limit": 20,
        "historyId": 12345
    }
    ```
    *   `limit`: 返回热词的数量限制。
    *   `historyId`: 这是**必填**参数，用于指定要分析的新闻数据所属的抓取历史ID。

#### 3.2.3 关键词时间趋势分析 (`/api/analysis/time-trend`)

*   **请求类型**: `POST`
*   **请求 URL**: `http://localhost:8080/api/analysis/time-trend`
*   **Headers**:
    *   `Content-Type`: `application/json`
*   **Body**: 选择 `raw` 和 `JSON`，然后输入以下 JSON 格式的请求体：

    ```json
    {
        "keyword": "示例关键词",
        "timeUnit": "day",
        "historyId": 12345
    }
    ```
    *   `keyword`: 要分析的关键词。
    *   `timeUnit`: 时间单位，例如 `day`, `week`, `month`, `year`。
    *   `historyId`: 这是**必填**参数，用于指定要分析的新闻数据所属的抓取历史ID。

#### 3.2.4 内容来源分布 (`/api/analysis/source-distribution`)

*   **请求类型**: `POST`
*   **请求 URL**: `http://localhost:8080/api/analysis/source-distribution`
*   **Headers**:
    *   `Content-Type`: `application/json`
*   **Body**: 选择 `raw` 和 `JSON`，然后输入以下 JSON 格式的请求体：

    ```json
    {
        "historyId": 12345
    }
    ```
    *   `historyId`: 这是**必填**参数，用于指定要分析的新闻数据所属的抓取历史ID。