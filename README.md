# CarGPS 车机版

CarGPS 车机版是面向 1024×600 横屏车机的离线 GPS 仪表，不依赖地图服务或 Google Play Services。本仓库只维护车机应用及其直接依赖的定位核心。

当前版本：[v0.1.2](https://github.com/lonnnnnng/cargps/releases/tag/v0.1.2) · [更新说明](./docs/release-notes-v0.1.2.md)

## 工程结构

```text
cargps/
├── gps-core/      # 车机版独立的定位、NMEA、速度和行程领域逻辑
├── car-app/       # CASKA 横屏车机版，包名 com.cargps
├── docs/
├── gradle/
├── gradlew
└── settings.gradle.kts
```

`car-app` 只依赖本项目内的 `gps-core`，两个模块共同构成车机应用。

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
- [本轮改造与验证](./docs/ui-redesign-20261126.md)
- [界面规则](./DESIGN.md)
- [截图、UI 树与发布产物说明](./artifacts/README.md)

## 当前阶段

已完成可独立构建的车机应用：

- `car-app`：原生 Kotlin + Jetpack Compose 横屏主仪表。
- `gps-core`：车机项目独立维护的定位、NMEA、质量门、速度平滑和行程统计。
- Android 8.1 / API 27 系统定位权限与 `GPS_PROVIDER` 接入。
- API 27 可用的 NMEA、GNSS 卫星状态监听，并在详情中分别显示 GPS/北斗卫星数量。
- 定位质量门、速度平滑、行程开始/暂停/继续/结束及实时统计。
- GGA、RMC、GSA、GSV、VTG、ZDA 基础解析与校验和验证。
- 使用本地 SQLite 增量保存活动行程点、暂停状态和已结束行程统计/轨迹；进程重建后可恢复活动行程。
- 车机版在恢复活动行程后明确标记“已恢复”。
- 本轮已完成离线原生界面、核心单元测试、Lint 与 Release APK 构建；未安装到真实车机。

## 常用验证命令

```zsh
./gradlew :gps-core:testDebugUnitTest
./gradlew :car-app:testDebugUnitTest
./gradlew :car-app:lintDebug :car-app:assembleDebug
```
