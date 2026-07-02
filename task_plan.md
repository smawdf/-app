# OrderDisk 重构方案任务计划

## 目标
- 深入分析当前 Android 项目结构与关键实现。
- 输出中文重构文档，目标为将现有双人点菜/菜谱应用演进为美团式点餐应用。
- 尽量写入 `D:\hermes\orderdisk-refactor-plan.md`；若权限受限，先生成仓库内副本并说明。

## 阶段
- [complete] 收集项目结构与关键代码上下文
- [complete] 提炼现状分析、痛点、关键文件
- [complete] 设计目标架构、数据源、UI 路线、数据库变更
- [complete] 生成 Markdown 文档
- [complete] 处理目标输出路径限制：最终文档保存在仓库 `docs/`，旧草稿归档到 `docs/refactor/`
- [complete] 检查 Phase 2-4 重构完成度并整理项目文件
- [complete] 验证 Phase 2-4：单元测试、Debug APK、Release APK 均通过

## 已知约束
- 当前工作区仅允许写入 `D:\kaifa\myapp\OrderDisk` 与 `D:\tmp`。
- 需要遵循项目 AGENTS 指南，优先读取真实代码，不做臆测。
- 当前工作区存在大量未提交源码和未跟踪文件；整理时不得回滚或删除业务源码。
