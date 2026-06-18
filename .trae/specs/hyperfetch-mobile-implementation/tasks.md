# Tasks

## Phase 1: 项目基础架构搭建
- [x] Task 1: 创建 Android 项目结构与基础配置
  - [x] SubTask 1.1: 创建 Gradle 项目配置（build.gradle, settings.gradle）
  - [x] SubTask 1.2: 创建 AndroidManifest.xml 配置（权限、Service声明）
  - [x] SubTask 1.3: 创建包结构目录（ui, service, scheduler, engine, protocol, classify, rate, notification, repo, event）

- [x] Task 2: 实现数据模型与持久化层
  - [x] SubTask 2.1: 创建 DownloadTask 实体类（id, url, fileName, totalSize, downloaded, category, status, priority, threadCount, speedLimit, savePath, createTime）
  - [x] SubTask 2.2: 创建 TaskChunk 实体类（taskId, index, start, end, downloaded, status）
  - [x] SubTask 2.3: 创建 Category/TaskStatus/Priority 枚举类
  - [x] SubTask 2.4: 创建 SchedulerConfig 配置类（maxConcurrentTasks, multiTaskEnabled, queueStrategy）
  - [x] SubTask 2.5: 实现 Room 数据库（DownloadTaskEntity, TaskChunkEntity, ConfigEntity）
  - [x] SubTask 2.6: 实现 TaskRepository DAO 接口

## Phase 2: 下载核心引擎
- [x] Task 3: 实现协议处理器接口与 HTTP Handler
  - [x] SubTask 3.1: 创建 ProtocolHandler 接口（supports, probe, download）
  - [x] SubTask 3.2: 实现 HttpHandler（Range 请求、分块下载）
  - [x] SubTask 3.3: 实现 ResourceMeta 类（totalSize, supportRange）

- [x] Task 4: 实现多线程下载引擎
  - [x] SubTask 4.1: 创建 DownloadEngine 类（分块计算 splitChunks）
  - [x] SubTask 4.2: 实现双层线程池（任务调度池 + 分块工作池）
  - [x] SubTask 4.3: 实现 RandomAccessFile + FileChannel 写入策略
  - [x] SubTask 4.4: 实现线程数热调整功能（setCorePoolSize）

- [x] Task 5: 实现令牌桶限速器
  - [x] SubTask 5.1: 创建 TokenBucketLimiter 类
  - [x] SubTask 5.2: 实现 acquire 方法（令牌申领与阻塞）
  - [x] SubTask 5.3: 实现 setRate 方法（最小速率 16 KB/s 约束）
  - [x] SubTask 5.4: 实现 refill 方法（令牌补充）

- [x] Task 6: 实现 HLS Handler
  - [x] SubTask 6.1: 实现 HlsHandler（m3u8 解析、分片下载）
  - [x] SubTask 6.2: 实现分片合并逻辑（保留 .ts 文件作为 fallback）

## Phase 3: 任务调度与服务层
- [x] Task 7: 实现任务调度器
  - [x] SubTask 7.1: 创建 TaskScheduler 类（PriorityQueue 等待队列）
  - [x] SubTask 7.2: 实现 enqueue 方法（任务入队）
  - [x] SubTask 7.3: 实现 tryScheduleNext 方法（并发上限控制）
  - [x] SubTask 7.4: 实现 setMaxConcurrentTasks 方法（1-5 区间）

- [x] Task 8: 实现任务服务层
  - [x] SubTask 8.1: 创建 TaskService 类
  - [x] SubTask 8.2: 实现 createTask 方法（URL 解析、自动分类）
  - [x] SubTask 8.3: 实现 start/pause/delete 方法
  - [x] SubTask 8.4: 实现 setThreadCount/setSpeedLimit 方法
  - [x] SubTask 8.5: 实现 list 方法（LiveData 返回）

- [x] Task 9: 实现任务状态机
  - [x] SubTask 9.1: 定义状态流转规则（QUEUED -> DOWNLOADING -> PAUSED/COMPLETED/FAILED）
  - [x] SubTask 9.2: 实现状态变更事件发布

## Phase 4: 后缀分类系统
- [x] Task 10: 实现分类服务
  - [x] SubTask 10.1: 创建 CategoryService 类
  - [x] SubTask 10.2: 实现后缀映射表（mp4/m3u8 -> VIDEO, mp3/flac -> AUDIO, zip/rar -> ARCHIVE 等）
  - [x] SubTask 10.3: 实现双阶段识别（后缀优先，Content-Type fallback）
  - [x] SubTask 10.4: 实现 HEAD 请求获取 Content-Type

## Phase 5: 事件总线与通知
- [x] Task 11: 实现事件总线
  - [x] SubTask 11.1: 创建 EventBus 类（发布/订阅）
  - [x] SubTask 11.2: 定义事件类型（TaskCreatedEvent, ProgressEvent, StatusChangedEvent, ErrorEvent, CompletedEvent）
  - [x] SubTask 11.3: 实现主线程 Handler 派发 UI 事件
  - [x] SubTask 11.4: 实现子线程 Executor 派发通知更新

- [x] Task 12: 实现通知管理
  - [x] SubTask 12.1: 创建 NotificationManager 类
  - [x] SubTask 12.2: 实现前台服务通知（startForeground）
  - [x] SubTask 12.3: 实现进度通知更新（setProgress）
  - [x] SubTask 12.4: 创建通知渠道（NotificationChannel）

- [x] Task 13: 实现 DownloadService（Foreground Service）
  - [x] SubTask 13.1: 创建 DownloadService 类
  - [x] SubTask 13.2: 实现 onCreate（启动前台通知）
  - [x] SubTask 13.3: 实现 onStartCommand（接收 UI 指令）
  - [x] SubTask 13.4: 实现 START_STICKY 重启策略

## Phase 6: UI 界面实现
- [x] Task 14: 实现主界面布局
  - [x] SubTask 14.1: 创建 MainActivity
  - [x] SubTask 14.2: 实现底部 Tab 导航（全部/视频/音频/压缩包/其他）
  - [x] SubTask 14.3: 实现任务卡片列表（RecyclerView + Adapter）
  - [x] SubTask 14.4: 实现底部统计栏（并发数/总速度/任务数）
  - [x] SubTask 14.5: 实现浮动新建按钮（FAB）

- [x] Task 15: 实现任务卡片组件
  - [x] SubTask 15.1: 创建任务卡片布局（类型徽章、文件名、状态、进度条、速度、操作按钮）
  - [x] SubTask 15.2: 实现进度条渐变流光动画
  - [x] SubTask 15.3: 实现状态徽章样式（下载中/已暂停/已完成/排队）

- [x] Task 16: 实现新建任务对话框（BottomSheet）
  - [x] SubTask 16.1: 创建 NewTaskSheet BottomSheet
  - [x] SubTask 16.2: 实现下载链接输入（剪贴板自动填充）
  - [x] SubTask 16.3: 实现保存路径选择器
  - [x] SubTask 16.4: 实现线程数滑块（1-9）
  - [x] SubTask 16.5: 实现速度限制开关与数值输入
  - [x] SubTask 16.6: 实现优先级选择（普通/高）

- [x] Task 17: 实现设置页面
  - [x] SubTask 17.1: 创建 SettingsFragment
  - [x] SubTask 17.2: 实现多任务开关与最大并发数设置
  - [x] SubTask 17.3: 实现排队策略选择（优先级优先/创建时间优先）
  - [x] SubTask 17.4: 实现仅 Wi-Fi 下载开关
  - [x] SubTask 17.5: 实现电池优化白名单引导

- [x] Task 18: 实现暗色主题样式
  - [x] SubTask 18.1: 定义颜色变量（bg, bg2, ink, muted, rule, accent, accent2, danger, ok）
  - [x] SubTask 18.2: 创建 styles.xml 暗色主题
  - [x] SubTask 18.3: 实现渐变背景效果

## Phase 7: 异常处理与恢复
- [x] Task 19: 实现异常重试机制
  - [x] SubTask 19.1: 实现网络瞬断重试（最多 5 次，指数退避）
  - [x] SubTask 19.2: 实现 HTTP 4xx 直接失败逻辑
  - [x] SubTask 19.3: 实现 HTTP 5xx 重置分块重试

- [x] Task 20: 实现 WorkManager 兜底恢复
  - [x] SubTask 20.1: 创建 DownloadRecoveryWorker
  - [x] SubTask 20.2: 实现进程被杀后自动恢复逻辑

## Phase 8: 测试与验证
- [x] Task 21: 单元测试
  - [x] SubTask 21.1: TokenBucketLimiter 单元测试
  - [x] SubTask 21.2: TaskScheduler 单元测试
  - [x] SubTask 21.3: CategoryService 单元测试
  - [x] SubTask 21.4: DownloadEngine 分块计算测试

- [x] Task 22: 集成测试
  - [x] SubTask 22.1: HTTP 多线程下载测试
  - [x] SubTask 22.2: HLS 流媒体下载测试
  - [x] SubTask 22.3: 多任务并发测试
  - [x] SubTask 22.4: 断点续传测试

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3-6 依赖 Task 2
- Task 7-9 依赖 Task 4
- Task 10 依赖 Task 2
- Task 11-13 依赖 Task 7-9
- Task 14-18 依赖 Task 11
- Task 19-20 依赖 Task 13
- Task 21-22 依赖所有前置任务完成