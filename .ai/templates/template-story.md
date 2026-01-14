---
id: "{{id}}"  # Story ID, e.g., UA001
title: "{{title}}" # Short description
type: Feature # Options: Feature, Bug, Spike
epic_id: "{{epic_id}}" # Feature ID/Name, e.g., user-authentication
feature_title: "{{feature_title}}"
target_version: "{{target_version}}"
status: 草稿 # Options: 草稿, 进行中, 已完成, 已阻塞
priority: Medium # High, Medium, Low
assignee: AI_Agent
created_date: "{{date}}"
---

# Story: {{title}}

## 1. Background & Context (背景与上下文)
*简要描述为什么需要这个 Story，以及它与 Feature 目标的关联。如果是 Bug，请描述缺陷现象。*

## 2. User Story (用户故事)
> As a **[User Role]**, I want to **[Action]**, so that **[Benefit]**.

## 3. Acceptance Criteria (验收标准) - DoD
*必须满足以下所有条件才能视为完成。*
- [ ] **AC1**: [具体标准]
- [ ] **AC2**: [具体标准]
- [ ] **UI/UX**: 界面符合设计/预览效果 (如有)
- [ ] **Unit Tests**: 核心逻辑单元测试通过
- [ ] **Doc Sync**: 检查并更新了相关架构文档/PRD (如需)

---

## 4. Technical Design & Implementation (技术设计与实现)
*在开始编码前，简要规划实现思路。遵守 YAGNI 原则，只设计当前需要的。*

### 4.1 Affected Modules (受影响模块)
*   `[Module/File Name]`

### 4.2 Key Changes (关键改动)
*   [描述改动点 1]
*   [描述改动点 2]

### 4.3 Dependencies (依赖)
*   [列出需要 Mock 的外部依赖]

---

## 5. Development Steps (开发步骤 - TDD/Task Breakdown)
*将 Story 拆解为微小的、可测试的步骤。*

- [ ] **Task 1**: [描述]
    - *Test Case*: [描述测试意图]
- [ ] **Task 2**: [描述]
- [ ] **Task 3**: [描述]

---

## 6. Development Notes & Log (开发笔记与日志)
*记录开发过程中的思考、遇到的问题及其解决方案。这是"未完成 Story"时的 Bug 记录处。*

#### Log Structure (Example):
> - [YYYY-MM-DD HH:MM] **Action**: 开始任务 Task 1。
> - [YYYY-MM-DD HH:MM] **Issue**: 发现空指针异常。
>   - **Fix**: 增加空值检查。
> - [YYYY-MM-DD HH:MM] **Decision**: 为了方便测试，提取了 `Helper` 类。

**(Current Log):**
- [ ] 
