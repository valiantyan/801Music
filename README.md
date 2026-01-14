# AIDemo - 本地音乐播放器

## 项目简介

AIDemo 是一个基于 Android 平台的本地音乐播放器应用，专注于提供简洁、流畅的本地音频播放体验。应用支持扫描设备存储中的音频文件，提供直观的歌曲列表展示，并集成 Media3 播放引擎，实现系统级媒体通知和控制。

## 核心功能

### 1. 音频文件扫描
- 自动扫描设备 SD 卡中的音频文件
- 支持常见音频格式（MP3、AAC、FLAC、WAV 等）
- 后台异步扫描，不阻塞 UI

### 2. 歌曲列表展示
- 列表形式展示扫描到的所有歌曲
- 显示歌曲基本信息（标题、艺术家、时长等）
- 支持列表滚动和快速定位

### 3. 音乐播放
- 基于 Media3 1.9.0 播放引擎
- 点击歌曲立即播放
- 自动将当前列表添加到播放队列
- 支持播放控制（播放/暂停、上一首/下一首、进度控制）

### 4. 系统媒体集成
- 在系统媒体中心显示播放控制
- 通知栏显示播放通知和控制按钮
- 支持锁屏媒体控制
- 支持蓝牙设备媒体控制

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose（计划中）
- **播放引擎**: Media3 1.9.0
- **架构模式**: MVVM
- **异步处理**: Kotlin Coroutines + Flow
- **依赖注入**: Hilt（计划中）
- **最低 SDK**: Android 7.0 (API 24)
- **目标 SDK**: Android 14 (API 36)

## 项目结构

```
app/
├── src/main/
│   ├── java/com/valiantyan/aidemo/
│   │   ├── ui/              # UI 层（Activity、Fragment、Compose）
│   │   ├── viewmodel/       # ViewModel 层
│   │   ├── repository/      # 数据仓库层
│   │   ├── data/            # 数据层（本地数据库、文件系统）
│   │   ├── domain/          # 领域层（业务逻辑）
│   │   ├── player/          # 播放器模块（Media3 封装）
│   │   └── service/         # 后台服务（播放服务）
│   └── res/                 # 资源文件
```

## 开发规范

本项目遵循以下开发规范：
- 遵循 Kotlin 编码规范（见 `.cursor/rules/1000-kotlin-basics.mdc`）
- 采用 TDD 开发模式（测试驱动开发）
- 遵循敏捷工作流（见 `.cursor/rules/801-workflow-agile.mdc`）
- Git 提交信息遵循规范格式（见 `.cursor/rules/800-git-commit-format.mdc`）

## 依赖说明

### Media3 相关依赖
- `androidx.media3:media3-exoplayer` - 核心播放引擎
- `androidx.media3:media3-ui` - UI 组件
- `androidx.media3:media3-session` - 媒体会话管理
- `androidx.media3:media3-common` - 通用工具

### 其他核心依赖
- `androidx.lifecycle:lifecycle-viewmodel-ktx` - ViewModel
- `androidx.lifecycle:lifecycle-runtime-ktx` - Lifecycle
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` - 协程支持

## 权限要求

- `READ_EXTERNAL_STORAGE` - 读取外部存储（Android 12 及以下）
- `READ_MEDIA_AUDIO` - 读取音频文件（Android 13+）
- `FOREGROUND_SERVICE` - 前台服务（播放服务）
- `POST_NOTIFICATIONS` - 显示通知（Android 13+）

## 使用说明

### 扫描歌曲
1. 启动应用后，自动开始扫描设备存储中的音频文件
2. 扫描进度可在 UI 中查看
3. 扫描完成后，歌曲列表自动更新

### 播放音乐
1. 在歌曲列表中点击任意歌曲
2. 应用自动将当前列表添加到播放队列
3. 开始播放选中的歌曲
4. 可通过通知栏或锁屏界面控制播放

## 开发计划

详见 `.ai/prd/` 目录下的产品需求文档。

## 许可证

[待定]
