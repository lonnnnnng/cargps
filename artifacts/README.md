# 车机版验证产物

本目录只存放 `com.cargps` 在 `CASKA_1024x600` 上产生的本地验证资料。除本说明外，其余文件默认不进入 Git。

## 当前验证

- `cargps-split-car.png`：拆分为独立项目后的 1024x600 横屏截图。
- `cargps-split-car.xml`：同次验证的 UI Automator 树，可核对包名、文本和控件边界。

## 历史资料

- `caska-*.png`：CASKA 界面设计、权限、定位和行程状态截图。
- `cargps-car-*.png`：车机活动行程与 SQLite 恢复验证截图。
- `car-*.xml`：车机 UI Automator 状态快照。
- `release-v0.1.0/`、`release-v0.1.1/`：车机 APK、发布截图和 `SHA256SUMS`。

发布目录中的校验文件只列 `CarGPS-Car-*.apk`。移动或替换 APK 后必须重新运行 SHA-256 校验，不能把旧摘要当作新构建证据。
