# AGENTS.md

## 1. 目的与分层

本项目使用“AGENTS + SPEC + SKILL”推进开发与管理。

分层约定：
- AGENTS：全局规则与工作流，所有任务必须遵循。
- SPEC：功能范围与验收标准，具体功能以对应 SPEC 为唯一真源。
- SKILL：专项规范或流程工具，按任务需要加载并执行。

## 2. 工作流

- 先读 AGENTS，再读对应 SPEC。
- 没有 SPEC 先补写 SPEC，再开始实现。
- 先实现核心功能，再优化。
- 每个有意义变更后执行相关测试。
- 完成后同步更新文档与测试记录。

## 3. 规范优先级

- `.cursor/rules/` 为唯一最高准则，所有新增或修改内容必须遵循。
- 若 AGENTS 与 `.cursor/rules/` 冲突，以 `.cursor/rules/` 为准。

## 4. 语言与编码规范（Kotlin）

- 代码使用英文；注释与文档使用中文。
- 变量、参数、返回值必须显式类型声明，避免 `Any`。
- 函数体内不留空行，函数 < 20 条语句。
- 类 < 200 行且公开方法 < 10。
- 2 个及以上参数使用命名参数；3 个及以上参数一行一个并保留尾随逗号。
- 命名规范：类 `PascalCase`、函数/变量 `camelCase`、常量 `UPPERCASE`、布尔以 `is/has/can` 开头。
- 优先使用 `data class` 建模，优先 `val`。
- 校验逻辑放在类内部，遵循 SOLID 与组合优先。

## 5. 异常处理

- try 块内记录执行进度日志。
- 捕获具体异常并补充上下文，禁止吞异常。

## 6. 测试规范

- 每个公共函数尽量补充单元测试。
- 结构遵循 Arrange-Act-Assert。
- 测试命名：`method_condition_expectedResult`。
- 测试变量命名：`inputX`、`mockX`、`actualX`、`expectedX`。

## 7. 文档规范

- 所有 Markdown 文档遵循 `.cursor/rules/400-md-docs.mdc`。
- 如需日期，使用北京时间。

## 8. 提交与 PR

- 建议使用 Conventional Commits（带 scope）。
- PR 需包含变更摘要、关联任务、必要截图与本地测试结果。

## 9. SPEC 入口

- Room 改造分阶段 SPEC 位于 `specs/audio-scan-room-migration/`。
- 实现任务必须显式选择一个 SPEC 作为当前目标。
