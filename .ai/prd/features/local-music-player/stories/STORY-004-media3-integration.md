---
id: "STORY-004"
title: "Media3 播放引擎集成 - 集成 Media3 ExoPlayer 1.9.0"
type: Feature
epic_id: "FEAT-001"
feature_title: "本地音乐播放器"
target_version: "v1.0"
status: 草稿
priority: High
assignee: AI_Agent
created_date: "2025-01-27"
---

# Story: Media3 播放引擎集成 - 集成 Media3 ExoPlayer 1.9.0

## 1. Background & Context (背景与上下文)

Media3 ExoPlayer 是 Android 官方推荐的媒体播放引擎，功能强大且系统集成良好。此 Story 实现 Media3 ExoPlayer 1.9.0 的集成，包括：

- 封装 ExoPlayer 为统一的播放器接口
- 实现音频文件播放功能
- 支持播放控制（播放、暂停、跳转）
- 暴露播放状态（通过 Flow）
- 处理播放错误和异常情况

此 Story 为 STORY-003（播放控制）提供底层的播放引擎实现，是播放功能的技术基础。

## 2. User Story (用户故事)

> As a **音乐爱好者**, I want to **应用使用 Media3 播放引擎提供稳定、流畅的播放体验**, so that **我能够享受高质量的音频播放，支持多种音频格式**.

## 3. Acceptance Criteria (验收标准) - DoD

- [ ] **AC1**: 成功集成 Media3 ExoPlayer 1.9.0
- [ ] **AC2**: 能够播放本地音频文件（支持 MP3、AAC、FLAC、WAV、OGG、M4A）
- [ ] **AC3**: 支持播放控制：播放、暂停、跳转到指定位置
- [ ] **AC4**: 播放状态通过 Flow 实时暴露（播放中、暂停、完成、错误）
- [ ] **AC5**: 播放进度实时更新（当前位置、总时长、缓冲进度）
- [ ] **AC6**: 播放错误时能够正确处理和恢复
- [ ] **AC7**: 播放器资源正确管理（创建、释放、生命周期）
- [ ] **AC8**: 支持播放完成事件处理
- [ ] **AC9**: 播放器性能：启动延迟 < 500ms
- [ ] **AC10**: 播放过程中内存占用合理，无内存泄漏
- [ ] **AC11**: 支持音频焦点管理（与其他应用协调）
- [ ] **AC12**: 播放器封装为可测试的接口
- [ ] **Unit Tests**: 
  - 播放器接口单元测试通过
  - 播放状态管理逻辑测试通过
  - 错误处理逻辑测试通过
- [ ] **Integration Tests**: Media3 播放引擎集成测试通过
- [ ] **Doc Sync**: 检查并更新了相关架构文档（如需要）

---

## 4. Technical Design & Implementation (技术设计与实现)

### 4.1 Affected Modules (受影响模块)

- `player/MediaPlayerManager.kt` - Media3 ExoPlayer 封装
- `player/PlaybackController.kt` - 播放控制逻辑
- `data/repository/PlayerRepository.kt` - 播放器仓库实现
- `domain/model/PlaybackState.kt` - 播放状态模型
- `MainActivity.kt` - 应用生命周期管理（播放器释放）

### 4.2 Key Changes (关键改动)

1. **MediaPlayerManager (播放器管理器)**
   - 封装 ExoPlayer 实例
   - 实现统一的播放器接口
   - 管理播放器生命周期
   - 处理播放状态和事件

2. **ExoPlayer 初始化**
   - 创建 ExoPlayer 实例
   - 配置音频属性（采样率、声道等）
   - 设置音频焦点监听器
   - 注册播放事件监听器

3. **播放控制实现**
   - `play(uri: Uri)`: 开始播放
   - `pause()`: 暂停播放
   - `resume()`: 恢复播放
   - `seekTo(position: Long)`: 跳转到指定位置
   - `stop()`: 停止播放

4. **播放状态管理**
   - 监听 ExoPlayer 状态变化
   - 转换为领域模型 PlaybackState
   - 通过 Flow 暴露状态变化

5. **错误处理**
   - 捕获播放错误（文件不存在、格式不支持等）
   - 提供友好的错误信息
   - 支持错误恢复（如网络错误重试）

6. **音频焦点管理**
   - 请求音频焦点（AUDIOFOCUS_GAIN）
   - 处理音频焦点丢失（暂停播放）
   - 处理音频焦点恢复（恢复播放）

### 4.3 Dependencies (依赖)

- **Media3 ExoPlayer**: `androidx.media3:media3-exoplayer:1.9.0`
- **Media3 Common**: `androidx.media3:media3-common:1.9.0`
- **Media3 Decoder**: `androidx.media3:media3-decoder:1.9.0`
- **Media3 DataSource**: `androidx.media3:media3-datasource:1.9.0`
- **Kotlin Coroutines**: 异步操作
- **Kotlin Flow**: 状态数据流
- **需要 Mock 的外部依赖**:
  - ExoPlayer（测试时使用 Mock 或 TestExoPlayer）

### 4.4 播放器接口设计

```kotlin
// MediaPlayerManager.kt
interface MediaPlayerManager {
    fun play(uri: Uri)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(position: Long)
    fun getPlaybackState(): Flow<PlaybackState>
    fun release()
}

// PlaybackState.kt
data class PlaybackState(
    val isPlaying: Boolean,
    val position: Long,        // 当前位置（毫秒）
    val duration: Long,        // 总时长（毫秒）
    val bufferedPosition: Long, // 缓冲位置（毫秒）
    val playbackState: Int,     // ExoPlayer 状态
    val error: Exception?       // 错误信息
)
```

### 4.5 ExoPlayer 配置

```kotlin
// ExoPlayer 初始化
val exoPlayer = ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build(),
        true
    )
    .build()
```

---

## 5. Development Steps (开发步骤 - TDD/Task Breakdown)

- [ ] **Task 1**: 添加 Media3 依赖到 build.gradle.kts
    - *Test Case*: 验证依赖正确添加，项目能够编译

- [ ] **Task 2**: 创建 PlaybackState 数据模型
    - *Test Case*: 测试状态对象的创建和属性访问

- [ ] **Task 3**: 定义 MediaPlayerManager 接口
    - *Test Case*: 测试接口定义的正确性

- [ ] **Task 4**: 实现 ExoPlayer 初始化
    - *Test Case*: 测试 ExoPlayer 实例创建
    - *Test Case*: 测试音频属性配置

- [ ] **Task 5**: 实现播放控制（播放、暂停、停止）
    - *Test Case*: 测试播放本地音频文件
    - *Test Case*: 测试暂停和恢复功能
    - *Test Case*: 测试停止功能

- [ ] **Task 6**: 实现进度跳转功能
    - *Test Case*: 测试跳转到指定位置
    - *Test Case*: 测试边界情况（开始、结束位置）

- [ ] **Task 7**: 实现播放状态监听和转换
    - *Test Case*: 测试播放状态 Flow 的发送
    - *Test Case*: 测试状态转换的正确性

- [ ] **Task 8**: 实现播放进度更新
    - *Test Case*: 测试进度实时更新
    - *Test Case*: 测试缓冲进度更新

- [ ] **Task 9**: 实现错误处理
    - *Test Case*: 测试文件不存在错误处理
    - *Test Case*: 测试格式不支持错误处理
    - *Test Case*: 测试错误恢复逻辑

- [ ] **Task 10**: 实现音频焦点管理
    - *Test Case*: 测试音频焦点请求
    - *Test Case*: 测试音频焦点丢失处理
    - *Test Case*: 测试音频焦点恢复处理

- [ ] **Task 11**: 实现播放器生命周期管理
    - *Test Case*: 测试播放器资源释放
    - *Test Case*: 测试应用生命周期处理

- [ ] **Task 12**: 实现 PlayerRepository（集成 MediaPlayerManager）
    - *Test Case*: 测试仓库接口实现
    - *Test Case*: 测试与 ViewModel 的集成

- [ ] **Task 13**: 集成测试和性能优化
    - *Test Case*: 端到端测试 - 完整播放流程
    - *Test Case*: 性能测试 - 播放启动延迟 < 500ms
    - *Test Case*: 内存测试 - 无内存泄漏
    - *Test Case*: 边界测试 - 各种音频格式、损坏文件处理

---

## 6. Development Notes & Log (开发笔记与日志)

**(Current Log):**
- [ ] 

---

## 7. 设计考虑

### 7.1 ExoPlayer 生命周期
- 在应用启动时创建 ExoPlayer 实例（单例模式）
- 在应用退出时释放 ExoPlayer 资源
- 播放过程中保持 ExoPlayer 实例

### 7.2 播放状态同步
- 使用 Flow 实现响应式状态更新
- 播放进度更新频率控制（避免过于频繁的 UI 更新）
- 状态变化通过回调转换为 Flow

### 7.3 错误处理策略
- 文件不存在：显示友好错误，跳过该文件
- 格式不支持：显示格式错误提示
- 播放错误：尝试恢复，失败则通知用户

### 7.4 音频焦点管理
- 播放时请求音频焦点
- 其他应用请求焦点时，暂停播放
- 焦点恢复时，根据用户意图决定是否恢复播放

### 7.5 与 STORY-003 的关系
- 此 Story 提供播放引擎实现
- STORY-003 使用此 Story 提供的接口
- 两者通过 PlayerRepository 解耦

### 7.6 与 STORY-005 的关系
- 此 Story 提供播放器基础功能
- STORY-005 在此基础上添加 MediaSession 和通知栏集成
- MediaSessionService 使用此 Story 的播放器实例
