---
id: "STORY-003"
title: "音乐播放与队列管理 - 点击播放并管理播放队列"
type: Feature
epic_id: "FEAT-001"
feature_title: "本地音乐播放器"
target_version: "v1.0"
status: 已批准
priority: High
assignee: AI_Agent
created_date: "2025-01-27"
---

# Story: 音乐播放与队列管理 - 点击播放并管理播放队列

## 1. Background & Context (背景与上下文)

用户浏览歌曲列表后，需要能够播放选中的歌曲。此 Story 实现音乐播放和播放队列管理功能，包括：

- 点击歌曲后立即开始播放
- 自动将当前列表添加到播放队列
- 播放当前选中的歌曲
- 支持播放/暂停控制
- 支持上一首/下一首切换
- 显示播放进度和总时长
- 支持进度条拖拽跳转

此 Story 依赖 STORY-001（扫描）和 STORY-002（列表展示），是播放器的核心交互功能。播放引擎的具体实现将在 STORY-004 中完成。

## 2. User Story (用户故事)

> As a **音乐爱好者**, I want to **点击歌曲后能够立即播放，并且当前列表自动添加到播放队列**, so that **我可以连续播放多首歌曲，无需每次手动选择**.

## 3. Acceptance Criteria (验收标准) - DoD

- [ ] **AC1**: 用户点击歌曲列表中的歌曲后，立即开始播放该歌曲
- [ ] **AC2**: 点击歌曲时，自动将当前列表中的所有歌曲添加到播放队列
- [ ] **AC3**: 播放当前选中的歌曲（不是从队列第一首开始）
- [ ] **AC4**: 支持播放/暂停控制
- [ ] **AC5**: 支持上一首/下一首切换
- [ ] **AC6**: 显示当前播放歌曲的信息（标题、艺术家、封面）
- [ ] **AC7**: 显示播放进度（当前位置和总时长）
- [ ] **AC8**: 支持进度条拖拽跳转到指定位置
- [ ] **AC9**: 播放启动延迟 < 500ms（从点击到开始播放）
- [ ] **AC10**: 播放过程中，列表中的当前播放歌曲有视觉标识
- [ ] **AC11**: 播放队列支持循环播放（播放完最后一首后回到第一首）
- [ ] **AC12**: 播放过程中切换到其他 Fragment，播放继续（后台播放）
- [ ] **AC13**: 配置变更场景支持：
  - 播放过程中屏幕旋转，播放状态（当前歌曲、播放进度、队列）正确恢复
  - 系统主题切换后，播放控制界面样式正确适配
  - 分屏模式下播放控制界面布局正确显示
  - 进度条拖拽过程中配置变更，拖拽状态正确保存和恢复
  - 配置变更时播放不应中断（通过 Service 保证）
- [ ] **UI/UX**: 
  - 播放控制界面符合 Material Design 3 设计规范
  - 播放进度条交互流畅
  - 播放状态有明确的视觉反馈
- [ ] **Unit Tests**: 
  - 播放队列管理逻辑单元测试通过
  - 播放状态管理逻辑测试通过
- [ ] **Integration Tests**: 
  - 播放流程端到端测试通过
  - 配置变更测试：播放过程中旋转屏幕，验证播放状态恢复
  - 配置变更测试：进度条拖拽时旋转屏幕，验证拖拽状态恢复
  - 配置变更测试：切换主题、分屏模式，验证 UI 适配
- [ ] **Doc Sync**: 检查并更新了相关架构文档（如需要）

---

## 4. Technical Design & Implementation (技术设计与实现)

### 4.1 Affected Modules (受影响模块)

- `ui/player/PlayerFragment.kt` - 播放控制 Fragment
- `ui/player/PlayerControlsView.kt` - 播放控制组件（可选）
- `ui/player/PlayerFragmentBinding.kt` - ViewBinding（自动生成）
- `ui/songlist/SongListFragment.kt` - 列表 Fragment（点击事件）
- `viewmodel/PlayerViewModel.kt` - 播放 ViewModel
- `data/repository/PlayerRepository.kt` - 播放器数据仓库
- `player/MediaPlayerManager.kt` - Media3 播放器封装（STORY-004 实现）
- `player/MediaQueueManager.kt` - 播放队列管理
- `res/layout/fragment_player.xml` - 播放界面布局
- `res/layout/item_song.xml` - 列表项布局（添加播放状态标识）

### 4.2 Key Changes (关键改动)

1. **PlayerFragment (播放 Fragment)**
   - 使用 Navigation Component 从 SongListFragment 导航过来
   - 接收选中的歌曲信息（通过 Safe Args）
   - 显示播放控制界面
   - 订阅播放状态更新
   - **配置变更处理**：
     - 播放状态通过 ViewModel StateFlow 自动保存和恢复
     - 播放进度通过 Service 持久化，不受配置变更影响
     - 进度条拖拽状态通过 `onSaveInstanceState()` 保存
     - 确保配置变更时播放不中断（播放器在 Service 中）
     - 主题切换、分屏模式下布局正确适配

2. **PlayerViewModel (播放 ViewModel)**
   - 管理播放 UI 状态（StateFlow<PlayerUiState>）
   - 处理播放控制命令（播放、暂停、上一首、下一首、跳转）
   - 从 PlayerRepository 获取播放状态
   - 协调播放队列管理

3. **PlayerRepository (播放器仓库)**
   - 封装播放器操作接口
   - 管理播放队列
   - 暴露播放状态 Flow

4. **MediaQueueManager (队列管理器)**
   - 管理播放队列（当前列表）
   - 处理队列切换（上一首/下一首）
   - 支持循环播放逻辑

5. **播放状态同步**
   - 播放状态通过 Flow 同步到 UI
   - 列表中的当前播放歌曲高亮显示
   - 播放进度实时更新

6. **导航集成**
   - SongListFragment 点击歌曲 → 导航到 PlayerFragment
   - 通过 Safe Args 传递歌曲信息和完整列表

### 4.3 Dependencies (依赖)

- **Navigation Component**: Fragment 导航和参数传递
- **Media3 ExoPlayer**: 播放引擎（STORY-004 实现，此处使用接口）
- **Kotlin Flow**: 播放状态数据流
- **需要 Mock 的外部依赖**:
  - MediaPlayerManager（测试时 Mock 播放器）
  - PlayerRepository（测试时 Mock 数据源）

### 4.4 UI 状态设计

```kotlin
// PlayerUiState.kt
data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,        // 当前位置（毫秒）
    val duration: Long = 0L,        // 总时长（毫秒）
    val queue: List<Song> = emptyList(),  // 播放队列
    val currentIndex: Int = -1,     // 当前播放索引
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### 4.5 播放队列管理

```kotlin
// MediaQueueManager.kt
class MediaQueueManager {
    fun setQueue(songs: List<Song>, startIndex: Int)
    fun getCurrentSong(): Song?
    fun getNextSong(): Song?
    fun getPreviousSong(): Song?
    fun moveToNext(): Boolean
    fun moveToPrevious(): Boolean
    fun isLastSong(): Boolean
    fun isFirstSong(): Boolean
}
```

---

## 5. Development Steps (开发步骤 - TDD/Task Breakdown)

- [x] **Task 1**: 创建 PlayerUiState 数据类
    - *Test Case*: 测试状态对象的创建和属性访问

- [x] **Task 2**: 实现 MediaQueueManager（基础队列管理）
    - *Test Case*: 测试队列设置和当前歌曲获取
    - *Test Case*: 测试上一首/下一首切换逻辑

- [x] **Task 3**: 实现 MediaQueueManager（循环播放）
    - *Test Case*: 测试播放到最后一首后循环到第一首
    - *Test Case*: 测试单首歌曲的循环播放

- [x] **Task 4**: 实现 PlayerRepository（播放接口定义）
    - *Test Case*: 测试播放、暂停、跳转接口调用
    - *Test Case*: 测试播放状态 Flow 订阅

- [x] **Task 5**: 实现 PlayerViewModel（播放控制）
    - *Test Case*: 测试播放/暂停状态管理
    - *Test Case*: 测试上一首/下一首切换
    - *Test Case*: 测试进度跳转

- [ ] **Task 6**: 实现 PlayerViewModel（队列管理）
    - *Test Case*: 测试队列设置和当前歌曲选择
    - *Test Case*: 测试队列状态更新

- [ ] **Task 7**: 实现 PlayerFragment 布局 (fragment_player.xml)
    - *Test Case*: UI 测试 - 播放控制界面布局
    - *Test Case*: UI 测试 - 播放进度条显示

- [ ] **Task 8**: 实现 PlayerFragment（基础展示）
    - *Test Case*: UI 测试 - 播放信息正确显示
    - *Test Case*: UI 测试 - 播放状态订阅和更新
    - *Test Case*: 配置变更测试 - 播放过程中旋转屏幕，验证状态恢复

- [ ] **Task 9**: 实现 PlayerFragment（播放控制）
    - *Test Case*: UI 测试 - 播放/暂停按钮功能
    - *Test Case*: UI 测试 - 上一首/下一首按钮功能
    - *Test Case*: UI 测试 - 进度条拖拽跳转

- [ ] **Task 10**: 实现列表到播放的导航
    - *Test Case*: UI 测试 - 点击列表项导航到播放页面
    - *Test Case*: UI 测试 - 歌曲信息和列表正确传递

- [ ] **Task 11**: 实现列表中的播放状态标识
    - *Test Case*: UI 测试 - 当前播放歌曲高亮显示
    - *Test Case*: UI 测试 - 播放状态实时同步

- [ ] **Task 12**: 集成测试和性能优化
    - *Test Case*: 端到端测试 - 完整播放流程
    - *Test Case*: 性能测试 - 播放启动延迟 < 500ms
    - *Test Case*: 边界测试 - 单首歌曲、空队列、队列切换
    - *Test Case*: 配置变更测试 - 播放过程中各种配置变更场景（旋转、主题、分屏）

---

## 6. Development Notes & Log (开发笔记与日志)

**(Current Log):**
- [x] 2025-01-27 创建 [PlayerUiState] 数据类并补充 [PlayerUiStateTest] 单元测试。
- [x] 2025-01-27 运行单元测试：`./gradlew :app:testDebugUnitTest`。
- [x] 2025-01-27 实现 [MediaQueueManager] 基础队列管理并补充 [MediaQueueManagerTest] 单元测试。
- [x] 2025-01-27 运行单元测试：`./gradlew :app:testDebugUnitTest`。
- [x] 2025-01-27 完成 [MediaQueueManager] 循环播放逻辑并更新单元测试用例。
- [x] 2025-01-27 运行单元测试：`./gradlew :app:testDebugUnitTest`。
- [x] 2025-01-27 新增 [PlayerRepository] 接口与 [PlayerRepositoryImpl]，补充 [PlaybackState] 与单元测试。
- [x] 2025-01-27 运行单元测试：`./gradlew :app:testDebugUnitTest`。
- [x] 2025-01-27 新增 [PlayerViewModel] 播放控制并补充 [PlayerViewModelTest] 单元测试。
- [x] 2025-01-27 运行单元测试：`./gradlew :app:testDebugUnitTest`。

---

## 7. 设计考虑

### 7.1 播放队列策略
- **自动添加**: 点击歌曲时，自动将当前列表添加到队列
- **当前歌曲**: 播放选中的歌曲，而不是队列第一首
- **队列持久化**: v1.0 不持久化队列，应用重启后需要重新选择

### 7.2 播放状态同步
- 使用 Flow 实现播放状态的响应式更新
- 列表和播放页面共享播放状态（通过 ViewModel scope）
- 播放进度更新频率控制（避免过于频繁的 UI 更新）

### 7.3 播放控制
- 播放/暂停：切换播放状态
- 上一首/下一首：切换队列中的歌曲
- 进度跳转：支持拖拽到指定位置

### 7.4 后台播放
- 播放过程中切换到其他 Fragment，播放继续
- 播放状态通过 MediaSessionService 管理（STORY-005）

### 7.5 与 STORY-004 的关系
- 此 Story 定义播放控制和队列管理的业务逻辑
- STORY-004 实现具体的 Media3 ExoPlayer 集成
- 两者通过 PlayerRepository 接口解耦
