# Issue Tracker Configuration (任务工单与需求跟踪器配置)

- **Type**: `local-markdown`
- **Directory**: `.scratch/shop-platform/issues`
- **Spec Directory**: `.scratch/shop-platform/spec`

> **归档状态说明**：`shop-platform` 的 6 份工单（`0001` - `0006`）与总体规格 `spec/spec.md` 已在交付后从工作区移除，仅保留在 git 历史中（最近一次提交仍包含该目录）。
>
> - 查看规格：`git show HEAD:.scratch/shop-platform/spec/spec.md`
> - 查看工单：`git show HEAD:.scratch/shop-platform/issues/0004-product-and-showcase.md`（替换编号即可）
> - 恢复全部归档文件：`git checkout HEAD -- .scratch/shop-platform`
>
> 后续若在新分支上继续使用工单流程，请先执行上面的恢复命令，再按 `local-markdown` 约定在 `.scratch/shop-platform/issues` 中新建工单。
