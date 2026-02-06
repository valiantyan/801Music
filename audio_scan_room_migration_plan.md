# 音频扫描与本地媒体库持久化改造方案（Room 版，大厂实践）

## 目标摘要
- 将“每次启动全量扫描”改为“首启/无库扫描 + 后续读库秒开 + 增量同步”。
- 扫描结果持久化到 Room，启动优先读库。
- 无数据时自动扫描；有数据时展示列表并支持手动“扫描新音频”。
- 新下载音频可通过后台增量同步或手动按钮进入数据库。

## 现状与问题（基于当前代码）
- 当前导航起点是扫描页：`app/src/main/res/navigation/nav_graph.xml`
- 扫描页首次创建即触发扫描：`app/src/main/java/com/valiantyan/music801/ui/scan/ScanProgressFragment.kt`
- `AudioRepository` 仅内存缓存，无持久化：`app/src/main/java/com/valiantyan/music801/data/repository/AudioRepository.kt`
- 结果：每次冷启动都扫描，启动慢、耗电高、用户体感差。

## 总体改造策略
- 数据源分层：`MediaStore/文件系统 -> 同步器 -> Room -> UI`。
- 启动策略：先 `Room` 回显，再按策略触发增量同步。
- 扫描策略：
  - `首启或库空`：自动全量扫描。
  - `库非空`：后台增量同步，不阻塞首屏。
  - `用户手动`：按钮触发“扫描新音频”。
- 同步策略：
  - Upsert 新增/变更音频。
  - 清理已删除音频（软删除或硬删除，默认硬删除）。
  - 记录 `lastScanAt` 与 `lastSyncToken`。

## 数据库设计（Room）

### 新增表 1：`songs`
- 主键：`id`（建议使用 `MediaStore.Audio.Media._ID` 转字符串；无 MediaStore 场景退化为 `filePath`）。
- 字段：
  - `title`, `artist`, `album`, `duration`, `filePath`, `fileSize`, `dateAdded`, `albumArtPath`
  - `modifiedAt`（文件修改时间）
  - `scannedAt`（入库时间）
- 索引：
  - `filePath` 唯一索引
  - `dateAdded` 普通索引
  - `modifiedAt` 普通索引

### 新增表 2：`library_sync_state`
- 单行表（`id=1`）：
  - `lastScanAt: Long`
  - `lastFullScanAt: Long`
  - `lastSyncToken: Long`（可用 MediaStore `DATE_MODIFIED` 最大值）
  - `lastScanStatus: String`（`SUCCESS/FAILED/RUNNING`）
  - `lastError: String?`
- 用途：判断是否首扫、是否需要自动增量、故障可观测。

### DAO 接口
- `SongDao`
  - `observeAllSongs(): Flow<List<SongEntity>>`
  - `getCount(): Int`
  - `upsertAll(items: List<SongEntity>)`
  - `deleteByIds(ids: List<String>)`
  - `deleteNotInIds(ids: List<String>)`（全量扫描后对账）
- `LibrarySyncStateDao`
  - `getState(): LibrarySyncStateEntity?`
  - `upsert(state: LibrarySyncStateEntity)`

### Room Database
- 新增 `MusicDatabase`，版本 `v1`（当前项目无 Room，可直接 v1）。
- 提供 `songDao()`、`librarySyncStateDao()`。

## 领域与仓库接口改造

### `AudioRepository` 职责重构
- 从“内存缓存仓库”改为“媒体库仓库（Room 持久化 + 同步编排）”。
- 对外接口（新增/调整）：
  - `observeSongs(): Flow<List<Song>>`（替代当前 `getAllSongs`，底层读 Room）
  - `suspend fun ensureInitialScanIfNeeded(): ScanDecision`
  - `fun scanAndSync(mode: ScanMode): Flow<ScanProgress>`
  - `suspend fun hasLocalSongs(): Boolean`

### `ScanMode`
- `FULL_INITIAL`
- `INCREMENTAL_AUTO`
- `MANUAL`

### `ScanDecision`
- `SKIP_ALREADY_HAS_DATA`
- `RUN_INITIAL_SCAN`
- `RUN_INCREMENTAL_SYNC`

## 同步器组件（新增）
- `AudioLibrarySyncService`（非 Android Service，纯业务类）
  - `fullScanAndRebuild(rootPath)`
  - `incrementalSync()`
- 默认建议使用 `MediaStore` 做增量识别（大厂常规）：
  - 全量：查询全部音频并入库。
  - 增量：按 `DATE_MODIFIED > lastSyncToken` 拉取变更并 upsert。
  - 删除检测：对比当前媒体 ID 集与库内 ID 集，删除不存在项。
- 若短期不改 `MediaStore`，可先保留现有 `AudioFileScanner` 做全量，后续迭代增量。

## 启动与页面流程改造

### 导航改造
- 将 `startDestination` 从 `scanProgressFragment` 调整为 `songListFragment`。
- `scanProgressFragment` 保留为显式扫描页（手动触发或无数据自动跳转时使用）。

### `SongListViewModel` 改造
- 初始化时：
  - 先订阅 `observeSongs()`，立即展示本地数据。
  - 并发触发 `ensureInitialScanIfNeeded()`：
    - 若库空：自动扫描（可跳转扫描页或嵌入页内进度）。
    - 若有库：可选触发 `INCREMENTAL_AUTO`（后台，不阻塞列表）。
- 新增 UI 状态字段：
  - `isSyncing`, `lastSyncAt`, `showScanEntry`, `emptyReason`

### `SongListFragment` 改造
- 空态时显示：
  - 文案：“暂无音频”
  - 按钮：“扫描新音频”
- 非空时在顶部菜单或卡片提供“扫描新音频”入口。
- 手动点击触发 `MANUAL` 扫描，同步进度可用现有扫描页承载。

### `ScanProgressFragment` 改造
- 不再默认作为启动入口。
- 接收 `ScanMode` 参数。
- 扫描完成后返回列表页并刷新。

## 权限与异常策略
- 无存储权限：
  - 不触发扫描。
  - 列表展示空态与授权引导按钮。
- 扫描失败：
  - 保留旧数据（不清空已有库）。
  - 更新 `library_sync_state.lastScanStatus=FAILED`。
  - UI 提示“扫描失败，可重试”。
- 扫描取消：
  - 不覆盖现有库。
  - 记录状态为 `FAILED/CANCELED`。

## 测试方案（必须覆盖）

### 单元测试
- `AudioRepository`
  - 库空时 `ensureInitialScanIfNeeded` 返回 `RUN_INITIAL_SCAN`
  - 库非空时返回 `SKIP` 或 `RUN_INCREMENTAL_SYNC`
  - `scanAndSync` 成功后 Room 有数据且状态更新
- `AudioLibrarySyncService`
  - 全量 upsert 正确
  - 增量仅更新变更项
  - 删除对账正确
- `SongListViewModel`
  - 启动优先显示库数据
  - 无数据触发自动扫描状态
  - 手动扫描入口状态正确

### 集成测试（Robolectric）
- 首次启动无数据：
  - 进入列表页空态后自动扫描，完成后显示列表
- 二次启动有数据：
  - 不进入强制扫描流程，直接显示列表
- 新增音频后手动扫描：
  - 新音频出现
- 权限拒绝：
  - 不扫描，显示授权引导

### 回归测试
- 现有播放器列表点击、播放队列、播放页导航不回归。
- 扫描取消与失败不清空旧库。

## 验收标准（产品与技术）
- 冷启动首屏可在 `300ms~800ms` 内看到已有列表（设备差异允许）。
- 非首启不再出现“每次全盘扫描”行为。
- 新下载音频可在手动扫描后稳定入库并展示。
- 无权限/失败/取消路径均有清晰 UI 反馈，且不破坏已有数据。

## 分阶段落地（降低风险）
1. Phase 1：接入 Room，扫描结果落库，启动改为读库优先。  
2. Phase 2：导航起点改为列表页，空态+手动扫描入口上线。  
3. Phase 3：增量同步（MediaStore token）与删除对账。  
4. Phase 4：性能优化与埋点（扫描耗时、入库数量、失败率）。

## 关键接口/类型变更清单
- 新增：
  - `SongEntity`, `LibrarySyncStateEntity`, `SongDao`, `LibrarySyncStateDao`, `MusicDatabase`
  - `AudioLibrarySyncService`, `ScanMode`, `ScanDecision`
- 修改：
  - `AudioRepository`：由内存缓存改为 Room + 同步编排
  - `SongListViewModel`：增加启动决策与同步状态字段
  - `nav_graph`：起始页切换到 `songListFragment`

## 假设与默认值
- 默认使用 Room 作为唯一本地真源，内存仅做 UI 层缓存。
- 默认支持“自动首扫 + 手动重扫”，自动增量同步在库非空时后台触发。
- 默认扫描来源优先 `MediaStore`（更稳、更省电、更符合 Android 大厂实践）。
- 默认删除策略为硬删除（文件不存在即从库移除）。
