# Android 下载管理器 Spec

## Why
当前基础项目仅包含一个 Hello World 级别的 Compose 模板，需要基于工程交付规约实现一款功能完整的 Android 下载管理器应用，支持多类型文件下载、自动分类、多线程加速、下载限速、任务队列管理、文件加密与分享等核心功能。

## What Changes
- 将基础项目从 Hello World 升级为完整的下载管理器应用
- 新增 SQLDelight 数据库层（下载任务、分类规则）
- 新增 OkHttp 下载引擎（多线程分块下载、断点续传、令牌桶限速）
- 新增 AES-256-GCM 文件加密模块
- 新增分类规则引擎（大类型 + 后缀名小类型自动归类）
- 新增 MVVM + Repository 架构分层
- 新增 Jetpack Compose UI（首页、下载列表、设置、分类管理）
- 新增 Hilt 依赖注入
- 新增 Kotlin Coroutines 异步处理
- 新增系统分享、剪切板检测、通知栏进度等 Android 集成
- **BREAKING**: 包名和应用名变更为下载管理器

## Impact
- Affected specs: 全部功能（下载引擎、分类系统、加密、UI、存储、网络）
- Affected code:
  - `app/build.gradle.kts` — 新增依赖
  - `app/src/main/AndroidManifest.xml` — 权限和组件声明
  - `app/src/main/java/com/example/myjetpackcompose/` — 全部源码重构

## ADDED Requirements

### Requirement: 下载引擎核心
系统 SHALL 提供基于 OkHttp 的多线程下载引擎，支持 HTTP/HTTPS 协议的文件下载。

#### Scenario: 单线程下载成功
- **WHEN** 用户添加一个直链下载任务
- **THEN** 系统使用单线程下载文件，下载完成后文件完整可用

#### Scenario: 多线程分块下载
- **WHEN** 用户设置线程数为 3-9
- **THEN** 系统使用 HTTP Range 请求头进行分块并发下载，完成后合并文件

#### Scenario: 断点续传
- **WHEN** 下载中断后恢复
- **THEN** 系统从已下载的进度继续，不重新下载已完成部分

#### Scenario: 暂停/恢复/取消
- **WHEN** 用户对下载中任务执行暂停/恢复/取消操作
- **THEN** 任务状态相应切换，取消时清理临时文件

### Requirement: 下载速度限制
系统 SHALL 提供令牌桶（Token Bucket）限速算法，支持全局和单任务限速。

#### Scenario: 全局限速生效
- **WHEN** 设置全局下载速度限制为 512KB/s
- **THEN** 所有下载任务总速度稳定在 512KB/s 左右（波动不超过 20%）

#### Scenario: 动态调整限速
- **WHEN** 下载过程中修改限速值
- **THEN** 下载速度实时变化到新的限制值

#### Scenario: 不限速
- **WHEN** 限速设置为 0（不限速）
- **THEN** 下载速度不受限制，使用最大带宽

### Requirement: 多任务队列管理
系统 SHALL 支持 1-5 个任务同时下载，超出并发数的任务进入等待队列。

#### Scenario: 超出并发排队
- **WHEN** 最大并发数设为 3，添加 5 个任务
- **THEN** 3 个任务开始下载，2 个进入等待队列

#### Scenario: 队列自动推进
- **WHEN** 一个下载中任务完成
- **THEN** 等待队列首位任务自动开始下载

#### Scenario: 批量操作
- **WHEN** 用户选择多个任务执行暂停/恢复/取消
- **THEN** 所有选中任务同时执行相应操作

### Requirement: 自动分类系统
系统 SHALL 根据文件后缀名自动将下载文件归类到大类型和小类型（后缀名）。

#### Scenario: 默认分类匹配
- **WHEN** 下载文件名为 `movie.mp4`
- **THEN** 自动归类为 视频 > mp4

#### Scenario: 未知后缀归入其他
- **WHEN** 下载未知后缀名文件
- **THEN** 自动归类为 其他

#### Scenario: 自定义大类型
- **WHEN** 用户添加新大类型"电子书"并指定 epub/mobi/azw3 后缀名
- **THEN** 对应后缀名文件自动归类到电子书

#### Scenario: 后缀名大小写不敏感
- **WHEN** 文件名为 `MOVIE.MP4`
- **THEN** 仍正确归类为 视频 > mp4

### Requirement: 文件加密
系统 SHALL 提供 AES-256-GCM 文件加密，下载完成后自动加密存储。

#### Scenario: 加密后文件名混淆
- **WHEN** 文件下载完成并加密
- **THEN** 文件名变为 Base64 编码 + .enc 后缀，外部无法直接识别

#### Scenario: 加密解密往返一致
- **WHEN** 对加密文件执行解密
- **THEN** 解密后文件与原始文件内容完全一致

#### Scenario: 加密文件外部不可读
- **WHEN** 用其他应用尝试打开加密文件
- **THEN** 无法识别或打开文件

### Requirement: 文件分享
系统 SHALL 支持通过 Android 系统分享面板分享已下载文件。

#### Scenario: 分享加密文件
- **WHEN** 用户分享已加密的文件
- **THEN** 系统自动解密后分享，分享完成后重新加密

#### Scenario: 分享未加密文件
- **WHEN** 用户分享未加密的文件
- **THEN** 直接分享原文件

### Requirement: 添加任务方式
系统 SHALL 支持多种方式添加下载任务。

#### Scenario: FAB 按钮添加
- **WHEN** 用户点击首页浮动按钮
- **THEN** 弹出添加任务对话框，输入 URL 后开始下载

#### Scenario: 系统分享接收
- **WHEN** 用户从浏览器或其他应用分享链接到下载管理器
- **THEN** App 打开并自动填充 URL 到添加对话框

#### Scenario: 剪切板检测
- **WHEN** 剪切板包含下载链接，用户切换到 App
- **THEN** 弹出提示检测到剪切板中的下载链接

### Requirement: 下载存储路径
系统 SHALL 按分类层级自动创建存储目录。

#### Scenario: 默认路径创建
- **WHEN** 首次下载 mp4 文件
- **THEN** 自动创建 `/sdcard/DownloadManager/视频/mp4/` 目录

#### Scenario: 自定义根目录
- **WHEN** 用户在设置中修改下载根目录
- **THEN** 后续下载保存到新目录下的分类子目录中

### Requirement: 首页 UI
系统 SHALL 提供首页，包含横向分类滑动、下载统计、最近下载列表。

#### Scenario: 横向分类展示
- **WHEN** 用户进入首页
- **THEN** 顶部展示横向滑动的分类卡片（视频、压缩包、音频、安装包、文档、其他），每张卡片显示图标、名称、文件数量

#### Scenario: 最近下载列表
- **WHEN** 用户进入首页
- **THEN** 显示最近下载的任务列表，包含进度、状态、操作按钮

### Requirement: 下载列表 UI
系统 SHALL 提供下载列表页面，支持 Tab 切换和任务管理。

#### Scenario: Tab 切换
- **WHEN** 用户在下载列表页切换 Tab
- **THEN** 显示对应状态的任务（全部/下载中/已完成/失败）

#### Scenario: 任务卡片
- **WHEN** 查看下载中任务
- **THEN** 显示文件名、分类标签、大小、进度条、速度、暂停/删除按钮

### Requirement: 设置 UI
系统 SHALL 提供设置页面，包含下载、存储、外观等设置项。

#### Scenario: 线程数设置
- **WHEN** 用户通过滑块调整最大线程数（1-9）
- **THEN** 设置保存，新任务使用该线程数

#### Scenario: 并发数设置
- **WHEN** 用户通过滑块调整最大并发数（1-5）
- **THEN** 设置保存，任务队列按新并发数调整

#### Scenario: 主题设置
- **WHEN** 用户选择主题模式（跟随系统/亮色/暗色）
- **THEN** App 立即切换到对应主题

### Requirement: 分类管理 UI
系统 SHALL 提供分类管理页面，支持大类型和后缀名小类型的增删改。

#### Scenario: 查看默认分类
- **WHEN** 进入分类管理页
- **THEN** 显示 6 个默认大类型，点击展开查看后缀名小类型

#### Scenario: 添加大类型
- **WHEN** 用户添加新大类型并填写名称和图标
- **THEN** 新类型出现在列表和首页横向滑动中

#### Scenario: 添加后缀名小类型
- **WHEN** 用户为某个大类型添加新的后缀名
- **THEN** 该后缀名文件自动归类到对应大类型

### Requirement: 主题系统
系统 SHALL 支持亮色/暗色主题，使用 Material Design 3 设计语言。

#### Scenario: 跟随系统主题
- **WHEN** 系统切换亮色/暗色模式
- **THEN** App 自动切换对应主题

#### Scenario: 手动切换主题
- **WHEN** 用户在设置中手动选择主题
- **THEN** App 立即应用所选主题

## MODIFIED Requirements

### Requirement: 基础项目改造
将原 Hello World Compose 项目改造为下载管理器应用。

- 原 `Greeting` Composable 替换为完整的 App 导航结构
- 原主题系统扩展为 Material 3 亮色/暗色双主题
- 新增 Hilt 依赖注入配置
- 新增 SQLDelight 数据库配置

## REMOVED Requirements

### Requirement: 原示例代码
**Reason**: 基础项目仅为模板，示例代码需全部替换为业务代码
**Migration**: 删除 `Greeting` 和 `GreetingPreview` 等示例代码
