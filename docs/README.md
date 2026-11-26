# 车机版资料索引

本目录只保存 GPS 车机版的产品、技术和发布文档。目标设备固定为 `CASKA_1024x600`，Android 8.1 / API 27，1024x600 横屏。

## 产品与技术

- [产品规格](./product-spec.md)：车机信息层级、状态、交互和 CASKA 验收口径。
- [技术设计](./technical-design.md)：API 27 定位实现、数据流、模块 seam、持久化和测试重点。
- [2026-11-26 界面改造与验证](./ui-redesign-20261126.md)：速度与时钟主屏、性能优化、验证范围与产物。
- [ADR-0001](./adr/0001-use-platform-locationmanager.md)：采用系统 `LocationManager` 作为唯一定位主来源。

## 发布记录

- [v0.1.4](./release-notes-v0.1.4.md)：启用 R8 与资源压缩，缩小安装包。
- [v0.1.3](./release-notes-v0.1.3.md)：单模块合并、应用包名和名称更新。
- [车机发布签名](./release-signing.md)：Release 签名密钥的本机位置、备份和构建设置。
- [v0.1.2](./release-notes-v0.1.2.md)：车机主屏重设计、定位诊断及行程性能优化。
- [v0.1.0](./release-notes-v0.1.0.md)：首个车机正式版本。
- [v0.1.1](./release-notes-v0.1.1.md)：拆分前联合发布版本中的车机侧记录，车机 UI 行为保持不变。

项目首页使用的[夜间效果图](images/night.png)保存在 `docs/images/`，随源码提交，供 README 展示。完整验证截图、UI Automator XML、APK 和 `SHA256SUMS` 统一归档于 [artifacts](../artifacts/README.md)；本轮原生离线预览位于 `artifacts/ui-redesign-20261126/`。
