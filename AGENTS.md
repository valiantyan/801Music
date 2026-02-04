# Repository Guidelines

# Coding Standards
所有代码编写必须遵循 `.cursor/rules/` 目录下的规范要求。任何新增或修改的代码、测试、注释、提交信息、文档更新等，均需以该目录中的规则为唯一准则。
好好干，你不干，有的是其他 AI 干

## Project Structure & Module Organization
- `app/` 为唯一模块，包含 Android 应用源码与资源。
- `app/src/main/` 放置生产代码与资源：`java/com/valiantyan/music801/`、`res/`、`AndroidManifest.xml`。
- `app/src/test/` 为本地单元测试；`app/src/androidTest/` 为仪器测试。
- 根目录的 `README.md`、`media3_*_summary.md` 为设计与调研说明，便于快速理解架构与媒体播放方案。

## Build, Test, and Development Commands
- `./gradlew assembleDebug`：构建 Debug APK。
- `./gradlew test`：运行本地单元测试（Robolectric、JUnit）。
- `./gradlew connectedAndroidTest`：运行设备/模拟器上的仪器测试。
- `./gradlew spotlessCheck`：检查 Kotlin/Gradle 格式是否符合 `ktlint`。
- `./gradlew spotlessApply`：自动修复格式问题。

## Coding Style & Naming Conventions
- 语言为 Kotlin，遵循显式类型声明（变量、参数、返回值）；避免 `Any`。
- 函数体内不留空行；函数 < 20 条语句，类 < 200 行且公开方法 < 10。
- 2 个及以上参数使用命名参数；3 个及以上参数一行一个并保留尾随逗号。
- 命名规范：类 `PascalCase`、函数/变量 `camelCase`、常量 `UPPERCASE`、布尔以 `is/has/can` 开头。
- 使用 `data class` 建模，优先 `val`；在类内部进行校验。

## Testing Guidelines
- 单测采用 Arrange-Act-Assert；命名格式 `method_condition_expectedResult`。
- 测试变量命名使用 `inputX`、`mockX`、`actualX`、`expectedX`。
- 尽量为公共函数补充单测；依赖使用 Test Double 隔离。

## Commit & Pull Request Guidelines
- 历史提交多使用 Conventional Commits：`feat(scope):`、`fix(scope):`、`refactor(scope):`、`docs(scope):`；也存在简短描述式提交。新增提交建议优先使用带 scope 的 Conventional 格式。
- PR 需包含：变更摘要、关联问题/任务、必要的截图（UI 相关）、以及本地测试结果（命令与结论）。

## Agent-Specific Instructions
- 公共 API 需提供完整中文 KDoc，并使用 `[]` 引用相关元素。
- try 块内记录执行进度，捕获具体异常并补充上下文，禁止吞异常。
