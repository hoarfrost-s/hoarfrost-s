# Tasks

- [x] Task 1: 项目基础架构搭建 — 配置 Gradle 依赖、包名、Hilt DI、SQLDelight
  - [x] SubTask 1.1: 配置 build.gradle.kts 依赖（OkHttp、Coroutines、Hilt、SQLDelight、Navigation Compose、Coil等）
  - [x] SubTask 1.2: 配置 settings.gradle.kts 和根 build.gradle.kts
  - [x] SubTask 1.3: 创建 Application 类和 Hilt 入口
  - [x] SubTask 1.4: 创建包结构目录（di/data/engine/network/encrypt/category/repository/viewmodel/ui/util）
  - [x] SubTask 1.5: 更新 AndroidManifest.xml（权限、Application、Service声明）

- [x] Task 2: 主题与 UI 基础组件 — Material 3 主题、通用组件
  - [x] SubTask 2.1: 设计并实现亮色/暗色主题配色系统
  - [x] SubTask 2.2: 实现通用 UI 组件（进度条、空状态、分类Chip等）
  - [x] SubTask 2.3: 实现底部导航栏和导航图

- [x] Task 3: SQLDelight 数据库层 — 下载任务表、分类规则表、DAO
  - [x] SubTask 3.1: 创建 DownloadTask.sq 下载任务表及查询语句
  - [x] SubTask 3.2: 创建 Category.sq 分类表和 SubCategory 表及查询语句
  - [x] SubTask 3.3: 实现 DownloadDao 和 CategoryDao
  - [x] SubTask 3.4: 实现默认分类数据初始化

- [x] Task 4: 分类规则引擎 — CategoryMatcher 及默认规则
  - [x] SubTask 4.1: 定义 Category 和 SubCategory 数据模型
  - [x] SubTask 4.2: 实现 CategoryMatcher 接口和实现类
  - [x] SubTask 4.3: 配置默认分类规则（6大类型 + 所有后缀名）
  - [x] SubTask 4.4: 编写分类匹配器单元测试

- [x] Task 5: 下载引擎核心 — 多线程分块下载、令牌桶限速
  - [x] SubTask 5.1: 实现 TokenBucket 令牌桶限速器
  - [x] SubTask 5.2: 实现 ChunkedDownloader 多线程分块下载器
  - [x] SubTask 5.3: 实现 DownloadManager 任务调度（并发控制、等待队列）
  - [x] SubTask 5.4: 实现 DownloadEngine 接口及 DownloadEngineImpl
  - [x] SubTask 5.5: 实现下载进度 Flow 回调

- [x] Task 6: 文件加密模块 — AES-256-GCM 加密解密
  - [x] SubTask 6.1: 实现 FileEncryptor 接口
  - [x] SubTask 6.2: 实现 AESFileEncryptor（AES-256-GCM 流式加密）
  - [x] SubTask 6.3: 实现文件名混淆/还原
  - [x] SubTask 6.4: 编写加密模块单元测试

- [x] Task 7: Repository 层 — 数据仓库、业务逻辑编排
  - [x] SubTask 7.1: 实现 DownloadRepository（下载任务编排）
  - [x] SubTask 7.2: 实现 CategoryRepository（分类数据管理）
  - [x] SubTask 7.3: 实现 SettingsRepository（设置持久化）
  - [x] SubTask 7.4: 实现 FileRepository（文件操作、分享）

- [x] Task 8: ViewModel 层 — UI 状态管理
  - [x] SubTask 8.1: 实现 DownloadViewModel（下载列表状态）
  - [x] SubTask 8.2: 实现 CategoryViewModel（分类管理状态）
  - [x] SubTask 8.3: 实现 SettingsViewModel（设置状态）
  - [x] SubTask 8.4: 实现 HomeViewModel（首页数据聚合）

- [x] Task 9: 首页 UI — 横向分类滑动、统计、最近下载
  - [x] SubTask 9.1: 实现 HomeScreen 主界面布局
  - [x] SubTask 9.2: 实现分类横向滑动卡片组件
  - [x] SubTask 9.3: 实现下载统计卡片
  - [x] SubTask 9.4: 实现最近下载列表
  - [x] SubTask 9.5: 实现 FAB 添加下载按钮和 BottomSheet 对话框

- [x] Task 10: 下载列表 UI — Tab切换、任务卡片、多选操作
  - [x] SubTask 10.1: 实现 DownloadListScreen（Tab + LazyColumn）
  - [x] SubTask 10.2: 实现下载任务卡片组件（进度、速度、操作按钮）
  - [x] SubTask 10.3: 实现多选操作模式
  - [x] SubTask 10.4: 实现空状态界面

- [x] Task 11: 设置页 UI — 下载设置、存储设置、外观设置
  - [x] SubTask 11.1: 实现 SettingsScreen 设置列表布局
  - [x] SubTask 11.2: 实现线程数/并发数滑块设置
  - [x] SubTask 11.3: 实现限速设置
  - [x] SubTask 11.4: 实现存储路径设置
  - [x] SubTask 11.5: 实现主题切换设置
  - [x] SubTask 11.6: 实现加密开关设置

- [x] Task 12: 分类管理 UI — 大类型管理、后缀名小类型管理
  - [x] SubTask 12.1: 实现 CategoryManageScreen 主界面
  - [x] SubTask 12.2: 实现大类型列表（展开/折叠）
  - [x] SubTask 12.3: 实现添加/编辑大类型对话框
  - [x] SubTask 12.4: 实现后缀名小类型管理
  - [x] SubTask 12.5: 实现添加后缀名对话框

- [x] Task 13: Android 系统集成 — 分享、剪切板、通知、前台服务
  - [x] SubTask 13.1: 实现系统分享接收（Share Intent）
  - [x] SubTask 13.2: 实现剪切板链接检测
  - [x] SubTask 13.3: 实现下载通知栏进度
  - [x] SubTask 13.4: 实现前台下载服务
  - [x] SubTask 13.5: 实现文件分享功能

- [x] Task 14: 单元测试 — 核心模块测试
  - [ ] SubTask 14.1: TokenBucket 限速器单元测试
  - [x] SubTask 14.2: CategoryMatcher 分类匹配器单元测试
  - [x] SubTask 14.3: FileEncryptor 加密解密单元测试
  - [ ] SubTask 14.4: DownloadRepository 仓库层单元测试

# Task Dependencies

- Task 2 依赖 Task 1
- Task 3 依赖 Task 1
- Task 4 依赖 Task 3
- Task 5 依赖 Task 1
- Task 6 依赖 Task 1
- Task 7 依赖 Task 3, Task 4, Task 5, Task 6
- Task 8 依赖 Task 7
- Task 9 依赖 Task 2, Task 8
- Task 10 依赖 Task 2, Task 8
- Task 11 依赖 Task 2, Task 8
- Task 12 依赖 Task 2, Task 8
- Task 13 依赖 Task 7, Task 8
- Task 14 依赖 Task 4, Task 5, Task 6, Task 7
