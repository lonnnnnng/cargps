# 车机版验证产物

本目录存放车机应用 `com.cargps` 的本地验证资料。除本说明外，其余文件默认不进入 Git。

## 当前验证

- `release-v0.1.2/`：v0.1.2 车机 Release APK、公开签名证书与 `SHA256SUMS`；APK 使用 CarGPS 专用发布密钥签名。旧调试签名包留存在 `previous-debug-signed/`，不再作为 Release 附件。
- `ui-redesign-20261126/`：本轮调试 APK、SHA256SUMS、API 27/28 离线原生渲染及测试/Lint 报告。
- `night.png` / `day-searching.png` / `large-font.png`：从 API 27 原生渲染图裁取的 1024×528 应用内容区。使用测试数据，非道路实测。
- `api-27/` 与 `api-28/`：Robolectric 原始图，外部灰色区域属于测试宿主背景，不是车机系统栏。
- Debug APK 使用本机调试签名；未正式发布，不保证能覆盖使用不同签名的已安装版本。

## 历史资料

以下为历史产物名称索引，当前检出不一定含有这些文件：

- `caska-*.png`：CASKA 界面设计、权限、定位和行程状态截图。
- `cargps-car-*.png`：车机活动行程与 SQLite 恢复验证截图。
- `car-*.xml`：车机 UI Automator 状态快照。
- `release-v0.1.0/`、`release-v0.1.1/`：车机 APK、发布截图和 `SHA256SUMS`。

发布目录中的校验文件只列 `CarGPS-Car-*.apk`。移动或替换 APK 后必须重新运行 SHA-256 校验，不能把旧摘要当作新构建证据。
