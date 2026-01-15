# music801 - 本地音乐播放器

## 项目简介

music801 是一个基于 Android 平台的本地音乐播放器应用，专注于提供简洁、流畅的本地音频播放体验。应用采用 **单 Activity + 多 Fragment + Navigation** 架构，使用 **MVVM + MVI 混合模式**和 **Clean Architecture** 分层设计，支持扫描设备存储中的音频文件，提供直观的歌曲列表展示，并集成 Media3 ExoPlayer 1.9.0 播放引擎，实现系统级媒体通知和控制。

**核心特性**：
- 🎵 完全离线工作，不依赖网络
- 🎨 遵循 Material Design 3 设计规范
- 🔄 支持配置变更（屏幕旋转、主题切换、分屏模式）后状态恢复
- ⚡ 响应式状态管理，使用 StateFlow 实现单向数据流
- 🎯 类型安全的导航和参数传递（Safe Args）
- 🧪 高可测试性，支持单元测试和集成测试

## 核心功能

### 1. 音频文件扫描
- 自动扫描设备 SD 卡和内部存储中的音频文件
- 支持常见音频格式：MP3、AAC、FLAC、WAV、OGG、M4A
- 后台异步扫描，不阻塞 UI 线程
- 实时显示扫描进度（已扫描文件数、当前扫描路径）
- 提取音频元数据（标题、艺术家、专辑、时长等）
- 支持 Android 13+ 分区存储权限模型（READ_MEDIA_AUDIO）

### 2. 歌曲列表展示
- 使用 RecyclerView 以列表形式展示所有扫描到的歌曲
- 显示歌曲基本信息（标题、艺术家、时长、封面）
- 支持流畅滚动，保持 60fps 帧率
- 配置变更后滚动位置自动恢复
- 支持深色/浅色主题切换

### 3. 音乐播放与队列管理
- 基于 Media3 ExoPlayer 1.9.0 播放引擎
- 点击歌曲立即播放（启动延迟 < 500ms）
- 自动将当前列表添加到播放队列
- 支持播放控制（播放/暂停、上一首/下一首、进度跳转）
- 播放队列支持循环播放
- 配置变更时播放不中断，状态正确恢复

### 4. 系统媒体集成
- 在系统媒体中心显示播放信息和控制
- 通知栏显示播放通知，包含播放控制按钮
- 支持锁屏界面播放控制
- 支持蓝牙设备媒体控制
- 使用前台服务确保后台播放稳定性

### 5. 收藏功能
- 在列表、播放页、通知栏多个位置快速收藏/取消收藏
- 收藏状态实时同步显示
- 使用 Room 数据库持久化收藏数据
- 应用重启后收藏状态正确恢复

## 技术栈

### 核心框架
- **编程语言**: Kotlin 2.0.21
- **UI 系统**: Android View System (XML Layout) + ViewBinding
- **架构模式**: MVVM + MVI 混合模式 + Clean Architecture
- **导航**: Navigation Component 2.8.6（单 Activity + 多 Fragment）
- **异步处理**: Kotlin Coroutines 1.10.2 + Flow
- **状态管理**: StateFlow（响应式状态管理）
- **依赖注入**: 手动依赖注入（v1.0），未来可迁移到 Hilt

### 播放引擎
- **Media3 ExoPlayer**: 1.9.0（官方推荐播放引擎）
- **Media3 Session**: 1.9.0（系统媒体会话集成）
- **Media3 UI**: 1.9.0（播放器 UI 组件）

### 数据存储
- **Room Database**: 2.6.1（收藏数据持久化）
- **DataStore Preferences**: 1.1.1（偏好设置）

### Android 配置
- **最低 SDK**: Android 7.0 (API 24)
- **目标 SDK**: Android 14 (API 36)
- **编译 SDK**: Android 14 (API 36)

## 项目架构

### 架构模式
本项目采用 **Clean Architecture（整洁架构）** 三层分层设计：

1. **表现层 (Presentation Layer)**
   - 单 Activity (`MainActivity`) + 多 Fragment 架构
   - Navigation Component 管理 Fragment 导航
   - ViewModel 使用 `StateFlow<UiState>` 管理 UI 状态
   - ViewBinding 实现类型安全的视图绑定

2. **领域层 (Domain Layer)**
   - 领域模型（Entities）：`Song`、`PlaybackState`、`ScanProgress`
   - Repository 接口定义（数据访问契约）
   - 业务逻辑独立于 UI 和数据源

3. **数据层 (Data Layer)**
   - Repository 实现（统一数据访问接口）
   - Room 数据库（收藏数据持久化）
   - 文件系统扫描（音频文件发现）
   - Media3 ExoPlayer（音频播放引擎）

4. **服务层 (Service Layer)**
   - MediaSessionService（媒体会话服务）
   - 前台服务确保后台播放

### 项目结构

```
app/
├── src/main/
│   ├── java/com/valiantyan/music801/
│   │   ├── ui/                      # UI 层
│   │   │   ├── main/                # MainActivity
│   │   │   ├── songlist/           # 歌曲列表 Fragment
│   │   │   ├── player/             # 播放控制 Fragment
│   │   │   ├── scan/               # 扫描进度 Fragment
│   │   │   └── navigation/         # 导航扩展
│   │   ├── viewmodel/              # ViewModel 层
│   │   │   ├── SongListViewModel.kt
│   │   │   ├── PlayerViewModel.kt
│   │   │   └── ScanViewModel.kt
│   │   ├── domain/                 # 领域层
│   │   │   └── model/              # 领域模型
│   │   │       ├── Song.kt
│   │   │       ├── ScanProgress.kt
│   │   │       └── PlaybackState.kt
│   │   ├── data/                   # 数据层
│   │   │   ├── repository/        # 数据仓库
│   │   │   ├── database/           # Room 数据库
│   │   │   ├── datasource/         # 数据源
│   │   │   └── local/             # 本地数据源
│   │   ├── player/                 # 播放器模块
│   │   │   ├── MediaPlayerManager.kt
│   │   │   ├── PlaybackController.kt
│   │   │   └── MediaQueueManager.kt
│   │   ├── service/                # 服务层
│   │   │   ├── MediaSessionService.kt
│   │   │   └── PlayerNotificationManager.kt
│   │   └── di/                     # 依赖注入
│   │       └── AppModule.kt
│   └── res/                        # 资源文件
│       ├── layout/                 # 布局文件
│       ├── navigation/             # 导航图
│       └── values/                 # 值资源
```

### 数据流向

```
用户操作
    ↓
Fragment (UI 层)
    ↓
ViewModel (处理用户交互)
    ↓
Repository (数据访问)
    ↓
DataSource (Room / File System / Media3)
    ↓
Flow<Data> (响应式数据流)
    ↓
ViewModel (更新 StateFlow<UiState>)
    ↓
Fragment (自动更新 UI)
```

### 状态管理

- **ViewModel**: 使用 `StateFlow<UiState>` 管理 UI 状态
- **自动配置变更处理**: ViewModel 在配置变更时不重建，状态自动保持
- **单向数据流**: UI → ViewModel → Repository → DataSource
- **响应式更新**: 使用 `collectAsStateWithLifecycle()` 订阅状态变化

## 开发规范

本项目遵循以下开发规范：

### 编码规范
- 遵循 Kotlin 编码规范和最佳实践（见 `.cursor/rules/1000-kotlin-basics.mdc`）
- 使用 ViewBinding 替代 `findViewById`
- 使用 StateFlow 管理 UI 状态
- 所有耗时操作在后台线程执行（Coroutines）

### 开发流程
- 采用 **TDD（测试驱动开发）** 模式
- 遵循敏捷工作流（见 `.cursor/rules/801-workflow-agile.mdc`）
- Git 提交信息遵循规范格式（见 `.cursor/rules/800-git-commit-format.mdc`）

### 架构原则
- **单一职责原则**: 每个类/模块只负责一个功能
- **状态驱动**: UI 完全由状态驱动，无内部 UI 状态
- **响应式编程**: 使用 Flow 实现响应式 UI
- **可测试性优先**: 架构设计优先考虑可测试性

## 核心依赖

### Media3 播放引擎
- `androidx.media3:media3-exoplayer:1.9.0` - 核心播放引擎
- `androidx.media3:media3-session:1.9.0` - 媒体会话管理
- `androidx.media3:media3-ui:1.9.0` - UI 组件
- `androidx.media3:media3-common:1.9.0` - 通用工具
- `androidx.media3:media3-decoder:1.9.0` - 音频解码
- `androidx.media3:media3-datasource:1.9.0` - 媒体数据源

### 架构组件
- `androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6` - ViewModel
- `androidx.lifecycle:lifecycle-runtime-ktx:2.8.6` - Lifecycle
- `androidx.navigation:navigation-fragment-ktx:2.8.6` - Navigation Component
- `androidx.navigation:navigation-ui-ktx:2.8.6` - Navigation UI

### 数据存储
- `androidx.room:room-runtime:2.6.1` - Room 数据库
- `androidx.room:room-ktx:2.6.1` - Room Kotlin 扩展
- `androidx.datastore:datastore-preferences:1.1.1` - DataStore

### 异步处理
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2` - 协程支持
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2` - 协程核心

### UI 组件
- `com.google.android.material:material:1.13.0` - Material Design 3
- `androidx.constraintlayout:constraintlayout:2.2.1` - ConstraintLayout

### 测试框架
- `junit:junit:4.13.2` - 单元测试
- `io.mockk:mockk:1.13.10` - Kotlin Mock 框架
- `app.cash.turbine:turbine:1.1.0` - Flow 测试工具
- `androidx.test.espresso:espresso-core:3.7.0` - UI 集成测试

详细依赖版本管理见 `gradle/libs.versions.toml`

## 权限要求

### 存储权限
- `READ_EXTERNAL_STORAGE` - 读取外部存储（Android 12 及以下）
- `READ_MEDIA_AUDIO` - 读取音频文件（Android 13+）

### 服务权限
- `FOREGROUND_SERVICE` - 前台服务（播放服务）
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - 媒体播放前台服务类型

### 通知权限
- `POST_NOTIFICATIONS` - 显示通知（Android 13+）

## 使用说明

### 首次使用
1. 启动应用后，自动请求存储权限
2. 授予权限后，应用自动开始扫描设备存储中的音频文件
3. 扫描过程中可查看实时进度（已扫描文件数、当前扫描路径）
4. 扫描完成后，自动跳转到歌曲列表

### 播放音乐
1. 在歌曲列表中点击任意歌曲
2. 应用自动将当前列表添加到播放队列，开始播放选中的歌曲
3. 在播放页面可控制播放/暂停、上一首/下一首、进度跳转
4. 可通过通知栏、锁屏界面或系统媒体中心控制播放

### 收藏歌曲
1. 在歌曲列表、播放页面或通知栏点击收藏按钮
2. 收藏状态实时同步到所有界面
3. 收藏数据持久化保存，应用重启后仍保留

### 配置变更支持
- **屏幕旋转**: 旋转设备后，列表滚动位置、播放状态等自动恢复
- **主题切换**: 切换系统主题后，UI 样式自动适配
- **分屏模式**: 分屏模式下应用正常显示和运行

## 性能目标

- **播放启动延迟**: < 500ms（从点击到开始播放）
- **UI 帧率**: 保持 60fps，列表滚动流畅
- **扫描性能**: 1000 首歌曲扫描时间 < 30 秒
- **内存占用**: 运行时 < 150MB（不含音频缓冲）
- **电池效率**: 播放时 CPU 占用 < 10%

## 配置变更支持

应用完整支持以下配置变更场景，确保用户体验不受影响：

- ✅ **屏幕旋转**: RecyclerView 滚动位置自动恢复，播放状态保持
- ✅ **系统主题切换**: UI 样式自动适配深色/浅色主题
- ✅ **分屏模式**: 布局正确适配，功能正常
- ✅ **语言切换**: 字符串资源自动更新
- ✅ **字体大小调整**: 布局正确适配新字体大小

状态保存机制：
- ViewModel 使用 StateFlow，配置变更时状态自动保持
- Fragment 使用 `onSaveInstanceState()` 保存视图状态
- RecyclerView 使用 `LinearLayoutManager.onSaveInstanceState()` 保存滚动位置
- 播放状态通过 MediaSessionService 持久化

## 开发计划

详细的产品需求文档和开发计划见：
- 产品需求文档：`.ai/prd/` 目录
- 架构设计文档：`.ai/architecture/arch-overview.md`
- Story 开发任务：`.ai/prd/features/local-music-player/stories/`

## 相关文档

- [架构设计文档](.ai/architecture/arch-overview.md) - 详细的架构设计和技术选型说明
- [产品需求文档](.ai/prd/) - 功能需求和用户故事
- [开发规范](.cursor/rules/) - 编码规范和开发流程

## 许可证

[待定]
