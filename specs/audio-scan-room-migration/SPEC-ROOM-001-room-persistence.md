---
id: "SPEC-ROOM-001"
title: "音频扫描落库与启动读库（Phase 1）"
type: Spec
epic_id: "FEAT-001"
feature_title: "本地音乐播放器"
target_version: "v1.1"
status: "已完成"
priority: P0
owner: "产品经理"
tech_owner: "客户端负责人"
created_date: "2026-02-06"
---

# SPEC-ROOM-001 音频扫描落库与启动读库（Phase 1）

## 1. 背景与问题

当前应用启动后默认执行全量扫描，首屏等待时间长且耗电高。现有架构为单 Activity + 多 Fragment，使用 MVVM + MVI 混合模式与 Clean Architecture。为了做到首屏优先展示已有库，需要将扫描结果持久化到 Room，并将列表的数据真源切换到数据库。

## 2. 目标（Goals）

- 首次使用或数据库为空时自动全量扫描并落库。
- 库非空时启动优先读库，首屏展示不依赖扫描流程。
- 建立最小可用的同步状态记录，支撑后续手动增量扫描。

## 3. 非目标（Non-Goals）

- 不实现目录选择 UI。
- 不实现增量扫描与删除对账。
- 不引入外部埋点平台。

## 4. 范围（Scope）

### 4.1 In Scope

- 新增 `songs` 与 `library_sync_state` 表。
- 列表改为订阅 Room 数据流。
- 首扫决策：自动首扫或跳过扫描。
- 全量扫描成功/失败状态写入 `library_sync_state`。

### 4.2 Out of Scope

- 导航入口调整与目录选择页面。
- 启动自动增量扫描。
- 扫描性能专项优化。

## 5. 工作流（Workflow）

### 5.1 入口条件

- App 启动并进入列表业务初始化。
- 具备读取媒体权限。

### 5.2 主流程

1. ViewModel 订阅 `observeSongs()`，先渲染本地已有数据。
2. Repository 执行 `ensureInitialScanIfNeeded()`。
3. 若库为空，触发 `FULL_INITIAL` 全量扫描并落库。
4. 若库非空，跳过扫描，继续展示数据库内容。
5. 更新 `library_sync_state` 状态字段。

### 5.3 异常流程

- 权限拒绝：不触发扫描，返回空态与授权引导状态。
- 扫描失败：保留旧库，写入 `lastScanStatus=FAILED` 与 `lastError`。
- 扫描取消：不覆盖旧库，写入失败或取消状态。

### 5.4 退出条件

- 至少满足其一：
1. `songs` 表有可展示数据。
2. 明确返回可恢复错误状态（权限拒绝/扫描失败）。

## 6. 需求定义

### 6.1 功能需求（FR）

- FR1：新增 Room 数据库与表结构，持久化歌曲数据。
- FR2：列表展示的数据源改为数据库查询 Flow。
- FR3：提供“是否需要首扫”的决策能力。
- FR4：全量扫描完成后更新同步状态。
- FR5：扫描失败时不清空旧库，保留可用数据。

### 6.2 非功能需求（NFR）

- 启动读库耗时不显著增加，首屏展示目标在 300ms~800ms 范围内。
- 扫描落库应在后台线程执行，避免阻塞主线程。

## 7. 设计方案

### 7.1 数据模型

- `songs` 表：
  - 主键 `id`：优先使用 MediaStore 音频 ID 转字符串；无 ID 时回退为 `filePath`。
  - 必要字段：`title`、`artist`、`album`、`duration`、`filePath`、`fileSize`、`dateAdded`、`albumArtPath`、`modifiedAt`、`scannedAt`。
  - 索引：`filePath` 唯一索引；`dateAdded`、`modifiedAt` 普通索引。

- `library_sync_state` 表：单行表（`id=1`）。
  - 字段：`lastScanAt`、`lastFullScanAt`、`lastSyncToken`、`lastScanStatus`、`lastError`。

### 7.2 Repository 改造

- `AudioRepository` 职责从内存缓存改为数据库真源。
- 对外新增/调整接口：
- `observeSongs()`：返回 `Flow<List<Song>>`，底层读 Room。
- `ensureInitialScanIfNeeded()`：返回首扫决策结果。
- `scanAndSync(ScanMode)`：执行全量扫描并落库。

### 7.3 扫描执行

- 复用现有文件系统扫描器进行全量扫描。
- 以批量 upsert 写入 Room。
- 更新 `library_sync_state` 的 `lastScanAt`、`lastFullScanAt` 与 `lastScanStatus`。

### 7.4 架构对齐

- 保持 Clean Architecture 分层：UI → ViewModel → Repository → DataSource。
- 所有 IO 使用 `Dispatchers.IO` 执行。

## 8. 验收标准（DoD）

- AC1：库为空时触发全量扫描，扫描完成后 `songs` 非空。
- AC2：库非空时启动优先展示列表，首屏展示不依赖扫描流程。
- AC3：`library_sync_state` 在扫描完成后正确更新。
- AC4：扫描失败不清空旧数据，保留可用歌曲列表。
- AC5：扫描与写库全程不阻塞主线程。

## 9. 测试计划

- 单元测试：
- `ensureInitialScanIfNeeded()` 在库空时返回 `RUN_INITIAL_SCAN`。
- `ensureInitialScanIfNeeded()` 在库非空时返回 `SKIP_ALREADY_HAS_DATA`。
- `scanAndSync()` 成功后写入 Room 并更新同步状态。

- 集成测试（Robolectric）：
- 首次启动无数据可完成扫描并展示列表。
- 二次启动有数据不进入全量扫描流程。
- 权限拒绝时不触发扫描且返回授权引导状态。

## 10. 监控与可观测性

- 记录扫描开始、完成与失败日志。
- 保留 `library_sync_state.lastScanStatus` 作为最小可观测字段。

## 11. 发布与回滚

- 灰度策略：先在内部测试渠道验证。
- 回滚策略：保留旧扫描逻辑开关，必要时回退到内存缓存方案。

## 12. 风险与应对

- 风险：Room 写入批量过大导致卡顿。
- 应对：分批 upsert 与 IO 线程执行。

## 13. 兼容性与权限

- 权限处理沿用现有存储权限逻辑。
- API 24+ 兼容策略保持不变。

## 14. 安全与隐私

- 仅存储本地文件元数据，不上传网络。
- 遵循最小权限原则。

## 15. 依赖与前置

- Room 依赖与编译配置（KSP 或 KAPT）。
- 现有扫描器可复用。

## 16. 决策结论

- 首次使用或库为空时采用自动全量扫描，无需用户二次确认。
