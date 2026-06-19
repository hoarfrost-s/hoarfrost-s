# Tasks - HyperFetch Mobile 下载管理器

## Phase 1: 项目基础重构

- [x] Task 1.1: 重命名包名和应用配置
  - [ ] SubTask 1.1.1: 修改 namespace 从 com.example.myjetpackcompose 到 com.hyperfetch
  - [ ] SubTask 1.1.2: 更新 applicationId 和应用名称为 HyperFetch
  - [ ] SubTask 1.1.3: 提升 minSdk 从 21 到 26

- [x] Task 1.2: 添加核心依赖库
  - [ ] SubTask 1.2.1: 添加 Room 数据库依赖
  - [ ] SubTask 1.2.2: 添加 OkHttp 网络依赖
  - [ ] SubTask 1.2.3: 添加 EventBus 事件总线依赖
  - [ ] SubTask 1.2.4: 添加 WorkManager 后台任务依赖

- [x] Task 1.3: 配置 AndroidManifest 权限
  - [ ] SubTask 1.3.1: 添加网络访问权限 (INTERNET, ACCESS_NETWORK_STATE)
  - [ ] SubTask 1.3.2: 添加存储访问权限 (READ/WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE)
  - [ ] SubTask 1.3.3: 添加前台服务权限和声明
  - [ ] SubTask 1.3.4: 添加通知权限

## Phase 2: 数据模型与持久化

- [x] Task 2.1: 定义核心数据模型
  - [ ] SubTask 2.1.1: 创建 Category 枚举 (VIDEO/AUDIO/ARCHIVE/DOCUMENT/PROGRAM/OTHER)
  - [ ] SubTask 2.1.2: 创建 TaskStatus 枚举 (QUEUED/DOWNLOADING/PAUSED/COMPLETED/FAILED)
  - [ ] SubTask 2.1.3: 创建 Priority 枚举 (NORMAL/HIGH)
  - [ ] SubTask 2.1.4: 创建 DownloadTask 数据类
  - [ ] SubTask 2.1.5: 创建 TaskChunk 数据类
  - [ ] SubTask 2.1.6: 创建 SchedulerConfig 配置类

- [x] Task 2.2: 实现 Room 数据库
  - [ ] SubTask 2.2.1: 创建 DownloadTaskEntity 实体
  - [ ] SubTask 2.2.2: 创建 TaskChunkEntity 实体
  - [ ] SubTask 2.2.3: 创建 DownloadTaskDao 数据访问对象
  - [ ] SubTask 2.2.4: 创建 AppDatabase 数据库类
  - [ ] SubTask 2.2.5: 实现 TaskRepository 仓库类

## Phase 3: 事件总线

- [x] Task 3.1: 实现 EventBus 模块
  - [ ] SubTask 3.1.1: 创建 EventBusWrapper 封装类
  - [ ] SubTask 3.1.2: 创建 TaskCreatedEvent 事件
  - [ ] SubTask 3.1.3: 创建 ProgressEvent 事件
  - [ ] SubTask 3.1.4: 创建 StatusChangedEvent 事件
  - [ ] SubTask 3.1.5: 创建 ErrorEvent 事件
  - [ ] SubTask 3.1.6: 创建 CompletedEvent 事件
  - [ ] SubTask 3.1.7: 创建 SchedulerConfigChangedEvent 事件

## Phase 4: 核心引擎实现

- [x] Task 4.1: 实现 ProtocolHandler 协议接口
  - [ ] SubTask 4.1.1: 创建 ProtocolHandler 接口
  - [ ] SubTask 4.1.2: 实现 ResourceMeta 元数据类
  - [ ] SubTask 4.1.3: 实现 ProgressCallback 回调接口
  - [ ] SubTask 4.1.4: 实现 HttpHandler HTTP协议处理器
  - [ ] SubTask 4.1.5: 实现 HlsHandler HLS流媒体处理器
  - [ ] SubTask 4.1.6: 实现 FtpHandler FTP协议处理器
  - [ ] SubTask 4.1.7: 实现 ProtocolFactory 工厂类

- [x] Task 4.2: 实现 TokenBucketLimiter 限速器
  - [ ] SubTask 4.2.1: 实现令牌桶核心算法
  - [ ] SubTask 4.2.2: 实现 acquire() 令牌获取方法
  - [ ] SubTask 4.2.3: 实现 setRate() 动态限速调整
  - [ ] SubTask 4.2.4: 添加最小速率限制 (16 KB/s)

- [x] Task 4.3: 实现 DownloadEngine 下载引擎
  - [ ] SubTask 4.3.1: 实现 splitChunks() 分块策略
  - [ ] SubTask 4.3.2: 实现多线程下载协调
  - [ ] SubTask 4.3.3: 实现 RandomAccessFile 分块写入
  - [ ] SubTask 4.3.4: 实现文件合并逻辑
  - [ ] SubTask 4.3.5: 实现线程池管理
  - [ ] SubTask 4.3.6: 实现错误重试逻辑 (指数退避)

- [x] Task 4.4: 实现 TaskScheduler 任务调度器
  - [ ] SubTask 4.4.1: 实现 enqueue() 入队方法
  - [ ] SubTask 4.4.2: 实现 onTaskFinished() 任务完成回调
  - [ ] SubTask 4.4.3: 实现 tryScheduleNext() 调度逻辑
  - [ ] SubTask 4.4.4: 实现 setMaxConcurrentTasks() 并发数调整
  - [ ] SubTask 4.4.5: 实现优先级队列排序

## Phase 5: 服务层

- [x] Task 5.1: 实现 CategoryService 分类服务
  - [ ] SubTask 5.1.1: 定义后缀到分类的映射配置
  - [ ] SubTask 5.1.2: 实现自动分类方法
  - [ ] SubTask 5.1.3: 支持 Content-Type 备用识别

- [x] Task 5.2: 实现 TaskService 任务服务
  - [ ] SubTask 5.2.1: 实现 createTask() 创建任务
  - [ ] SubTask 5.2.2: 实现 start()/pause()/resume() 状态控制
  - [ ] SubTask 5.2.3: 实现 delete() 删除任务
  - [ ] SubTask 5.2.4: 实现 setThreadCount() 线程调整
  - [ ] SubTask 5.2.5: 实现 setSpeedLimit() 限速调整
  - [ ] SubTask 5.2.6: 实现 list() 任务列表查询
  - [ ] SubTask 5.2.7: 实现任务恢复逻辑

- [x] Task 5.3: 实现 DownloadService 前台服务
  - [ ] SubTask 5.3.1: 实现 onCreate() 初始化引擎
  - [ ] SubTask 5.3.2: 实现 onStartCommand() 接收指令
  - [ ] SubTask 5.3.3: 实现 startForeground() 前台通知
  - [ ] SubTask 5.3.4: 实现 Binder 通信接口
  - [ ] SubTask 5.3.5: 实现 START_STICKY 自动重启

## Phase 6: 通知管理

- [x] Task 6.1: 实现通知栏功能
  - [ ] SubTask 6.1.1: 创建通知渠道
  - [ ] SubTask 6.1.2: 实现进度更新通知
  - [ ] SubTask 6.1.3: 实现下载完成通知
  - [ ] SubTask 6.1.4: 实现通知点击打开应用

## Phase 7: UI 层实现

- [x] Task 7.1: 实现暗色主题
  - [ ] SubTask 7.1.1: 配置 Color.kt 暗色配色
  - [ ] SubTask 7.1.2: 配置 Theme.kt Material3 暗色主题

- [x] Task 7.2: 实现主界面 (MainScreen)
  - [ ] SubTask 7.2.1: 实现分类 Tab 切换 (全部/视频/音频/压缩包/其他)
  - [ ] SubTask 7.2.2: 实现任务卡片列表 (TaskCard)
  - [ ] SubTask 7.2.3: 实现底部统计栏 (并发数/总速度/任务数)
  - [ ] SubTask 7.2.4: 实现浮动新建按钮 (FAB)

- [x] Task 7.3: 实现任务卡片组件 (TaskCard)
  - [ ] SubTask 7.3.1: 显示文件类型图标和名称
  - [ ] SubTask 7.3.2: 显示下载状态徽章
  - [ ] SubTask 7.3.3: 显示进度条和下载速度
  - [ ] SubTask 7.3.4: 实现操作按钮 (暂停/继续/删除)

- [x] Task 7.4: 实现新建任务 BottomSheet
  - [ ] SubTask 7.4.1: 实现 URL 输入框 (支持粘贴)
  - [ ] SubTask 7.4.2: 实现保存路径选择器
  - [ ] SubTask 7.4.3: 实现分类下拉选择
  - [ ] SubTask 7.4.4: 实现线程数滑块 [1-9]
  - [ ] SubTask 7.4.5: 实现速度限制开关和数值
  - [ ] SubTask 7.4.6: 实现优先级选择
  - [ ] SubTask 7.4.7: 实现立即开始开关

- [x] Task 7.5: 实现设置页面 (SettingsScreen)
  - [ ] SubTask 7.5.1: 实现多任务开关
  - [ ] SubTask 7.5.2: 实现最大并发数滑块 [1-5]
  - [ ] SubTask 7.5.3: 实现排队策略选择
  - [ ] SubTask 7.5.4: 实现仅 Wi-Fi 下载开关
  - [ ] SubTask 7.5.5: 实现电池优化白名单引导

- [x] Task 7.6: 实现网络状态监听
  - [ ] SubTask 7.6.1: 实现 Wi-Fi 切换暂停/恢复逻辑
  - [ ] SubTask 7.6.2: 实现网络恢复自动续传

## Phase 8: 集成与测试

- [x] Task 8.1: 集成 EventBus 事件订阅
  - [ ] SubTask 8.1.1: 在 MainActivity 订阅 ProgressEvent
  - [ ] SubTask 8.1.2: 在 MainActivity 订阅 StatusChangedEvent
  - [ ] SubTask 8.1.3: 在 MainActivity 订阅 CompletedEvent

- [x] Task 8.2: 实现分享接收功能
  - [ ] SubTask 8.2.1: 配置 IntentFilter 接收分享
  - [ ] SubTask 8.2.2: 实现从剪贴板获取 URL

- [x] Task 8.3: 验证构建和功能
  - [ ] SubTask 8.3.1: 执行 Gradle sync 和 build
  - [ ] SubTask 8.3.2: 验证基础功能流程

## Task Dependencies

- Task 2.x 依赖于 Task 1.x
- Task 3.1 依赖于 Task 2.1
- Task 4.1 依赖于 Task 2.x
- Task 4.2 依赖于 Task 3.1
- Task 4.3 依赖于 Task 4.1, Task 4.2, Task 4.4
- Task 4.4 依赖于 Task 4.1
- Task 5.1 依赖于 Task 2.x
- Task 5.2 依赖于 Task 4.x, Task 5.1, Task 3.1
- Task 5.3 依赖于 Task 5.2
- Task 6.1 依赖于 Task 5.3
- Task 7.x 依赖于 Task 1.1, Task 7.1
- Task 8.1 依赖于 Task 5.x, Task 6.1, Task 7.x
- Task 8.2 依赖于 Task 5.2
- Task 8.3 依赖于 Task 8.1, Task 8.2
