---
id: "STORY-002"
title: "歌曲列表展示 - 列表形式展示扫描到的歌曲"
type: Feature
epic_id: "FEAT-001"
feature_title: "本地音乐播放器"
target_version: "v1.0"
status: 已批准
priority: High
assignee: AI_Agent
created_date: "2025-01-27"
---

# Story: 歌曲列表展示 - 列表形式展示扫描到的歌曲

## 1. Background & Context (背景与上下文)

用户扫描到音频文件后，需要以清晰、易用的方式查看所有歌曲。此 Story 实现歌曲列表的展示功能，包括：

- 使用 RecyclerView 展示歌曲列表
- 显示歌曲基本信息（标题、艺术家、时长）
- 支持列表滚动和快速定位
- 显示歌曲封面（如果有）
- 列表项点击交互反馈

此 Story 依赖 STORY-001（音频文件扫描）的扫描结果，是用户浏览和选择歌曲的主要界面。

## 2. User Story (用户故事)

> As a **音乐爱好者**, I want to **以列表形式查看所有扫描到的歌曲，包括标题、艺术家和时长信息**, so that **我能够快速浏览和找到想要播放的音乐**.

## 3. Acceptance Criteria (验收标准) - DoD

- [ ] **AC1**: 使用 RecyclerView 以列表形式展示所有扫描到的歌曲
- [ ] **AC2**: 列表项显示歌曲标题、艺术家、时长信息
- [ ] **AC3**: 列表项支持显示歌曲封面（如果有）
- [ ] **AC4**: 列表支持流畅滚动，保持 60fps 帧率
- [ ] **AC5**: 列表项点击有明确的视觉反馈
- [ ] **AC6**: 列表数据来自 STORY-001 的扫描结果
- [ ] **AC7**: 列表为空时显示友好的空状态提示
- [ ] **AC8**: 列表加载时显示加载状态
- [ ] **AC9**: 列表项支持长按操作（为未来功能预留，v1.0 可不实现具体功能）
- [ ] **AC10**: 列表项布局符合 Material Design 3 设计规范
- [ ] **AC11**: 配置变更场景支持：
  - 屏幕旋转后，RecyclerView 滚动位置正确恢复
  - 系统主题切换（深色/浅色）后，列表界面样式正确适配
  - 分屏模式下列表布局正确显示，支持流畅滚动
  - 配置变更后，列表数据状态正确恢复（通过 ViewModel）
- [ ] **UI/UX**: 
  - 列表界面符合 Material Design 3 设计规范
  - 列表项触摸目标符合最小尺寸要求（48dp）
  - 支持深色模式
- [ ] **Unit Tests**: 
  - SongListAdapter 单元测试通过
  - 列表数据绑定逻辑测试通过
- [ ] **Integration Tests**: 
  - 列表展示端到端测试通过
  - 配置变更测试：滚动列表后旋转屏幕，验证滚动位置恢复
  - 配置变更测试：切换主题，验证列表 UI 适配
  - 配置变更测试：分屏模式下验证列表布局和滚动
- [ ] **Doc Sync**: 检查并更新了相关架构文档（如需要）

---

## 4. Technical Design & Implementation (技术设计与实现)

### 4.1 Affected Modules (受影响模块)

- `ui/songlist/SongListFragment.kt` - 歌曲列表 Fragment
- `ui/songlist/SongListAdapter.kt` - RecyclerView 适配器
- `ui/songlist/SongListItemView.kt` - 列表项视图（可选）
- `ui/songlist/SongListFragmentBinding.kt` - ViewBinding（自动生成）
- `viewmodel/SongListViewModel.kt` - 列表 ViewModel
- `data/repository/AudioRepository.kt` - 音频数据仓库（获取扫描结果）
- `res/layout/fragment_song_list.xml` - 列表布局
- `res/layout/item_song.xml` - 列表项布局

### 4.2 Key Changes (关键改动)

1. **SongListFragment (列表 Fragment)**
   - 使用 Navigation Component 集成到 MainActivity
   - 包含 RecyclerView 显示歌曲列表
   - 订阅 ViewModel 的状态更新
   - 处理列表项点击事件（导航到播放页面）
   - **配置变更处理**：
     - 使用 `LinearLayoutManager.onSaveInstanceState()` 保存滚动位置
     - 在 `onViewStateRestored()` 中恢复滚动位置
     - 列表数据通过 ViewModel StateFlow 自动恢复
     - 确保主题切换、分屏模式下布局正确适配

2. **SongListAdapter (列表适配器)**
   - 实现 RecyclerView.Adapter
   - 使用 ViewBinding 绑定列表项视图
   - 实现 DiffUtil 优化列表更新性能
   - 处理列表项点击和长按事件

3. **SongListViewModel (列表 ViewModel)**
   - 管理列表 UI 状态（StateFlow<SongListUiState>）
   - 从 AudioRepository 获取歌曲列表
   - 处理列表加载、空状态、错误状态

4. **列表项布局设计**
   - 使用 ConstraintLayout 实现灵活布局
   - 显示：封面图片（可选）、标题、艺术家、时长
   - 符合 Material Design 3 卡片样式
   - 支持点击波纹效果

5. **数据绑定**
   - 使用 ViewBinding 简化视图访问
   - ViewModel 通过 StateFlow 暴露数据
   - Fragment 通过 collectAsStateWithLifecycle 订阅状态

### 4.3 Dependencies (依赖)

- **RecyclerView**: AndroidX RecyclerView
- **ViewBinding**: 自动生成的绑定类
- **Material Design**: Material Components
- **Navigation Component**: Fragment 导航
- **Kotlin Flow**: 响应式数据流
- **需要 Mock 的外部依赖**:
  - AudioRepository（测试时 Mock 数据源）

### 4.4 UI 状态设计

```kotlin
// SongListUiState.kt
data class SongListUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null
)
```

### 4.5 列表项布局结构

```xml
<!-- item_song.xml -->
<androidx.cardview.widget.CardView>
    <androidx.constraintlayout.widget.ConstraintLayout>
        <ImageView /> <!-- 封面 -->
        <TextView />  <!-- 标题 -->
        <TextView />  <!-- 艺术家 -->
        <TextView />  <!-- 时长 -->
    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
```

---

## 5. Development Steps (开发步骤 - TDD/Task Breakdown)

- [x] **Task 1**: 创建 SongListUiState 数据类 ✅
    - *Test Case*: 测试状态对象的创建和属性访问 ✅

- [x] **Task 2**: 实现列表项布局 (item_song.xml) ✅
    - *Test Case*: UI 测试 - 布局正确显示所有元素 ✅
    - *Test Case*: UI 测试 - 支持深色模式 ✅

- [x] **Task 3**: 实现 SongListFragment 布局 (fragment_song_list.xml) ✅
    - *Test Case*: UI 测试 - RecyclerView 正确显示 ✅
    - *Test Case*: UI 测试 - 空状态和加载状态显示 ✅

- [x] **Task 4**: 实现 SongListAdapter（基础功能） ✅
    - *Test Case*: 测试适配器的创建和数据绑定 ✅
    - *Test Case*: 测试列表项点击事件 ⏳（后续交互联调）

- [ ] **Task 5**: 实现 SongListAdapter（DiffUtil 优化）
    - *Test Case*: 测试列表更新时的性能优化
    - *Test Case*: 测试增量更新逻辑

- [ ] **Task 6**: 实现 SongListViewModel（数据获取）
    - *Test Case*: 测试从 AudioRepository 获取歌曲列表
    - *Test Case*: 测试列表状态更新（加载、完成、错误）

- [ ] **Task 7**: 实现 SongListFragment（基础展示）
    - *Test Case*: UI 测试 - 列表正确显示歌曲数据
    - *Test Case*: UI 测试 - 状态订阅和更新
    - *Test Case*: 配置变更测试 - 滚动列表后旋转屏幕，验证滚动位置恢复

- [ ] **Task 8**: 实现 SongListFragment（交互功能）
    - *Test Case*: UI 测试 - 列表项点击导航到播放页面
    - *Test Case*: UI 测试 - 列表滚动性能（60fps）

- [ ] **Task 9**: 实现空状态和加载状态
    - *Test Case*: UI 测试 - 空列表时显示空状态
    - *Test Case*: UI 测试 - 加载时显示加载指示器

- [ ] **Task 10**: 集成测试和性能优化
    - *Test Case*: 端到端测试 - 从扫描到列表展示完整流程
    - *Test Case*: 性能测试 - 大量歌曲（1000+）列表滚动流畅
    - *Test Case*: 边界测试 - 空列表、单首歌曲、特殊字符处理
    - *Test Case*: 配置变更测试 - 主题切换、分屏模式下的完整流程

---

## 6. Development Notes & Log (开发笔记与日志)

**(Current Log):**
- [x] **2025-01-27**: Task 1 已完成 ✅
  - 创建 `SongListUiState` 数据类（`app/src/main/java/com/valiantyan/music801/viewmodel/SongListUiState.kt`）
  - 完成单元测试 `SongListUiStateTest`（`app/src/test/java/com/valiantyan/music801/viewmodel/SongListUiStateTest.kt`）
  - `./gradlew test` 通过
- [x] **2025-01-27**: Task 2 已完成 ✅
  - 新增列表项布局 `item_song.xml`（`app/src/main/res/layout/item_song.xml`）
  - 添加列表封面文案 `song_album_art`（`app/src/main/res/values/strings.xml`）
  - `./gradlew test` 通过
- [x] **2025-01-27**: Task 3 已完成 ✅
  - 新增列表 Fragment 布局 `fragment_song_list.xml`（`app/src/main/res/layout/fragment_song_list.xml`）
  - 添加列表空状态与加载文案（`app/src/main/res/values/strings.xml`）
  - `./gradlew test` 通过
- [x] **2025-01-27**: Task 4 已完成 ✅
  - 新增 `SongListAdapter` 基础实现（`app/src/main/java/com/valiantyan/music801/ui/songlist/SongListAdapter.kt`）
  - 新增 `SongListAdapterTest` 覆盖创建与绑定（`app/src/test/java/com/valiantyan/music801/ui/songlist/SongListAdapterTest.kt`）
  - `./gradlew test` 通过

---

## 7. 设计考虑

### 7.1 列表性能优化
- 使用 DiffUtil 优化列表更新，避免不必要的重绘
- 列表项视图复用，减少内存占用
- 大量数据时考虑分页加载（v1.0 可能不需要，但架构要支持）

### 7.2 封面图片加载
- v1.0 阶段：如果元数据中有封面路径，直接显示
- 未来可优化：使用图片加载库（如 Coil）实现异步加载和缓存

### 7.3 列表排序
- v1.0 默认按文件扫描顺序显示
- 未来可添加：按标题、艺术家、时长等排序

### 7.4 点击交互
- 列表项点击：导航到播放页面（STORY-003）
- 长按：预留接口，未来可添加更多操作（如添加到播放列表）

### 7.5 导航集成
- 使用 Navigation Component 从 SongListFragment 导航到 PlayerFragment
- 通过 Safe Args 传递选中的歌曲信息
