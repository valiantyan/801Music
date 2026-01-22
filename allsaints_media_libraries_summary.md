# Media3 Libraries Summary

## 概览

本次阅读范围：

- `/Users/yanhao/Desktop/allsaints_code/media/libraries/common`
- `/Users/yanhao/Desktop/allsaints_code/media/libraries/common_ktx`
- `/Users/yanhao/Desktop/allsaints_code/media/libraries/exoplayer`
- `/Users/yanhao/Desktop/allsaints_code/media/libraries/session`

整体上，这是一套 AndroidX Media3 的核心库组合：`common` 提供基础模型与接口，`exoplayer` 提供播放实现，`session` 提供跨进程/系统集成的媒体会话能力，`common_ktx` 提供 Kotlin 扩展能力。

## 文件规模

| 模块 | 文件数 | 主要语言/类型 | 备注 |
|:--|--:|:--|:--|
| common | 217 | 211 Java | 基础数据模型与工具类为主 |
| common_ktx | 6 | 2 Kotlin | Player 相关协程扩展 |
| exoplayer | 610 | 522 Java + 1 Kotlin | 播放内核与渲染管线 |
| session | 268 | 88 Java + 3 AIDL | 会话/控制器与兼容层 |

## 模块总结

### common

定位：Media3 的基础数据模型、接口与通用工具库，其他模块都依赖它。

核心内容：

- 播放器抽象与基础实现：`Player`、`BasePlayer`、`SimpleBasePlayer`、`ForwardingPlayer`，统一播放控制与状态回调。
- 媒体与轨道模型：`MediaItem`、`MediaMetadata`、`Timeline`、`TrackGroup`、`Tracks`、`TrackSelectionOverride`、`TrackSelectionParameters`、`Format`、`MimeTypes`。
- 播放状态与错误体系：`PlaybackException`、`IllegalSeekPositionException`、`PlaybackParameters`、`PlayerTransferState`。
- 音视频基础能力：`AudioAttributes`、`VideoSize`、`DeviceInfo`、`SurfaceInfo`、`FrameInfo`、`VideoFrameProcessor`、`VideoGraph`。
- 广告与覆盖层：`AdPlaybackState`、`AdOverlayInfo`、`AdViewProvider`、`OverlaySettings`。
- 文本与样式：`text` 包含 `Cue`、`CueGroup` 与 `RubySpan`、`TextEmphasisSpan` 等文本样式 span。
- 音频处理：`audio` 包含音频焦点、音频处理链、变速与采样处理（`AudioFocusManager`、`AudioProcessor`、`SonicAudioProcessor` 等）。
- 通用工具：`util` 包含线程/时钟/日志、解析器、GL 工具、系统锁、网络状态等（`Util`、`Clock`、`TimestampAdjuster`、`ParsableByteArray`、`GlUtil` 等）。

依赖/构建特性：

- Gradle 中使用 Guava、AndroidX 注解与 checker/JSR305（主要为编译期注解）。
- 作为所有 Media3 模块的版本对齐约束基座。

### common_ktx

定位：为 Media3 提供 Kotlin 友好的 API 扩展。

核心内容：

- `PlayerExtensions.kt` 提供 `Player.listen` 与 `Player.listenTo` 两个协程扩展方法：
  - 将 `Player.Listener.onEvents` 以协程方式持续监听。
  - 支持事件过滤（指定事件集合）。
  - 使用 `suspendCancellableCoroutine`，在取消/异常时释放监听器并抛出异常。
  - 保证回调在 `Player.getApplicationLooper` 线程执行。

依赖/构建特性：

- Kotlin 插件与 `kotlinx-coroutines`。
- 依赖 `common` 与 `androidx.core`。

### exoplayer

定位：Media3 的实际播放器实现，提供可扩展、组件化的播放内核。

核心内容：

- 播放器实现：`ExoPlayer`、`SimpleExoPlayer`、`ExoPlayerImpl` 及内部调度（`ExoPlayerImplInternal`、`PlaybackInfo`）。
- 渲染与解码：`Renderer`、`MediaCodecAudioRenderer`、`MediaCodecVideoRenderer`、`MetadataRenderer`、`TextRenderer`，并提供 `DefaultRenderersFactory`。
- 媒体源与缓冲：`source` 包含 `MediaSource`/`MediaPeriod` 体系、`DefaultMediaSourceFactory`、`ConcatenatingMediaSource`、`ClippingMediaPeriod`、`SampleDataQueue` 等。
- 轨道选择与加载控制：`trackselection`（`DefaultTrackSelector` 等）、`LoadControl`/`DefaultLoadControl`。
- 音频/视频/字幕：`audio`、`video`、`text` 子包包含输出、处理、渲染与同步控制实现。
- DRM 与离线：`drm`、`offline` 支持许可证、下载与离线播放。
- 统计与带宽：`analytics`、`upstream` 支持带宽采样与播放统计。
- 资源与本地化：`res/values-*` 提供多语言字符串资源。

依赖/构建特性：

- 依赖 `common`、`container`、`datasource`、`decoder`、`extractor`、`database` 等模块。
- 提供丰富的单元测试与 Android instrumentation 测试。

### session

定位：媒体会话与跨进程控制层，向系统与其他应用暴露播放控制和媒体信息。

核心内容：

- 会话与控制器：`MediaSession`、`MediaController`、`MediaBrowser`，对外暴露播放控制、队列与状态。
- 服务与生命周期：`MediaSessionService`、`MediaLibraryService`，用于后台播放与媒体库能力。
- 会话通信：`IMediaSession.aidl`、`IMediaController.aidl`、`IMediaSessionService.aidl`，以及对应 Stub/Impl。
- 通知与交互：`MediaNotification`、`DefaultMediaNotificationProvider`、`MediaStyleNotificationHelper`、`MediaNotificationManager`。
- 连接与命令体系：`SessionCommand`、`SessionCommands`、`SessionResult`、`SessionError`、`ConnectionState`。
- 兼容层：`legacy` 包含 `MediaSessionCompat`、`MediaControllerCompat` 等旧版 API 的兼容实现与工具。
- 资源：多语言 strings、样式、颜色、通知相关资源。

依赖/构建特性：

- 依赖 `common`、`datasource`、`androidx.media`、`androidx.core`。
- 启用 AIDL，包含完整测试覆盖。

## 模块关系

- `common` 定义核心模型与 `Player` 抽象。
- `exoplayer` 以 `Player` 为核心提供具体实现与渲染管线。
- `session` 以 `Player` 为后端，通过会话/控制器对外暴露播放能力。
- `common_ktx` 为 `common` 提供 Kotlin 协程扩展与更友好的 API。
