---
id: "STORY-001"
title: "音频文件扫描 - 扫描设备存储中的音频文件"
type: Feature
epic_id: "FEAT-001"
feature_title: "本地音乐播放器"
target_version: "v1.0"
status: 已批准
priority: High
assignee: AI_Agent
created_date: "2025-01-27"
---

# Story: 音频文件扫描 - 扫描设备存储中的音频文件

## 1. Background & Context (背景与上下文)

本地音乐播放器的核心功能是播放设备存储中的音频文件。在用户能够播放音乐之前，应用需要首先发现和识别设备上的所有音频文件。这个 Story 实现了音频文件的扫描功能，包括：

- 扫描设备 SD 卡和内部存储
- 识别常见音频格式（MP3、AAC、FLAC、WAV、OGG、M4A）
- 提取音频元数据（标题、艺术家、专辑、时长等）
- 后台异步扫描，不阻塞 UI
- 显示扫描进度

此 Story 是后续所有功能的基础，没有扫描功能，用户无法看到和播放任何音乐。

## 2. User Story (用户故事)

> As a **音乐爱好者**, I want to **应用自动扫描设备存储中的音频文件**, so that **我能够看到所有可播放的音乐，无需手动添加文件**.

## 3. Acceptance Criteria (验收标准) - DoD

- [ ] **AC1**: 应用启动后能够自动开始扫描设备存储中的音频文件
- [ ] **AC2**: 扫描支持常见音频格式：MP3、AAC、FLAC、WAV、OGG、M4A
- [ ] **AC3**: 扫描范围包括 SD 卡和内部存储（在权限允许的情况下）
- [ ] **AC4**: 扫描过程在后台异步执行，不阻塞 UI 线程
- [ ] **AC5**: 扫描过程中显示进度信息（已扫描文件数、当前扫描路径等）
- [ ] **AC6**: 能够提取音频文件的元数据：标题、艺术家、专辑、时长、文件路径
- [ ] **AC7**: 扫描结果能够正确识别和过滤音频文件（跳过非音频文件）
- [ ] **AC8**: 扫描性能：1000 首歌曲扫描时间 < 30 秒（在中等性能设备上）
- [ ] **AC9**: 支持 Android 13+ 分区存储权限模型（READ_MEDIA_AUDIO）
- [ ] **AC10**: 扫描完成后，结果能够被其他模块（如列表展示）访问
- [ ] **UI/UX**: 扫描进度界面符合 Material Design 3 设计规范
- [ ] **Unit Tests**: 
  - 音频文件识别逻辑单元测试通过
  - 元数据提取逻辑单元测试通过
  - 扫描进度计算逻辑测试通过
- [ ] **Integration Tests**: 端到端扫描流程测试通过
- [ ] **Doc Sync**: 检查并更新了相关架构文档（如需要）

---

## 4. Technical Design & Implementation (技术设计与实现)

### 4.1 Affected Modules (受影响模块)

- `data/datasource/AudioFileScanner.kt` - 文件系统扫描器
- `data/datasource/MediaMetadataExtractor.kt` - 音频元数据提取器
- `data/repository/AudioRepository.kt` - 音频数据仓库
- `domain/model/Song.kt` - 歌曲领域模型
- `domain/model/ScanProgress.kt` - 扫描进度模型
- `ui/scan/ScanProgressFragment.kt` - 扫描进度界面
- `viewmodel/ScanViewModel.kt` - 扫描 ViewModel
- `MainActivity.kt` - 主 Activity（权限请求）

### 4.2 Key Changes (关键改动)

1. **AudioFileScanner (文件扫描器)**
   - 实现递归文件系统扫描
   - 支持音频格式过滤（通过文件扩展名）
   - 使用 Kotlin Coroutines 在后台线程执行
   - 通过 Flow 发送扫描进度更新

2. **MediaMetadataExtractor (元数据提取器)**
   - 使用 Android MediaMetadataRetriever 提取元数据
   - 处理元数据缺失的情况（使用文件名作为标题）
   - 支持多种音频格式的元数据提取

3. **AudioRepository (音频仓库)**
   - 封装扫描逻辑，提供统一的扫描接口
   - 管理扫描结果缓存（内存中）
   - 暴露 Flow<List<Song>> 供其他模块订阅

4. **ScanViewModel (扫描 ViewModel)**
   - 管理扫描状态（StateFlow<ScanUiState>）
   - 处理扫描开始、进度更新、完成、错误等状态
   - 协调 UI 和 Repository 之间的交互

5. **ScanProgressFragment (扫描进度界面)**
   - 显示扫描进度（进度条、已扫描文件数、当前路径）
   - 支持取消扫描操作
   - 扫描完成后自动导航到歌曲列表

6. **权限处理**
   - Android 12 及以下：READ_EXTERNAL_STORAGE
   - Android 13+：READ_MEDIA_AUDIO
   - 在 MainActivity 中处理权限请求

### 4.3 Dependencies (依赖)

- **Android 权限**: READ_EXTERNAL_STORAGE (API < 33) / READ_MEDIA_AUDIO (API 33+)
- **Kotlin Coroutines**: 异步扫描执行
- **Kotlin Flow**: 扫描进度和结果的数据流
- **MediaMetadataRetriever**: 音频元数据提取（Android 系统 API）
- **需要 Mock 的外部依赖**:
  - File 系统访问（测试时使用虚拟文件系统）
  - MediaMetadataRetriever（测试时 Mock）

### 4.4 数据模型设计

```kotlin
// Song.kt - 歌曲领域模型
data class Song(
    val id: String,              // 文件路径作为唯一标识
    val title: String,           // 标题
    val artist: String,          // 艺术家
    val album: String?,          // 专辑（可选）
    val duration: Long,          // 时长（毫秒）
    val filePath: String,        // 文件路径
    val fileSize: Long,          // 文件大小
    val dateAdded: Long,         // 添加时间
    val albumArtPath: String?    // 封面路径（可选）
)

// ScanProgress.kt - 扫描进度模型
data class ScanProgress(
    val scannedCount: Int,        // 已扫描文件数
    val totalCount: Int?,         // 总文件数（可能未知）
    val currentPath: String?,     // 当前扫描路径
    val isScanning: Boolean       // 是否正在扫描
)
```

---

## 5. Development Steps (开发步骤 - TDD/Task Breakdown)

- [x] **Task 1**: 创建 Song 和 ScanProgress 数据模型 ✅
    - *Test Case*: 测试 Song 对象的创建和属性访问 ✅ (3个测试用例全部通过)
    - *Test Case*: 测试 ScanProgress 对象的创建和状态更新 ✅ (5个测试用例全部通过)

- [ ] **Task 2**: 实现音频文件格式识别逻辑
    - *Test Case*: 测试 MP3、AAC、FLAC、WAV、OGG、M4A 格式识别
    - *Test Case*: 测试非音频文件被正确过滤

- [ ] **Task 3**: 实现 MediaMetadataExtractor
    - *Test Case*: 测试元数据提取（标题、艺术家、专辑、时长）
    - *Test Case*: 测试元数据缺失时的默认值处理
    - *Test Case*: 测试不同音频格式的元数据提取

- [ ] **Task 4**: 实现 AudioFileScanner（基础扫描功能）
    - *Test Case*: 测试单目录扫描
    - *Test Case*: 测试递归子目录扫描
    - *Test Case*: 测试扫描进度 Flow 的发送

- [ ] **Task 5**: 实现 AudioFileScanner（进度更新）
    - *Test Case*: 测试扫描进度正确计算和更新
    - *Test Case*: 测试大量文件扫描的性能

- [ ] **Task 6**: 实现 AudioRepository（扫描接口）
    - *Test Case*: 测试扫描方法调用
    - *Test Case*: 测试扫描结果的 Flow 订阅
    - *Test Case*: 测试扫描结果缓存

- [ ] **Task 7**: 实现 ScanViewModel
    - *Test Case*: 测试扫描状态管理（开始、进行中、完成、错误）
    - *Test Case*: 测试扫描进度更新到 UI 状态
    - *Test Case*: 测试扫描取消功能

- [ ] **Task 8**: 实现权限请求逻辑（MainActivity）
    - *Test Case*: 测试 Android 12 及以下权限请求
    - *Test Case*: 测试 Android 13+ 权限请求
    - *Test Case*: 测试权限被拒绝时的处理

- [ ] **Task 9**: 实现 ScanProgressFragment UI
    - *Test Case*: UI 测试 - 扫描进度显示
    - *Test Case*: UI 测试 - 取消按钮功能
    - *Test Case*: UI 测试 - 扫描完成后的导航

- [ ] **Task 10**: 集成测试和性能优化
    - *Test Case*: 端到端测试 - 完整扫描流程
    - *Test Case*: 性能测试 - 1000 首歌曲扫描时间 < 30 秒
    - *Test Case*: 边界测试 - 空目录、无权限、损坏文件处理

---

## 6. Development Notes & Log (开发笔记与日志)

**(Current Log):**
- [x] **2025-01-27**: Task 1 已完成 ✅
  - 创建了 `Song.kt` 数据模型（位于 `domain/model/Song.kt`）
    - 包含所有必需字段：id, title, artist, album, duration, filePath, fileSize, dateAdded, albumArtPath
    - 符合 Story 文档中的设计规范
  - 创建了 `ScanProgress.kt` 数据模型（位于 `domain/model/ScanProgress.kt`）
    - 包含所有必需字段：scannedCount, totalCount, currentPath, isScanning
    - 符合 Story 文档中的设计规范
  - 编写了完整的单元测试：
    - `SongTest.kt`: 3个测试用例全部通过
      - 测试对象创建和属性访问
      - 测试可选字段（album, albumArtPath）为 null 的情况
      - 测试数据类相等性比较
    - `ScanProgressTest.kt`: 5个测试用例全部通过
      - 测试对象创建和属性访问
      - 测试可选字段（totalCount, currentPath）为 null 的情况
      - 测试数据类相等性比较
      - 测试扫描完成状态
      - 测试扫描进行中状态
  - 所有测试用例已通过验证（0 failures, 0 errors） 

---

## 7. 设计考虑

### 7.1 扫描策略
- **首次启动**: 自动开始扫描
- **手动刷新**: 支持用户手动触发重新扫描
- **增量扫描**: v1.0 不实现增量扫描，每次全量扫描（未来可优化）

### 7.2 性能优化
- 使用协程在后台线程执行，避免阻塞 UI
- 大量文件时考虑分批处理，避免内存溢出
- 扫描进度更新频率控制（避免过于频繁的 UI 更新）

### 7.3 错误处理
- 文件读取失败：跳过该文件，记录日志，继续扫描
- 权限被拒绝：显示友好提示，引导用户授予权限
- 元数据提取失败：使用文件名作为标题，继续处理

### 7.4 权限处理
- Android 13+ 使用 READ_MEDIA_AUDIO（分区存储）
- Android 12 及以下使用 READ_EXTERNAL_STORAGE
- 权限请求在 MainActivity 中统一处理
