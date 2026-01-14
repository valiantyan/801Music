---
id: "STORY-005"
title: "系统媒体通知集成 - 通知栏和锁屏播放控制"
type: Feature
epic_id: "FEAT-001"
feature_title: "本地音乐播放器"
target_version: "v1.0"
status: 草稿
priority: High
assignee: AI_Agent
created_date: "2025-01-27"
---

# Story: 系统媒体通知集成 - 通知栏和锁屏播放控制

## 1. Background & Context (背景与上下文)

用户在使用音乐播放器时，经常需要在应用外控制播放（如锁屏、通知栏、蓝牙设备）。此 Story 实现系统媒体通知和控制集成，包括：

- 在系统媒体中心显示播放信息和控制
- 通知栏显示播放通知，包含播放控制按钮
- 锁屏界面显示播放控制
- 支持蓝牙设备媒体控制
- 支持 Android Auto（如果设备支持）

此 Story 依赖 STORY-004（Media3 播放引擎），通过 MediaSession 和 MediaSessionService 实现系统级媒体控制。

## 2. User Story (用户故事)

> As a **音乐爱好者**, I want to **在系统通知栏和锁屏界面看到播放控制**, so that **我可以在不打开应用的情况下控制播放，提升使用体验**.

## 3. Acceptance Criteria (验收标准) - DoD

- [ ] **AC1**: 播放时在系统媒体中心显示播放信息（标题、艺术家、封面）
- [ ] **AC2**: 通知栏显示播放通知，包含：
  - 歌曲标题和艺术家
  - 播放/暂停按钮
  - 上一首/下一首按钮
  - 关闭按钮
- [ ] **AC3**: 锁屏界面显示播放控制（播放/暂停、上一首/下一首）
- [ ] **AC4**: 通知栏控制按钮能够正确响应（播放、暂停、上一首、下一首、关闭）
- [ ] **AC5**: 播放状态变化时，通知栏自动更新
- [ ] **AC6**: 支持蓝牙设备媒体控制
- [ ] **AC7**: 通知栏显示歌曲封面（如果有）
- [ ] **AC8**: 通知栏样式符合 Material Design 3 设计规范
- [ ] **AC9**: 使用前台服务确保通知显示和后台播放
- [ ] **AC10**: 通知栏控制响应时间 < 200ms
- [ ] **AC11**: 应用退出后，通知栏仍然可用（通过服务保持）
- [ ] **AC12**: 支持 Android 13+ 通知权限（POST_NOTIFICATIONS）
- [ ] **UI/UX**: 
  - 通知栏样式美观，符合系统设计规范
  - 控制按钮图标清晰易识别
- [ ] **Unit Tests**: 
  - MediaSession 集成逻辑单元测试通过
  - 通知栏管理逻辑测试通过
- [ ] **Integration Tests**: 系统媒体通知端到端测试通过
- [ ] **Doc Sync**: 检查并更新了相关架构文档（如需要）

---

## 4. Technical Design & Implementation (技术设计与实现)

### 4.1 Affected Modules (受影响模块)

- `service/MediaSessionService.kt` - 媒体会话服务（继承 MediaSessionService）
- `service/PlayerNotificationManager.kt` - 通知栏管理器
- `player/MediaPlayerManager.kt` - 播放器管理器（与 MediaSession 集成）
- `MainActivity.kt` - 应用入口（启动服务）
- `AndroidManifest.xml` - 服务声明和权限配置
- `res/drawable/` - 通知栏图标资源

### 4.2 Key Changes (关键改动)

1. **MediaSessionService (媒体会话服务)**
   - 继承 `MediaSessionService`
   - 创建和管理 `MediaSession`
   - 处理系统媒体控制命令
   - 作为前台服务运行

2. **MediaSession 集成**
   - 创建 MediaSession 实例
   - 设置播放器回调（MediaSession.Callback）
   - 更新播放状态和元数据
   - 处理媒体按钮事件

3. **PlayerNotificationManager (通知栏管理器)**
   - 创建和更新播放通知
   - 管理通知栏样式和内容
   - 处理通知栏按钮点击事件
   - 显示歌曲封面

4. **前台服务配置**
   - 使用 `startForegroundService()` 启动服务
   - 创建前台服务通知
   - 设置服务优先级（FOREGROUND_SERVICE_MEDIA_PLAYBACK）

5. **通知栏样式设计**
   - 使用 MediaStyle 通知样式
   - 显示大图标（封面）
   - 添加媒体控制按钮
   - 支持展开/收起样式

6. **权限处理**
   - Android 13+：POST_NOTIFICATIONS 权限
   - 在 MainActivity 中请求权限

### 4.3 Dependencies (依赖)

- **Media3 Session**: `androidx.media3:media3-session:1.9.0`
- **Media3 ExoPlayer**: `androidx.media3:media3-exoplayer:1.9.0`
- **AndroidX Core**: 通知相关 API
- **需要 Mock 的外部依赖**:
  - NotificationManager（测试时 Mock）
  - MediaSession（测试时使用 TestMediaSession）

### 4.4 MediaSession 配置

```kotlin
// MediaSessionService.kt
class MusicPlayerService : MediaSessionService() {
    private lateinit var mediaSession: MediaSession
    
    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .build()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}
```

### 4.5 通知栏样式

```kotlin
// PlayerNotificationManager.kt
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
        .setShowActionsInCompactView(0, 1, 2)
        .setMediaSession(mediaSession.sessionToken))
    .setSmallIcon(R.drawable.ic_music_note)
    .setLargeIcon(albumArt)
    .setContentTitle(song.title)
    .setContentText(song.artist)
    .addAction(prevAction)
    .addAction(playPauseAction)
    .addAction(nextAction)
    .build()
```

---

## 5. Development Steps (开发步骤 - TDD/Task Breakdown)

- [ ] **Task 1**: 添加 Media3 Session 依赖到 build.gradle.kts
    - *Test Case*: 验证依赖正确添加，项目能够编译

- [ ] **Task 2**: 在 AndroidManifest.xml 中声明 MediaSessionService
    - *Test Case*: 验证服务声明正确

- [ ] **Task 3**: 创建 MediaSessionService 基础框架
    - *Test Case*: 测试服务创建和生命周期

- [ ] **Task 4**: 实现 MediaSession 创建和配置
    - *Test Case*: 测试 MediaSession 实例创建
    - *Test Case*: 测试 MediaSession 回调注册

- [ ] **Task 5**: 实现 MediaSession.Callback（播放控制）
    - *Test Case*: 测试播放/暂停命令处理
    - *Test Case*: 测试上一首/下一首命令处理
    - *Test Case*: 测试跳转命令处理

- [ ] **Task 6**: 实现播放状态更新到 MediaSession
    - *Test Case*: 测试播放状态同步
    - *Test Case*: 测试播放进度更新

- [ ] **Task 7**: 实现播放元数据更新到 MediaSession
    - *Test Case*: 测试歌曲信息更新
    - *Test Case*: 测试封面更新

- [ ] **Task 8**: 实现 PlayerNotificationManager（基础通知）
    - *Test Case*: 测试通知创建和显示
    - *Test Case*: 测试通知内容更新

- [ ] **Task 9**: 实现 PlayerNotificationManager（媒体样式）
    - *Test Case*: 测试 MediaStyle 通知样式
    - *Test Case*: 测试控制按钮显示

- [ ] **Task 10**: 实现通知栏按钮点击处理
    - *Test Case*: 测试播放/暂停按钮点击
    - *Test Case*: 测试上一首/下一首按钮点击
    - *Test Case*: 测试关闭按钮点击

- [ ] **Task 11**: 实现前台服务配置
    - *Test Case*: 测试前台服务启动
    - *Test Case*: 测试前台服务通知显示

- [ ] **Task 12**: 实现通知权限处理（Android 13+）
    - *Test Case*: 测试权限请求
    - *Test Case*: 测试权限被拒绝时的处理

- [ ] **Task 13**: 实现锁屏控制
    - *Test Case*: 测试锁屏界面显示播放控制
    - *Test Case*: 测试锁屏控制按钮功能

- [ ] **Task 14**: 集成测试和性能优化
    - *Test Case*: 端到端测试 - 完整通知栏控制流程
    - *Test Case*: 性能测试 - 通知栏控制响应时间 < 200ms
    - *Test Case*: 边界测试 - 各种播放状态、错误情况处理

---

## 6. Development Notes & Log (开发笔记与日志)

**(Current Log):**
- [ ] 

---

## 7. 设计考虑

### 7.1 前台服务策略
- 使用 FOREGROUND_SERVICE_MEDIA_PLAYBACK 类型
- 播放时启动前台服务，确保后台播放
- 停止播放后可以停止服务（或保持服务以支持快速恢复）

### 7.2 通知栏样式
- 使用 MediaStyle 提供系统级媒体通知样式
- 支持展开/收起，显示更多信息
- 大图标显示歌曲封面

### 7.3 MediaSession 生命周期
- 服务启动时创建 MediaSession
- 服务停止时释放 MediaSession
- MediaSession 与播放器生命周期绑定

### 7.4 控制命令处理
- 播放/暂停：切换播放状态
- 上一首/下一首：切换队列中的歌曲
- 关闭：停止播放并关闭通知

### 7.5 与 STORY-004 的关系
- 此 Story 在 STORY-004 的基础上添加系统集成
- MediaSessionService 使用 STORY-004 的播放器实例
- 通过 MediaSession 桥接系统控制和播放器

### 7.6 与 STORY-006 的关系
- 通知栏可以添加收藏按钮（STORY-006 实现）
- 收藏状态通过 MediaSession 元数据更新
