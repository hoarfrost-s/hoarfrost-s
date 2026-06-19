# Java 多线程下载管理器 — 移动端实现 Spec

## Why
构建一款面向 Android 手机端的沉浸式多线程下载管理器，解决移动端下载任务管理分散、下载效率低、后台下载中断等痛点。严格遵循工程交付规约的架构设计、模块划分和接口定义。

## What Changes
- 从零构建完整的 Android 多模块 Gradle 项目
- 实现 MVVM + Clean Architecture 分层架构
- 实现多线程分段下载引擎（1-9 线程，支持断点续传）
- 实现 FIFO 任务调度器（1-5 并发，Semaphore + Channel）
- 实现全局共享令牌桶限速器
- 实现文件自动分类系统（7 类别，5 级解析策略）
- 实现 M3U8 流媒体解析与分片下载
- 实现 Foreground Service 后台下载 + 通知栏进度
- 实现 Material 3 + Jetpack Compose 沉浸式 UI
- 实现分块进度条展示（每个线程独立进度可视化）
- 实现链接信息解析与详情查看功能
- 实现 Room 持久化 + DataStore 配置

## Impact
- Affected specs: 无（全新项目）
- Affected code: 全新创建所有模块

## ADDED Requirements

### Requirement: 多格式下载支持
系统 SHALL 支持视频、音频、压缩包、文档、图片、可执行文件、其他共 7 类文件格式的下载，根据 URL 资源类型自动匹配对应的下载策略。对于标准 HTTP 文件采用 Range 分段下载；对于 m3u8 流媒体先解析索引文件获取 ts 分片列表，再逐片下载并合并。

#### Scenario: 标准 HTTP 文件下载
- **WHEN** 用户添加一个 mp4 文件的 URL
- **THEN** 系统自动识别为视频类别，使用 HTTP Range 分段下载

#### Scenario: M3U8 流媒体下载
- **WHEN** 用户添加一个 m3u8 格式的 URL
- **THEN** 系统解析 m3u8 索引文件，获取 ts 分片列表，逐片下载后合并为 mp4

#### Scenario: 服务器不支持 Range
- **WHEN** 服务器不支持 Range 请求
- **THEN** 系统自动回退为单线程下载

### Requirement: 文件自动分类
系统 SHALL 依据文件后缀名自动归类至七个类别目录（视频/音频/压缩包/文档/图片/可执行/其他），采用 5 级解析策略（URL 后缀 → Content-Disposition → Content-Type → MimeTypeMap → 默认回退）。

#### Scenario: URL 包含明确后缀
- **WHEN** 用户输入 URL "https://example.com/video.mp4"
- **THEN** 系统解析后缀为 mp4，归类为"视频"，保存到 Videos/ 目录

#### Scenario: URL 无后缀
- **WHEN** 用户输入无后缀的 URL
- **THEN** 系统通过 HEAD 请求获取 Content-Type，推断文件类别

### Requirement: 多线程分段下载引擎
系统 SHALL 支持单任务 1-9 线程分段下载（默认 4），通过 SegmentDispatcher 将文件按线程数切分为多个数据段，使用 OkHttp Range 请求并行下载。每个并行任务拥有独立的临时文件目录。

#### Scenario: 4 线程分段下载
- **WHEN** 用户使用默认 4 线程下载一个 100MB 文件
- **THEN** 系统将文件切分为 4 段，并行下载，每段约 25MB

#### Scenario: 调整线程数
- **WHEN** 用户在设置中将线程数调整为 8
- **THEN** 后续新建任务使用 8 线程分段下载

### Requirement: 断点续传
系统 SHALL 在每个下载任务维护分段元数据，任务暂停或异常中断后，再次启动时从已下载位置继续传输。

#### Scenario: 网络中断后恢复
- **WHEN** 下载过程中网络断开
- **THEN** 网络恢复后，系统从已下载字节位置继续，不重新下载

### Requirement: 全局令牌桶限速
系统 SHALL 通过全局共享的 TokenBucketRateLimiter 实现下载速度控制。用户可设定全局下载速度上限（最小 16KB/s，最大无上限）。所有并行任务的所有下载线程共享同一个限速器实例。

#### Scenario: 设置速度限制
- **WHEN** 用户设置全局限速为 1MB/s
- **THEN** 所有下载任务的总带宽不超过 1MB/s

#### Scenario: 不限速
- **WHEN** 用户设置限速为 0（不限速）
- **THEN** 所有下载任务以最大可用带宽运行

### Requirement: 任务管理
系统 SHALL 支持通过 URL 输入、系统分享菜单、剪贴板识别三种方式添加下载任务。任务状态流转：WAITING → QUEUED → DOWNLOADING → MERGING → COMPLETED，异常状态：PAUSED / FAILED / CANCELLED。支持长按多选、批量操作、按状态/分类筛选、按文件名搜索。

#### Scenario: 添加下载任务
- **WHEN** 用户点击 FAB 按钮，输入 URL
- **THEN** 系统解析 URL 信息，显示文件名、分类、大小预览，用户确认后开始下载

#### Scenario: 任务状态流转
- **WHEN** 用户添加任务时槽位已满
- **THEN** 任务状态为 QUEUED，等待前序任务完成后自动开始

#### Scenario: 批量操作
- **WHEN** 用户长按任务卡片进入多选模式，选择多个任务并点击暂停
- **THEN** 所有选中任务被暂停

### Requirement: 链接信息解析与详情查看
系统 SHALL 在用户创建下载任务后自动解析链接，提供文件名、文件大小、文件类型、分类、Content-Type 等信息。下载任务开始后，用户可通过任务详情按钮查看这些链接信息。

#### Scenario: 添加任务时预览链接信息
- **WHEN** 用户输入 URL 后
- **THEN** 系统自动发起 HEAD 请求，展示文件名、大小、类型、分类等信息

#### Scenario: 下载中查看详情
- **WHEN** 用户点击下载中任务的详情按钮
- **THEN** 系统展示链接信息（URL、文件名、大小、类型、分类、线程数、保存路径、创建时间、Content-Type 等）

### Requirement: 多任务并行调度
系统 SHALL 支持 1-5 个任务同时下载（默认 3），采用 FIFO 公平调度。超出并发上限的任务自动排队。暂停不释放槽位，完成/失败/取消后立即释放。

#### Scenario: 并发数满时新任务排队
- **WHEN** 并发数设为 3，已有 3 个任务下载中，用户添加第 4 个任务
- **THEN** 第 4 个任务进入 QUEUED 状态，等待槽位释放

#### Scenario: 任务完成后自动调度
- **WHEN** 一个下载中的任务完成
- **THEN** 槽位释放，队列中第一个任务自动开始下载

### Requirement: 分块进度条
系统 SHALL 为每个下载任务展示分块进度条，直观反映各线程的下载进度。例如 4 线程下载时显示 4 段独立进度条，每段代表一个线程的下载进度。

#### Scenario: 4 线程下载进度展示
- **WHEN** 一个任务使用 4 线程下载
- **THEN** 任务卡片上显示 4 段分块进度条，每段独立展示对应线程的下载进度

#### Scenario: 单线程下载
- **WHEN** 一个任务使用单线程下载
- **THEN** 任务卡片上显示单一连续进度条

### Requirement: 美观进度条设计
系统 SHALL 提供美观、直观的进度条设计。进度条支持渐变色、流畅动画、圆角端点，使用 Material You 风格调色板。分块进度条各段之间使用细微间隙分隔，已完成段显示主题色，下载中段显示动画效果，未开始段显示浅色背景。

#### Scenario: 进度条动画
- **WHEN** 下载进行中
- **THEN** 进度条使用平滑过渡动画更新进度，下载中段有呼吸/脉冲动画效果

### Requirement: 后台下载与前台服务
系统 SHALL 通过 Foreground Service + 持久通知实现应用退后台后下载不中断。通知栏显示全局下载进度、速率、任务数，支持快捷暂停/恢复操作。Service 使用 START_STICKY 保证被杀后自动重启。

#### Scenario: 应用退到后台
- **WHEN** 用户按 Home 键或切换到其他应用
- **THEN** 下载继续在后台进行，通知栏显示下载进度

#### Scenario: 从通知栏控制
- **WHEN** 用户点击通知栏的暂停按钮
- **THEN** 所有下载任务暂停

### Requirement: 网络感知
系统 SHALL 监听网络状态变化。WiFi 断开时自动暂停所有任务（可配置），蜂窝网络下可设置自动限速，网络恢复后自动继续。

#### Scenario: WiFi 断开切换到蜂窝网络
- **WHEN** WiFi 断开，设备切换到蜂窝网络
- **THEN** 如开启蜂窝网络自动限速，则应用限速；否则继续下载

### Requirement: 数据持久化
系统 SHALL 使用 Room Database 持久化任务数据，使用 DataStore 存储配置偏好。进程被杀后重启可恢复未完成任务。

#### Scenario: 应用重启后恢复
- **WHEN** 应用被系统杀死后重新打开
- **THEN** 之前的任务列表从 Room 恢复，未完成任务可继续下载

### Requirement: UI 布局
系统 SHALL 采用 Material 3 设计语言，包含首页（任务列表）、分类浏览页、设置页、任务详情页、添加任务 BottomSheet。支持深色/浅色/跟随系统主题。

#### Scenario: 首页浏览
- **WHEN** 用户打开应用
- **THEN** 显示任务列表页面，包含搜索栏、筛选芯片、任务卡片列表、FAB 添加按钮、底部导航栏

#### Scenario: 设置页操作
- **WHEN** 用户进入设置页
- **THEN** 可调整线程数、并行数、限速、保存路径、主题等配置

## MODIFIED Requirements
无（全新项目）

## REMOVED Requirements
无（全新项目）