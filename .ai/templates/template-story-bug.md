---
id: "BUG-{{id}}"  # e.g., BUG-102
title: "{{title}}" # e.g., Login Crash on Android 12
type: Bug
epic_id: "{{epic_id}}" # Related Feature ID
target_version: "{{target_version}}"
status: 草稿 # Options: 草稿, 进行中, 已完成
severity: High # Critical, High, Medium, Low
created_date: "{{date}}"
---

# Bug: {{title}}

## 1. Defect Description (缺陷描述)
*详细描述 Bug 的表现。*

### 1.1 Reproduction Steps (重现步骤)
1.  [Step 1]
2.  [Step 2]
3.  [Step 3]

### 1.2 Expected Behavior (预期行为)
*[描述应该发生什么]*

### 1.3 Actual Behavior (实际行为)
*[描述实际发生了什么]*

## 2. Root Cause Analysis (根因分析) - To be filled during investigation
*   **Cause**: [待分析]
*   **Impact Area**: [受影响的代码范围]

## 3. Fix Strategy (修复策略)
*   [描述修复计划]

## 4. Verification & Regression (验证与回归) - DoD
- [ ] **Test Case**: 新增/修复了一个测试用例以复现此 Bug (Red state)
- [ ] **Verification**: Bug 已在本地修复 (Green state)
- [ ] **Regression**: 运行相关模块测试，确保无回归问题
- [ ] **Doc Sync**: 确认修复是否改变了系统行为，若是，反向更新 PRD/Arch

## 5. Development Log (修复日志)
*记录修复过程。*

- [ ] 
