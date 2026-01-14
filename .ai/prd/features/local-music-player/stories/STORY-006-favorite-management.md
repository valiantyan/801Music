---
id: "STORY-006"
title: "收藏功能 - 音频收藏与取消收藏"
type: Feature
epic_id: "FEAT-001"
feature_title: "本地音乐播放器"
target_version: "v1.0"
status: 草稿
priority: Medium
assignee: AI_Agent
created_date: "2025-01-27"
---

# Story: 收藏功能 - 音频收藏与取消收藏

## 1. Background & Context (背景与上下文)

用户在使用本地音乐播放器时，经常会有一些特别喜欢的音频文件。为了提升用户体验，需要提供收藏功能，让用户可以快速标记和管理喜欢的音频。收藏功能应该易于访问，用户可以在多个位置（列表、播放页、通知栏）快速收藏或取消收藏，并且收藏数据需要持久化保存，即使应用重启也不会丢失。

此 Story 实现收藏功能的完整流程，包括数据持久化、多入口操作和状态同步。

## 2. User Story (用户故事)

> As a **音乐爱好者**, I want to **收藏我喜欢的音频并在多个位置快速收藏/取消收藏**, so that **我可以快速访问我喜欢的音乐，并且收藏状态能够持久保存**.

## 3. Acceptance Criteria (验收标准) - DoD

- [ ] **AC1**: 用户可以在歌曲列表中点击收藏按钮，收藏/取消收藏当前歌曲
- [ ] **AC2**: 用户可以在播放页面中点击收藏按钮，收藏/取消收藏当前播放的歌曲
- [ ] **AC3**: 用户可以在系统媒体通知栏中点击收藏按钮，收藏/取消收藏当前播放的歌曲
- [ ] **AC4**: 收藏状态在所有界面实时同步显示（列表、播放页、通知栏）
- [ ] **AC5**: 收藏数据使用 Room 数据库持久化保存
- [ ] **AC6**: 应用重启后，收藏状态能够正确恢复
- [ ] **AC7**: 收藏按钮有明确的视觉反馈（已收藏/未收藏状态）
- [ ] **AC8**: 收藏操作响应时间 < 200ms
- [ ] **AC9**: 收藏数据支持查询（检查某首歌曲是否已收藏）
- [ ] **AC10**: 收藏操作支持撤销（取消收藏后可以重新收藏）
- [ ] **AC11**: 收藏状态图标符合 Material Design 3 设计规范
- [ ] **AC12**: 收藏功能不影响播放性能
- [ ] **UI/UX**: 
  - 收藏按钮图标清晰易识别（已收藏：实心图标，未收藏：空心图标）
  - 收藏操作有明确的视觉反馈
- [ ] **Unit Tests**: 
  - 收藏/取消收藏逻辑单元测试通过
  - Room 数据库操作单元测试通过
  - 收藏状态同步逻辑测试通过
- [ ] **Integration Tests**: 收藏功能端到端测试通过
- [ ] **Doc Sync**: 检查并更新了相关架构文档（如需要）

---

## 4. Technical Design & Implementation (技术设计与实现)

### 4.1 Affected Modules (受影响模块)

- `data/database/FavoriteEntity.kt` - 收藏数据实体
- `data/database/FavoriteDao.kt` - 收藏数据访问对象
- `data/database/AppDatabase.kt` - Room 数据库（添加收藏表）
- `data/repository/FavoriteRepository.kt` - 收藏数据仓库
- `data/datasource/FavoriteLocalDataSource.kt` - 收藏本地数据源
- `ui/songlist/SongListFragment.kt` - 列表 Fragment（添加收藏按钮）
- `ui/songlist/SongListAdapter.kt` - 列表适配器（收藏按钮处理）
- `ui/player/PlayerFragment.kt` - 播放 Fragment（添加收藏按钮）
- `service/PlayerNotificationManager.kt` - 通知栏管理器（添加收藏按钮）
- `viewmodel/SongListViewModel.kt` - 列表 ViewModel（收藏状态管理）
- `viewmodel/PlayerViewModel.kt` - 播放 ViewModel（收藏状态管理）

### 4.2 Key Changes (关键改动)

1. **Room 数据库设计**
   - 创建 `FavoriteEntity` 实体类
   - 创建 `FavoriteDao` 接口
   - 更新 `AppDatabase`，添加收藏表

2. **FavoriteRepository (收藏仓库)**
   - 封装收藏数据的业务逻辑
   - 提供 `addToFavorites(songId: String)` 方法
   - 提供 `removeFromFavorites(songId: String)` 方法
   - 提供 `isFavorite(songId: String): Flow<Boolean>` 方法
   - 提供 `getAllFavorites(): Flow<List<Song>>` 方法（可选，用于未来收藏列表）

3. **UI 层更新**
   - 歌曲列表项添加收藏图标按钮
   - 播放页面添加收藏按钮
   - 系统通知栏添加收藏自定义 Action
   - 收藏按钮根据状态显示不同的图标

4. **状态同步**
   - 使用 SharedFlow 或 StateFlow 实现收藏状态的实时同步
   - 所有界面订阅收藏状态变化，自动更新 UI

5. **数据模型扩展**
   - `Song` 数据类添加 `isFavorite: Boolean` 字段（运行时状态）
   - 使用 Flow 管理收藏状态变化

### 4.3 Dependencies (依赖)

- **Room 数据库**:
  - `androidx.room:room-runtime`
  - `androidx.room:room-ktx`
  - `androidx.room:room-compiler` (使用 ksp 或 kapt)
- **Kotlin Coroutines**: 异步数据库操作
- **Kotlin Flow**: 响应式状态管理
- **需要 Mock 的外部依赖**:
  - Room Database（测试时使用 in-memory database）
  - FavoriteRepository（测试时 Mock）

### 4.4 数据库设计

```kotlin
// FavoriteEntity.kt
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: String,  // 使用文件路径或唯一标识符
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

// FavoriteDao.kt
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    
    @Query("SELECT * FROM favorites WHERE songId = :songId")
    fun getFavorite(songId: String): Flow<FavoriteEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)
    
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)
    
    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun deleteFavoriteById(songId: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: String): Flow<Boolean>
}
```

### 4.5 Repository 接口设计

```kotlin
// FavoriteRepository.kt
interface FavoriteRepository {
    suspend fun addToFavorites(song: Song)
    suspend fun removeFromFavorites(songId: String)
    fun isFavorite(songId: String): Flow<Boolean>
    fun getAllFavorites(): Flow<List<Song>>
}
```

---

## 5. Development Steps (开发步骤 - TDD/Task Breakdown)

- [ ] **Task 1**: 添加 Room 数据库依赖到 build.gradle.kts
    - *Test Case*: 验证依赖正确添加，项目能够编译

- [ ] **Task 2**: 创建 FavoriteEntity 和 FavoriteDao
    - *Test Case*: 测试 FavoriteEntity 的创建和属性
    - *Test Case*: 测试 FavoriteDao 的增删查操作
    - *Test Case*: 测试 isFavorite 查询的准确性

- [ ] **Task 3**: 更新 AppDatabase，添加收藏表
    - *Test Case*: 测试数据库创建和表结构
    - *Test Case*: 测试数据库迁移（如有）

- [ ] **Task 4**: 实现 FavoriteLocalDataSource
    - *Test Case*: 测试数据源的增删查操作
    - *Test Case*: 测试 Flow 数据流

- [ ] **Task 5**: 实现 FavoriteRepository
    - *Test Case*: 测试添加收藏功能
    - *Test Case*: 测试取消收藏功能
    - *Test Case*: 测试收藏状态查询功能
    - *Test Case*: 测试重复收藏的处理（幂等性）

- [ ] **Task 6**: 扩展 Song 模型，添加收藏状态支持
    - *Test Case*: 测试 Song 对象的收藏状态字段
    - *Test Case*: 测试收藏状态的初始化和更新

- [ ] **Task 7**: 在歌曲列表中添加收藏按钮
    - *Test Case*: UI 测试 - 收藏按钮显示正确
    - *Test Case*: UI 测试 - 点击收藏按钮触发收藏操作
    - *Test Case*: UI 测试 - 收藏状态图标正确更新

- [ ] **Task 8**: 在播放页面中添加收藏按钮
    - *Test Case*: UI 测试 - 播放页面收藏按钮显示
    - *Test Case*: UI 测试 - 播放页面收藏操作
    - *Test Case*: UI 测试 - 播放页面收藏状态同步

- [ ] **Task 9**: 在系统通知栏中添加收藏按钮
    - *Test Case*: 测试通知栏收藏按钮显示
    - *Test Case*: 测试通知栏收藏按钮点击响应
    - *Test Case*: 测试通知栏收藏状态更新

- [ ] **Task 10**: 实现收藏状态实时同步
    - *Test Case*: 测试列表、播放页、通知栏三处收藏状态同步
    - *Test Case*: 测试应用重启后收藏状态恢复

- [ ] **Task 11**: 实现 ViewModel 收藏状态管理
    - *Test Case*: 测试 ViewModel 收藏状态订阅
    - *Test Case*: 测试收藏操作的状态更新

- [ ] **Task 12**: 集成测试和性能优化
    - *Test Case*: 端到端测试 - 完整收藏流程
    - *Test Case*: 性能测试 - 收藏操作响应时间 < 200ms
    - *Test Case*: 边界测试 - 大量收藏数据的性能

---

## 6. Development Notes & Log (开发笔记与日志)

**(Current Log):**
- [ ] 

---

## 7. 设计考虑

### 7.1 收藏标识符选择
- **选项1**: 使用文件路径作为唯一标识符
  - 优点：简单直接，无需额外字段
  - 缺点：文件移动或重命名会导致收藏丢失
- **选项2**: 使用文件路径 + 文件大小 + 修改时间作为组合键
  - 优点：更可靠，即使文件移动也能识别
  - 缺点：实现稍复杂
- **推荐**: v1.0 使用文件路径，未来可扩展为组合键

### 7.2 收藏状态同步策略
- 使用 Flow/StateFlow 实现响应式状态管理
- 所有界面订阅同一个收藏状态 Flow
- 收藏操作通过 Repository 统一处理，确保数据一致性

### 7.3 UI 设计
- 收藏图标：使用 Material Icons 的 `favorite`（实心）和 `favorite_border`（空心）
- 收藏按钮位置：
  - 列表：每项右侧，48dp 触摸目标
  - 播放页：播放控制区域，与其他控制按钮一致
  - 通知栏：作为自定义 Action，与其他媒体控制按钮一起

### 7.4 性能优化
- 收藏状态查询使用 Flow，支持响应式更新
- 批量操作时考虑使用事务
- 大量收藏数据时考虑分页加载（v1.0 可能不需要）

### 7.5 与 STORY-005 的关系
- 通知栏收藏按钮在 STORY-005 的基础上添加
- 通过 MediaSession 自定义 Action 实现
- 收藏状态通过 MediaSession 元数据更新
