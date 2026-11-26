# GPS 车机版

GPS 是面向 1024×600 横屏车机的离线 GPS 仪表，不依赖地图服务或 Google Play Services。本仓库只维护一个车机 Android 应用模块。

当前版本：[v0.1.4](https://github.com/lonnnnnng/cargps/releases/tag/v0.1.4) · [更新说明](./docs/release-notes-v0.1.4.md)

[下载 GPS-Car-v0.1.4.apk](https://github.com/lonnnnnng/cargps/releases/download/v0.1.4/GPS-Car-v0.1.4.apk) · [SHA-256 校验文件](https://github.com/lonnnnnng/cargps/releases/download/v0.1.4/SHA256SUMS)

Release 安装包约 **0.87 MiB**，比 v0.1.3 缩小约 **91.5%**；使用 R8 与资源压缩移除未使用的代码和资源。

## 工程结构

```text
cargps/
├── app/           # 唯一应用模块，包名 com.lonng.cargps
│   └── src/
│       ├── main/
│       ├── test/
│       └── androidTest/
├── docs/
├── gradle/
├── gradlew
└── settings.gradle.kts
```

界面、定位、行程统计、SQLite 存储和测试统一放在 `app/src/`，namespace 与 applicationId 均为 `com.lonng.cargps`。

Kotlin 源码位于 `app/src/main/java/com/lonng/cargps/`，按 `ui`、`domain`、`storage` 组织职责，不再拆成独立 Gradle 库模块。

## 车机版目标设备

- 目标车机：CASKA
- Android：8.1.0（API 27）
- 屏幕：1024×600、160 dpi、横屏
- 本轮使用 Robolectric 在本机验证 API 27/28；当前没有车机模拟器，实车验证需连接目标设备。

## 主要能力

- 显示瞬时速度、本次行程平均速度、移动平均速度和最高速度。
- 显示时间、经纬度、海拔、方向、定位精度、卫星数量和定位更新时间。
- 支持开始、暂停、继续、结束行程，并统计里程、总时长、移动时长和停车时长。
- 解析 GNSS NMEA 报文，补充定位质量与诊断信息。
- 本地保存行程，断网可用，不把定位数据上传到服务器。
- 提供 1024x600 横屏、大字号、高对比度、少操作的驾驶仪表。
- 主屏突出瞬时速度和时分秒；年月日单独分色，GPS/北斗数量和 DOP 移入定位详情。
- 增量计算行程统计，暂停/恢复与后台切换保存路段边界；诊断数据独立过期。

## 文档与验证资料

- [领域词汇](./CONTEXT.md)
- [资料索引](./docs/README.md)
- [产品规格](./docs/product-spec.md)
- [Android 技术设计](./docs/technical-design.md)
- [ADR-0001：采用系统 LocationManager](./docs/adr/0001-use-platform-locationmanager.md)
- [v0.1.4 更新说明](./docs/release-notes-v0.1.4.md)
- [界面改造与验证记录](./docs/ui-redesign-20261126.md)
- [界面规则](./DESIGN.md)
- [正式签名与构建](./docs/release-signing.md)
- [截图、UI 树与发布产物说明](./artifacts/README.md)

## 当前阶段

已完成可独立构建的车机应用：

- `app`：唯一 Gradle 模块，包含车机界面、定位、NMEA、质量门、速度平滑、行程统计和存储。
- 桌面名称与仪表顶部名称均为 `GPS`。
- Android 8.1 / API 27 系统定位权限与 `GPS_PROVIDER` 接入。
- API 27 可用的 NMEA、GNSS 卫星状态监听，并在详情中分别显示 GPS/北斗卫星数量。
- 定位质量门、速度平滑、行程开始/暂停/继续/结束及实时统计。
- GGA、RMC、GSA、GSV、VTG、ZDA 基础解析与校验和验证。
- 使用本地 SQLite 增量保存活动行程点、暂停状态和已结束行程统计/轨迹；进程重建后可恢复活动行程。
- 车机版在恢复活动行程后明确标记“已恢复”。
- 本轮已完成离线原生界面、核心单元测试、Lint 与 Release APK 构建；未安装到真实车机。
- 合并后的 `:app:testDebugUnitTest` 共 40 项通过；数据库仪器测试 APK 已编译打包，未在设备上运行。

## 常用验证命令

```zsh
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

按照[正式签名说明](./docs/release-signing.md)设置本机签名环境后，执行：

```zsh
./gradlew :app:lintRelease :app:assembleRelease
```

输出为 `app/build/outputs/apk/release/app-release.apk`，发布附件名称为 `GPS-Car-v0.1.4.apk`。Release 使用专用 RSA 发布密钥；密钥与口令不进入仓库。
