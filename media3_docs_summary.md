# Media3 文档总结

## 来源

- https://developer.android.com/media/media3?hl=zh-cn
- https://developer.android.com/media/media3/session/player?hl=zh-cn
- https://developer.android.com/media/media3/session/control-playback?hl=zh-cn
- https://developer.android.com/media/media3/session/background-playback?hl=zh-cn
- https://developer.android.com/media/media3/session/connect-to-media-app?hl=zh-cn

## Media3 概览

- Media3 是 Android 媒体库的新平台，面向音视频播放与编辑，提供更一致的架构和可扩展能力。
- 组件核心围绕统一接口 `Player` 展开，`ExoPlayer` 与 `MediaController` 都实现该接口。
- 新的 `MediaSession` 不再依赖传统“连接器”，可直接接收任意 `Player` 实现，减少组件耦合。
- 典型组件组合：
  - `Player` 负责播放控制与状态。
  - `MediaSession` 负责向系统与其他应用暴露播放信息与控制能力。
  - `MediaController`/`MediaBrowser` 负责从客户端侧控制或浏览媒体。

## Player 接口要点

- `Player` 定义播放器常见能力：播放/暂停/跳转、播放属性查询、队列管理、随机/循环/速度/音量设置、视频输出等。
- `ExoPlayer` 是 `Player` 的默认实现，提供完整的播放内核与扩展能力。
- 多个组件都实现 `Player`，确保跨组件交互一致：
  - `ExoPlayer`：媒体播放器实现。
  - `MediaController`：通过会话对远端播放器进行控制。

## 使用 MediaSession 控制与通告播放

- `MediaSession` 连接 `Player` 后可对外发布播放信息，并接收来自系统或其他应用的播放命令。
- 常见命令来源：耳机/遥控器等硬件按钮、语音助手（如 Google 助理）、Wear OS、车机系统等。
- 适用场景：希望系统级控件、通知栏、蓝牙设备、外部应用可控制播放时，应实现 `MediaSession`。

## 后台播放（MediaSessionService）

- 如需在应用不在前台时继续播放，推荐将 `Player` 与 `MediaSession` 放入 `Service` 中。
- 使用 `MediaSessionService`（或 `MediaLibraryService`）可让会话独立于 UI 存活，保证后台播放稳定。
- 关键目标：
  - 后台持续播放
  - 通过通知、系统媒体控件等维持可控性
  - 在进程存活与资源管理之间取得平衡

## 连接到媒体应用

- 两种主要客户端入口：
  - `MediaController`：控制播放、查询播放状态，适合系统控件、穿戴设备、车机、语音助手等场景。
  - `MediaBrowser`：用于浏览媒体库内容，适合内容发现与导航。
- 基本流程：
  - 先获取 `SessionToken` 以定位目标 `MediaSession`。
  - 使用 `MediaController` 或 `MediaBrowser` 连接并发起控制/浏览请求。
- 当播放器与 UI 位于不同进程或 `Service` 中时，`MediaController` 也可用于应用内自连接以统一控制路径。

## 实践建议

- `Player` 作为核心抽象，确保应用内外的控制路径一致。
- 需要系统级控制时，优先接入 `MediaSession`。
- 需要后台播放时，把 `Player` 与 `MediaSession` 放入 `MediaSessionService`。
- 需要内容浏览时使用 `MediaBrowser`，仅控制播放则使用 `MediaController`。
