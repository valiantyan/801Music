# 项目架构与播放器实现分析

## 概览
本项目为 Android 本地音乐播放器，采用单 Activity + 多 Fragment + Navigation 架构。核心播放能力已抽取为强隔离模块：`player-api`（接口与模型）与 `player-impl`（Media3 具体实现），App 仅依赖 API 层以降低耦合并便于替换播放实现。

## 模块划分
- `app`：UI、导航、权限、ViewModel 等应用层逻辑。
- `player-api`：播放控制接口与模型（`PlayerController`、`PlaybackState`、`Song`、`PlayerCommands`）。
- `player-impl`：Media3 实现、`MediaSessionService`、通知、控制器工厂与自动初始化。

## 关键目录
- `app/src/main/java/com/valiantyan/music801/ui/`：界面与交互。
- `app/src/main/java/com/valiantyan/music801/viewmodel/`：UI 状态与业务控制。
- `player-api/src/main/java/com/valiantyan/music801/`：对外契约（接口/模型）。
- `player-impl/src/main/java/com/valiantyan/music801/`：Media3 播放实现与后台服务。

## 播放器执行链路
1. **UI 触发**：`PlayerFragment`/`SongListFragment` 通过 `PlayerViewModel` 调用 `PlayerController`。
2. **控制器连接**：`MediaControllerManager` 使用 `MediaController` 连接 `MusicPlayerService` 的 `MediaSession`。
3. **服务创建**：`MusicPlayerService` 在 Service 内创建 `ExoPlayer` 与 `MediaSession`，并挂接通知管理。
4. **命令执行**：播放、切歌、seek 等命令走 `MediaController` -> `MediaSession` -> `ExoPlayer`。
5. **状态同步**：`MediaControllerManager` 监听 `Player.Listener` 并定时刷新，更新 `PlaybackState`。
6. **自定义命令**：收藏按钮通过 `PlayerCommands.ACTION_TOGGLE_FAVORITE` 发送到 `MediaSession.Callback`。

## 强隔离策略
- **API 层仅暴露接口**：`player-api` 不依赖 Media3。
- **实现层 internal**：`player-impl` 内部类尽量 `internal`，避免被 app 直接引用。
- **Startup 注册**：`PlayerControllerInitializer` 使用 AndroidX Startup 自动注册控制器工厂。

## 架构 Mermaid
```mermaid
flowchart LR
  subgraph App[app 模块]
    UI[Fragment/Activity]
    VM[ViewModel]
    UI --> VM
  end

  subgraph API[player-api 模块]
    PC[PlayerController 接口]
    PS[PlaybackState]
    S[Song]
    CMD[PlayerCommands]
  end

  subgraph Impl[player-impl 模块]
    MCM[MediaControllerManager]
    SVC[MusicPlayerService]
    MS[MediaSession]
    EP[ExoPlayer]
    NOTI[PlayerNotificationManager]
    INIT[PlayerControllerInitializer]
  end

  UI --> PC
  VM --> PC
  PC -.-> MCM
  INIT --> MCM
  MCM --> MS
  SVC --> MS
  MS --> EP
  MS --> NOTI
  CMD --> MCM
  CMD --> SVC
```

## 播放器组件职责
- **`PlayerController`**：对外播放控制 API（播放、暂停、切歌、seek、收藏）。
- **`MediaControllerManager`**：封装 `MediaController` 连接、队列与状态同步。
- **`MusicPlayerService`**：持有 `ExoPlayer` 与 `MediaSession`，处理系统命令与自定义命令。
- **`PlayerNotificationManager`**：媒体通知与前台服务控制。

## 关键差异与修正
- 已严格对齐官方 Media3 架构：`ExoPlayer` 与 `MediaSession` 在 `MediaSessionService` 中创建并管理。
- UI 完全通过 `MediaController` 驱动，避免直接触碰底层播放器。
- 收藏按钮作为 `SessionCommand` 自定义命令实现，并可通过 `PlayerCommands` 对外稳定暴露。

## 推荐后续扩展
- 收藏状态持久化：Room/DataStore。
- `PlaybackState` 扩展收藏字段并通知 UI。
- 自定义命令添加结果反馈与 UI 展示（Toast/图标切换）。

## 播放器执行流程图（详细版）
```mermaid
flowchart LR
  %% UI 触发
  subgraph UI[UI 层]
    U1[用户点击播放/切歌/收藏]
    U2[PlayerFragment/SongListFragment]
    U3[PlayerViewModel]
  end

  %% API 层
  subgraph API[player-api]
    A1[PlayerController]
    A2[PlayerCommands.ACTION_TOGGLE_FAVORITE]
    A3[PlaybackState]
  end

  %% 控制层
  subgraph Control[控制层]
    C1[MediaControllerManager]
    C2[MediaController]
    C3[SessionToken]
  end

  %% 服务层
  subgraph Service[服务层]
    S1[MusicPlayerService]
    S2[MediaSession]
    S3[PlaybackSessionCallback]
    S4[PlayerNotificationManager]
  end

  %% 播放层
  subgraph Playback[播放层]
    P1[ExoPlayer]
    P2[MediaItem/MediaMetadata]
  end

  %% 系统层
  subgraph System[系统/外部]
    Y1[通知栏/锁屏]
    Y2[蓝牙/车机]
  end

  %% 主链路
  U1 --> U2 --> U3 --> A1 --> C1
  C1 -->|未连接| C3 --> C2
  C2 -->|发送控制命令| S2 --> P1
  P1 -->|状态变化| S2 --> C2 --> C1 --> A3
  A3 --> U3 --> U2

  %% 队列与媒体项
  C1 -->|setQueue| P2 --> C2 --> S2 --> P1

  %% 自定义命令（收藏）
  U1 -->|收藏操作| U3 --> A1
  A1 --> A2 --> C1
  C1 -->|sendCustomCommand| C2 --> S2 --> S3
  S3 -->|处理收藏命令| S2

  %% 通知与系统交互
  S2 --> S4 --> Y1
  Y1 -->|系统控制| S2
  Y2 -->|媒体控制| S2
```
