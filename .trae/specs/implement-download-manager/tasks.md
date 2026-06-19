# Tasks

- [x] Task 1: 创建 Android 多模块项目结构
  - [x] SubTask 1.1: 创建 Gradle Kotlin DSL 根项目，配置各模块 build.gradle.kts
  - [x] SubTask 1.2: 创建模块目录结构（:app, :common, :domain, :data, :engine, :service, :ui:home, :ui:category, :ui:settings）
  - [x] SubTask 1.3: 配置版本目录（libs.versions.toml），声明所有依赖（Compose, OkHttp, Room, Hilt, Coroutines）
  - [x] SubTask 1.4: 创建 Application 类（DownloadApp）和 MainActivity，配置 Hilt

- [x] Task 2: 实现 common 模块（工具类与常量）
  - [x] SubTask 2.1: 实现 FileCategory 枚举和 CategoryResolver 映射表
  - [x] SubTask 2.2: 实现 TaskStatus 枚举和状态机
  - [x] SubTask 2.3: 实现 UrlParser、FileUtils、FormatUtils 工具类
  - [x] SubTask 2.4: 定义 DownloadTask 数据模型和 Segment 数据模型

- [x] Task 3: 实现 domain 模块（UseCase 和 Repository 接口）
  - [x] SubTask 3.1: 定义 Repository 接口（TaskRepository, CategoryRepository, ConfigRepository）
  - [x] SubTask 3.2: 实现 AddTaskUseCase（URL 解析、分类、任务创建）
  - [x] SubTask 3.3: 实现 StartTaskUseCase、PauseTaskUseCase、ResumeTaskUseCase、CancelTaskUseCase
  - [x] SubTask 3.4: 实现 ScheduleTasksUseCase（调度逻辑封装）

- [x] Task 4: 实现 data 模块（Room 持久化与 DataStore）
  - [x] SubTask 4.1: 创建 Room Entity（DownloadTaskEntity）和 DAO（TaskDao）
  - [x] SubTask 4.2: 创建 AppDatabase 和 TypeConverter
  - [x] SubTask 4.3: 实现 TaskRepositoryImpl（Room 操作 + 文件系统操作）
  - [x] SubTask 4.4: 实现 SettingsDataStore（配置读写）
  - [x] SubTask 4.5: 配置 Hilt DI 模块（DatabaseModule, RepositoryModule）

- [x] Task 5: 实现 engine 模块（下载引擎核心）
  - [x] SubTask 5.1: 实现 SlotManager（Semaphore 并发槽位管理）
  - [x] SubTask 5.2: 实现 TokenBucketRateLimiter（全局共享令牌桶限速器）
  - [x] SubTask 5.3: 实现 SegmentDispatcher（分段调度与并行下载）
  - [x] SubTask 5.4: 实现 DownloadEngine（下载引擎主控，含断点续传）
  - [x] SubTask 5.5: 实现 TaskScheduler（FIFO 调度器，Channel + Mutex）
  - [x] SubTask 5.6: 实现 M3u8Parser（流媒体解析与 ts 分片下载+合并）
  - [x] SubTask 5.7: 配置 Engine 模块 Hilt DI

- [x] Task 6: 实现 service 模块（后台下载与通知）
  - [x] SubTask 6.1: 实现 NotificationHelper（通知渠道创建、进度通知构建）
  - [x] SubTask 6.2: 实现 DownloadForegroundService（START_STICKY，暂停/恢复/停止 Action）
  - [x] SubTask 6.3: 实现 NetworkMonitor（ConnectivityManager 网络状态监听）
  - [x] SubTask 6.4: 配置 Service 模块 Hilt DI

- [x] Task 7: 实现 UI 模块 — 首页任务列表
  - [x] SubTask 7.1: 实现 TaskViewModel（任务列表状态管理、搜索、筛选）
  - [x] SubTask 7.2: 实现 HomeScreen（LazyColumn 任务列表、搜索栏、筛选 Chips、FAB）
  - [x] SubTask 7.3: 实现 TaskCard（任务卡片组件，含分段进度条、状态标签、操作按钮）
  - [x] SubTask 7.4: 实现 SegmentedProgressBar（分块进度条组件，渐变色、动画、圆角）
  - [x] SubTask 7.5: 实现 AddTaskBottomSheet（URL 输入、链接信息预览、确认下载）
  - [x] SubTask 7.6: 实现手势操作（左滑暂停/恢复、右滑删除、长按多选）
  - [x] SubTask 7.7: 实现 BottomNavigation 导航框架

- [x] Task 8: 实现 UI 模块 — 任务详情页
  - [x] SubTask 8.1: 实现 TaskDetailScreen（链接信息展示、分段进度详情、操作按钮）
  - [x] SubTask 8.2: 实现链接信息展示区域（URL、文件名、大小、类型、分类、Content-Type、线程数、保存路径、创建时间）

- [x] Task 9: 实现 UI 模块 — 分类浏览页与设置页
  - [x] SubTask 9.1: 实现 CategoryViewModel 和 CategoryScreen（分类文件浏览）
  - [x] SubTask 9.2: 实现 SettingsViewModel 和 SettingsScreen（Slider/下拉/开关/文件选择）
  - [x] SubTask 9.3: 实现主题切换（深色/浅色/跟随系统）

- [x] Task 10: 集成与收尾
  - [x] SubTask 10.1: 在 MainActivity 中集成 Navigation、BottomNavigation、主题
  - [x] SubTask 10.2: 注册系统分享 Intent Filter（ShareReceiver）
  - [x] SubTask 10.3: 实现剪贴板 URL 识别
  - [x] SubTask 10.4: 实现 APK 下载后自动安装（FileProvider）
  - [x] SubTask 10.5: 全局异常处理与错误提示

# Task Dependencies
- Task 2 依赖 Task 1（项目结构）
- Task 3 依赖 Task 2（domain 依赖 common 模型）
- Task 4 依赖 Task 2（data 依赖 common 模型）
- Task 5 依赖 Task 2（engine 依赖 common 模型）
- Task 6 依赖 Task 5（service 依赖 engine）
- Task 7 依赖 Task 3, Task 4, Task 5（UI 依赖 domain/data/engine）
- Task 8 依赖 Task 7（详情页依赖首页导航）
- Task 9 可与 Task 7, Task 8 并行
- Task 10 依赖 Task 7, Task 8, Task 9