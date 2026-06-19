# Checklist - HyperFetch Mobile 下载管理器验收清单

## Phase 1: 项目基础重构

  - [x] 包名已修改为 com.hyperfetch
  - [x] 应用名称已修改为 HyperFetch
  - [x] minSdk 已提升到 26
  - [x] Room 数据库依赖已添加
  - [x] OkHttp 网络依赖已添加
  - [x] EventBus 事件总线依赖已添加
  - [x] WorkManager 后台任务依赖已添加
  - [x] AndroidManifest 已配置网络权限
  - [x] AndroidManifest 已配置存储权限
  - [x] AndroidManifest 已声明 Foreground Service
  - [x] AndroidManifest 已配置通知权限

## Phase 2: 数据模型与持久化

  - [x] Category 枚举定义完整 (VIDEO/AUDIO/ARCHIVE/DOCUMENT/PROGRAM/OTHER)
  - [x] TaskStatus 枚举定义完整 (QUEUED/DOWNLOADING/PAUSED/COMPLETED/FAILED)
  - [x] Priority 枚举定义完整 (NORMAL/HIGH)
  - [x] DownloadTask 数据类包含所有必需字段
  - [x] TaskChunk 数据类包含所有必需字段
  - [x] SchedulerConfig 配置类定义正确
  - [x] DownloadTaskEntity Room 实体正确
  - [x] TaskChunkEntity Room 实体正确
  - [x] DownloadTaskDao 数据访问接口完整
  - [x] AppDatabase 数据库类配置正确
  - [x] TaskRepository 仓库类实现 CRUD 操作

## Phase 3: 事件总线

  - [x] EventBusWrapper 封装类实现
  - [x] TaskCreatedEvent 事件类实现
  - [x] ProgressEvent 事件类实现 (包含 downloaded, total, speed)
  - [x] StatusChangedEvent 事件类实现 (包含 from, to 状态)
  - [x] ErrorEvent 事件类实现 (包含 cause, retryCount)
  - [x] CompletedEvent 事件类实现 (包含 file 路径)
  - [x] SchedulerConfigChangedEvent 事件类实现

## Phase 4: 核心引擎实现

  - [x] ProtocolHandler 接口定义正确
  - [x] ResourceMeta 元数据类包含 totalSize, supportRange
  - [x] ProgressCallback 回调接口定义正确
  - [x] HttpHandler 实现 HTTP Range 请求
  - [x] HlsHandler 实现 m3u8 解析和分片下载
  - [x] FtpHandler 实现 FTP 协议下载
  - [x] ProtocolFactory 工厂类实现协议选择
  - [x] TokenBucketLimiter 令牌桶算法实现
  - [x] acquire() 令牌获取方法正确
  - [x] setRate() 动态限速调整正确
  - [x] 最小速率 16 KB/s 限制生效
  - [x] DownloadEngine 分块策略正确
  - [x] 多线程下载协调逻辑正确
  - [x] RandomAccessFile 分块写入正确
  - [x] 文件合并逻辑正确
  - [x] 线程池管理正确
  - [x] 错误重试逻辑 (指数退避) 正确
  - [x] TaskScheduler enqueue() 入队正确
  - [x] TaskScheduler onTaskFinished() 回调正确
  - [x] TaskScheduler tryScheduleNext() 调度正确
  - [x] 并发数调整正确 (1-5范围)
  - [x] 优先级队列排序正确

## Phase 5: 服务层

  - [x] CategoryService 后缀映射配置完整
  - [x] 自动分类方法正确
  - [x] Content-Type 备用识别正确
  - [x] TaskService createTask() 创建正确
  - [x] TaskService start()/pause()/resume() 状态控制正确
  - [x] TaskService delete() 删除正确
  - [x] TaskService setThreadCount() 线程调整正确
  - [x] TaskService setSpeedLimit() 限速调整正确
  - [x] TaskService list() 任务列表查询正确
  - [x] 任务恢复逻辑正确
  - [x] DownloadService onCreate() 初始化正确
  - [x] DownloadService onStartCommand() 指令处理正确
  - [x] DownloadService startForeground() 前台通知正确
  - [x] Binder 通信接口正确
  - [x] START_STICKY 自动重启配置正确

## Phase 6: 通知管理

  - [x] 通知渠道创建正确
  - [x] 进度更新通知正确显示速度和进度
  - [x] 下载完成通知正确
  - [x] 通知点击打开应用正确

## Phase 7: UI 层实现

  - [x] Color.kt 暗色配色定义正确
  - [x] Theme.kt Material3 暗色主题配置正确
  - [x] 分类 Tab 切换正确 (全部/视频/音频/压缩包/其他)
  - [x] 任务卡片列表正确显示
  - [x] 底部统计栏正确显示 (并发数/总速度/任务数)
  - [x] 浮动新建按钮 (FAB) 功能正确
  - [x] TaskCard 显示文件类型图标和名称
  - [x] TaskCard 显示下载状态徽章
  - [x] TaskCard 显示进度条和下载速度
  - [x] TaskCard 操作按钮功能正确
  - [x] 新建任务 BottomSheet URL 输入正确
  - [x] 新建任务 BottomSheet 路径选择器正确
  - [x] 新建任务 BottomSheet 线程数滑块 [1-9] 正确
  - [x] 新建任务 BottomSheet 速度限制正确
  - [x] 新建任务 BottomSheet 优先级选择正确
  - [x] 设置页面多任务开关正确
  - [x] 设置页面最大并发数 [1-5] 正确
  - [x] 设置页面仅 Wi-Fi 下载开关正确
  - [x] 电池优化白名单引导正确
  - [x] Wi-Fi 切换暂停/恢复逻辑正确
  - [x] 网络恢复自动续传正确

## Phase 8: 集成与测试

  - [x] EventBus 订阅正确
  - [x] ProgressEvent 实时更新 UI
  - [x] StatusChangedEvent 状态同步正确
  - [x] CompletedEvent 完成通知正确
  - [x] IntentFilter 分享接收正确
  - [x] 剪贴板 URL 获取正确
  - [x] Gradle sync 成功
  - [x] Debug build 成功

## 功能验收

  - [x] HTTP 多线程下载正常工作
  - [x] HLS 流媒体下载正常工作
  - [x] 线程数热调整平滑
  - [x] 速度限制生效
  - [x] 多任务并发下载正确
  - [x] 断点续传正确
  - [x] 后台下载正确
  - [x] 通知栏进度显示正确
  - [x] 应用重启后任务恢复正确
  - [x] 后缀自动分类正确
