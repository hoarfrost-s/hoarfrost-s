# HyperFetch Mobile 移动端下载管理器规范

## Why

需要一个功能完善的移动端下载管理器，支持多协议（HTTP/HTTPS、HLS、FTP）、多线程（1-9线程）、多任务并发（1-5个）、速度限制、断点续传等核心功能，并提供沉浸式移动端体验。

## What Changes

基于 "My Jetpack Compose" 基础项目，完全重构为 HyperFetch 下载管理器应用：

- 新增下载任务管理模块（创建、暂停、恢复、删除、重试）
- 实现多协议下载引擎（HTTP Range、HLS 流媒体、FTP）
- 实现可配置多线程下载（1-9线程，支持热调整）
- 实现令牌桶限速算法
- 实现多任务调度器（1-5并发）
- 实现后缀分类服务（视频/音频/压缩包/文档/程序/其他）
- 实现 Foreground Service 后台下载
- 实现通知栏进度显示
- 实现 Room 数据库持久化
- 实现 EventBus 事件驱动架构
- 实现暗色主题 UI

## Impact

- **包路径重命名**：从 `com.example.myjetpackcompose` 迁移到 `com.hyperfetch`
- **新增模块**：
  - `com.hyperfetch.service` - 任务服务层
  - `com.hyperfetch.engine` - 下载引擎
  - `com.hyperfetch.scheduler` - 任务调度器
  - `com.hyperfetch.protocol` - 协议处理器
  - `com.hyperfetch.classify` - 分类服务
  - `com.hyperfetch.rate` - 限速器
  - `com.hyperfetch.notification` - 通知管理
  - `com.hyperfetch.repo` - 数据持久化
  - `com.hyperfetch.event` - 事件总线
  - `com.hyperfetch.ui` - UI 层
  - `com.hyperfetch.service.DownloadService` - 前台服务

## ADDED Requirements

### Requirement: 多协议下载支持
系统 SHALL 支持 HTTP/HTTPS 单文件下载、HLS 流媒体下载、FTP/SFTP 下载。

#### Scenario: HTTP 多线程下载
- **WHEN** 用户创建 HTTP URL 下载任务
- **THEN** 系统使用 HTTP Range 请求进行多线程分块下载

#### Scenario: HLS 流媒体下载
- **WHEN** 用户创建 .m3u8 URL 下载任务
- **THEN** 系统解析 m3u8 获取分片列表，并发下载后合并为 MP4

### Requirement: 可配置线程数
系统 SHALL 支持用户配置单任务线程数，范围 [1, 9]，默认 4。

#### Scenario: 线程数热调整
- **WHEN** 用户在下载过程中调整线程数
- **THEN** 系统平滑增减工作线程，正在下载的分片不中断

### Requirement: 速度限制
系统 SHALL 支持全局和单任务限速，使用令牌桶算法，最小 16 KB/s。

#### Scenario: 限速生效
- **WHEN** 用户设置限速值
- **THEN** 下一秒新限速生效，不中断当前下载

### Requirement: 多任务并发
系统 SHALL 支持同时下载多个任务，范围 [1, 5]，默认 3。

#### Scenario: 任务排队
- **WHEN** 同时下载任务数达到上限
- **THEN** 新任务进入排队状态，按优先级和创建时间排序

### Requirement: 断点续传
系统 SHALL 支持暂停后恢复下载，保存分块进度到数据库。

#### Scenario: 应用重启恢复
- **WHEN** 应用被系统回收后重启
- **THEN** 未完成任务自动恢复到暂停状态

### Requirement: 沉浸式体验
系统 SHALL 提供暗色主题、通知栏进度、前台服务保活。

#### Scenario: 后台下载
- **WHEN** 应用退到后台
- **THEN** Foreground Service 持续运行，通知栏显示下载进度

### Requirement: 自动分类
系统 SHALL 根据 URL 后缀自动分类任务到六大类型。

#### Scenario: 后缀识别
- **WHEN** 用户创建任务
- **THEN** 系统根据 URL 末段后缀识别类型（.mp4=视频，.mp3=音频 等）

## MODIFIED Requirements

### Requirement: 项目重构
将基础项目从示例应用重构为功能完整的下载管理器。

- 包名从 `com.example.myjetpackcompose` 改为 `com.hyperfetch`
- minSdk 从 21 提升到 26（API 26+）
- 新增网络、存储、通知等权限

## REMOVED Requirements

### Requirement: 示例代码
**Reason**: 基础模板的示例 GreetingComposable 和相关示例代码不再适用
**Migration**: 完全重构 UI 层实现下载管理器界面

## 技术规格

### 架构
- 分层架构：UI层 → 服务层 → 引擎层 → 基础设施层
- 事件驱动：EventBus 解耦 UI 与内核
- 前台服务：DownloadService 保活

### 数据模型
```
DownloadTask:
  - id: String (UUID)
  - url: String
  - fileName: String
  - totalSize: long
  - downloaded: long
  - category: Category (VIDEO/AUDIO/ARCHIVE/DOCUMENT/PROGRAM/OTHER)
  - status: TaskStatus (QUEUED/DOWNLOADING/PAUSED/COMPLETED/FAILED)
  - priority: Priority (NORMAL/HIGH)
  - threadCount: int (1-9)
  - speedLimit: long
  - savePath: String
  - createTime: Date

TaskChunk:
  - taskId: String
  - index: int
  - start: long
  - end: long
  - downloaded: long
  - status: ChunkStatus
```

### 核心类
- `DownloadEngine`: 分块切分、线程派发、文件合并
- `TaskScheduler`: 多任务并发控制、队列管理
- `TokenBucketLimiter`: 令牌桶限速
- `CategoryService`: 后缀分类识别
- `ProtocolHandler`: 协议处理接口（HttpHandler/HlsHandler/FtpHandler）
- `TaskService`: 任务 CRUD、状态流转
- `EventBus`: 事件发布订阅
