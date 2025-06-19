# Java Web爬虫系统 - 项目结构说明

## 项目概述

本项目是一个基于Java和Spring Boot的新闻爬虫系统，主要功能包括：

1. 单页新闻爬取
2. 批量新闻爬取（二级爬取和关键词爬取）
3. 新闻导出（Word和PDF格式）
4. 爬取历史记录管理

## 项目结构

项目采用标准的Spring Boot分层架构，主要包括以下几个部分：

### 控制器层 (`controller`)

负责处理HTTP请求和响应，按功能划分为多个专注的控制器：

- `SingleCrawlerController`: 处理单个URL爬取功能
- `BatchCrawlerController`: 处理批量爬取功能（二级爬取和关键词爬取）
- `ExportController`: 处理文件导出功能（Word和PDF）
- `HistoryController`: 处理历史记录查询功能
- `UserController`: 处理用户认证相关功能

### 服务层 (`service`)

实现业务逻辑，包括：

- `NewsCrawlerService`: 核心爬虫服务，实现单页、二级和关键词爬取功能
- `FileExportService`: 文件导出服务，支持Word和PDF格式的导出
- `CrawlHistoryService`: 爬取历史记录服务
- `UserService`: 用户管理服务，处理注册、登录等功能

### 数据访问层 (`repository`)

处理数据持久化，使用Spring Data JPA：

- `NewsDataRepository`: 新闻数据仓库
- `CrawlHistoryRepository`: 爬取历史仓库
- `UserRepository`: 用户数据仓库

### 实体层 (`entity`)

定义数据模型：

- `NewsData`: 新闻数据实体
- `CrawlHistory`: 爬取历史实体
- `User`: 用户实体

### 数据传输对象 (`DTO`)

用于请求和响应的数据封装：

- `RegistrationRequest`: 用户注册请求

### 配置层 (`config`)

包含系统配置类：

- `WebSecurityConfig`: 安全配置

## 代码结构和组织原则

1. **单一职责原则**：每个类和方法只负责一个功能，如控制器被拆分为多个专注的控制器
2. **接口分离原则**：通过接口定义服务，便于扩展和测试
3. **依赖注入**：使用Spring的依赖注入机制实现松耦合
4. **统一日志**：使用SLF4J进行日志记录
5. **异常处理**：每个方法都包含适当的异常处理逻辑
6. **参数验证**：在控制器层进行参数验证，确保数据有效性

## 代码结构改进

在最近的重构中，我们进行了以下改进：

1. 将大型的`CrawlerController`拆分为多个专注的控制器，每个控制器负责特定的功能领域
2. 统一了日志处理方式，使用SLF4J日志框架记录关键操作和异常
3. 改进了异常处理机制，提供更详细的错误信息
4. 增强了代码注释，包括类级别和方法级别的JavaDoc文档
5. 优化了控制器和服务之间的交互方式

## 开发指南

1. 在开发新功能时，应遵循现有的代码结构和命名规范
2. 添加新控制器时，应考虑其职责范围，避免功能重叠
3. 对于大型方法，应考虑拆分为多个小方法，提高可读性和可维护性
4. 所有公共API都应提供完整的JavaDoc注释
5. 修改现有代码时，应确保与现有代码风格保持一致

## 部署结构

本项目采用标准的Spring Boot打包部署方式，生成可执行JAR文件，可以通过以下命令运行：

```
java -jar demo-0.0.1-SNAPSHOT.jar
```

系统默认监听8080端口，可以通过配置文件修改。

## 数据库结构

系统使用MySQL数据库，主要表结构包括：

- `news_data`: 存储爬取的新闻数据
- `crawl_history`: 存储爬取历史记录
- `user`: 存储用户信息

详细的数据库表结构可以参考实体类定义。 