# GPS 车机版 v0.1.3

本版本将工程收敛为单个 Android 应用模块，并统一新的应用身份。

## 应用身份与工程

- 合并原 `car-app` 与 `gps-core` 到唯一 `app` 模块；界面、定位、NMEA、统计、SQLite 存储、单元测试与数据库仪器测试统一位于 `app/src/`。
- Kotlin 包名、namespace 与 applicationId 统一为 `com.lonng.cargps`。
- 启动器名称和仪表顶部名称均为 `GPS`。
- 版本名称 `0.1.3`，版本代码 `4`，最低支持 Android 8.1（API 27）。
- 不迁移旧应用的本地数据。

## 验证

- `:app:testDebugUnitTest`：40 项测试通过。
- `:app:compileDebugAndroidTestKotlin` 与 `:app:assembleDebugAndroidTest` 通过；SQLite 仪器测试已编译，未连接设备运行。
- `:app:lintRelease` 和 `:app:assembleRelease` 通过。
- 已核验 Release APK 的包名、显示名称、版本、最低 API 与正式签名证书。
- 未在真实车机上安装或路测。

## 下载

- `GPS-Car-v0.1.3.apk`：Release 安装包。
- `GPS-Release-Certificate.pem`：公开签名证书。
- `SHA256SUMS`：APK 与证书的 SHA-256 校验值。
