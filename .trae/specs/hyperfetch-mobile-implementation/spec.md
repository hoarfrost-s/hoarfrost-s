# HyperFetch Mobile 下载管理器实现规格

## Why
为 Android 移动端用户提供一款支持多协议、多类型、可配置多线程、多任务并发的沉浸式下载工具，满足用户在手机端高效下载各类文件的需求。

## What Changes
- 新建完整的 Android 项目结构，采用分层架构 + 事件总线设计
- 实现多协议下载引擎（HTTP/HLS/FTP）
- 实现多线程分块下载与断点续传
- 实现多任务并发调度（最多5个任务同时下载）
- 实现后缀自动分类系统
- 实现令牌桶限速算法
- 实现前台服务保活与通知栏进度显示
- 实现完整的 UI 界面（暗色主题、底部导航、任务卡片）

## Impact
- 新建项目：`app/src/main/java/com/hyperfetch/` 目录结构
- 涉及模块：UI、Service、Scheduler、Engine、Protocol、Classify、Rate、Notification、Repo、Event

## ADDED Requirements

### Requirement: 多协议下载引擎
系统 SHALL 提供统一的下载抽象层，支持 HTTP Range 多线程下载、HLS 流媒体分片下载合并、FTP 协议下载。

#### Scenario: HTTP 多线程下载成功
- **WHEN** 用户创建 HTTP 下载任务并设置线程数为 4
- **THEN** 系统将文件等分为 4 块，并发下载，最终合并为完整文件

#### Scenario: HLS 流媒体下载成功
- **WHEN** 用户下载 .m3u8 文件
- **THEN** 系统解析分片列表，并发下载所有 .ts 分片，合并为 .mp4 文件

### Requirement: 多任务并发调度
系统 SHALL 支持最多 5 个任务同时下载，超出上限的任务进入排队状态。

#### Scenario: 并发上限控制
- **WHEN** 用户设置最大并发数为 3 且有 5 个任务
- **THEN** 3 个任务进入 DOWNLOADING 状态，2 个任务进入 QUEUED 状态

#### Scenario: 任务完成自动调度
- **WHEN** 一个下载任务完成
- **THEN** 系统自动从队列取出下一个任务开始下载

### Requirement: 线程数可配置
系统 SHALL 支持单任务线程数在 [1, 9] 区间内配置，默认值为 4。

#### Scenario: 线程数热调整
- **WHEN** 用户在下载过程中修改线程数
- **THEN** 引擎平滑增减工作线程，正在下载的分片不中断

### Requirement: 速度限制
系统 SHALL 支持全局或单任务限速，最低 16 KB/s。

#### Scenario: 限速生效
- **WHEN** 用户设置限速为 100 KB/s
- **THEN** 下载速度不超过 100 KB/s，下一秒生效

### Requirement: 后缀自动分类
系统 SHALL 根据 URL 后缀或 Content-Type 自动将任务归类为视频/音频/压缩包/文档/程序/其他。

#### Scenario: 后缀识别分类
- **WHEN** 用户下载 .mp4 文件
- **THEN** 系统自动归类为 VIDEO 分类

### Requirement: 前台服务保活
系统 SHALL 使用 Foreground Service 保证后台持续下载，防止系统回收。

#### Scenario: 后台下载保活
- **WHEN** 用户退到后台或锁屏
- **THEN** 下载任务继续执行，通知栏显示进度

### Requirement: 断点续传
系统 SHALL 支持网络切换、进程被杀后自动恢复下载。

#### Scenario: 网络恢复续传
- **WHEN** 网络断开后恢复
- **THEN** 系统自动从断点位置继续下载

### Requirement: 沉浸式 UI
系统 SHALL 提供暗色主题、底部 Tab 导航、任务卡片列表、通知栏进度显示。

#### Scenario: UI 响应
- **WHEN** 用户操作 UI
- **THEN** 响应时间 <= 100ms，主线程零阻塞

## MODIFIED Requirements
无修改需求，全部为新增功能。

## REMOVED Requirements
无移除需求。