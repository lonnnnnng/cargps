# CarGPS 车机版

CarGPS 车机版是面向 1024x600 横屏车机的离线 GPS 仪表，不依赖地图服务或 Google Play Services。手机版已拆分为独立项目，本项目只维护车机应用及其独立定位核心。

手机版独立仓库：[lonnnnnng/cargps-mobile](https://github.com/lonnnnnng/cargps-mobile)

## 工程结构

```text
car/
├── gps-core/      # 车机版独立的定位、NMEA、速度和行程领域逻辑
├── car-app/       # CASKA 横屏车机版，包名 com.cargps
├── docs/
├── gradle/
├── gradlew
└── settings.gradle.kts
```

`car-app` 只依赖本项目内的 `gps-core`，不依赖手机版项目或外部共享仓库。

## 车机版目标设备

- AVD：`CASKA_1024x600`
- Android：8.1.0（API 27）
- 屏幕：1024×600、160 dpi、横屏
- 当前模拟器序列号：`emulator-5554`

## 主要能力

- 显示瞬时速度、本次行程平均速度、移动平均速度和最高速度。
- 显示时间、经纬度、海拔、方向、定位精度、卫星数量和定位更新时间。
- 支持开始、暂停、继续、结束行程，并统计里程、总时长、移动时长和停车时长。
- 解析 GNSS NMEA 报文，补充定位质量与诊断信息。
- 本地保存行程，断网可用，不把定位数据上传到服务器。
- 提供 1024x600 横屏、大字号、高对比度、少操作的驾驶仪表。

## 文档与验证资料

- [领域词汇](./CONTEXT.md)
- [资料索引](./docs/README.md)
- [产品规格](./docs/product-spec.md)
- [Android 技术设计](./docs/technical-design.md)
- [ADR-0001：采用系统 LocationManager](./docs/adr/0001-use-platform-locationmanager.md)
- [当前 CASKA 验证截图](./artifacts/cargps-split-car.png)
- [截图、UI 树与发布产物说明](./artifacts/README.md)

## 当前阶段

已完成可独立构建的车机应用：

- `car-app`：原生 Kotlin + Jetpack Compose 横屏主仪表。
- `gps-core`：车机项目独立维护的定位、NMEA、质量门、速度平滑和行程统计。
- Android 8.1 / API 27 系统定位权限与 `GPS_PROVIDER` 接入。
- API 27 可用的 NMEA、GNSS 卫星状态监听。
- 定位质量门、速度平滑、行程开始/暂停/继续/结束及实时统计。
- GGA、RMC、GSA、GSV、VTG、ZDA 基础解析与校验和验证。
- 使用本地 SQLite 增量保存活动行程点、暂停状态和已结束行程统计/轨迹；进程重建后可恢复活动行程。
- 车机版在恢复活动行程后明确标记“已恢复”。
- 已构建并安装到 `CASKA_1024x600` 模拟器完成定位和模拟路线验证。

## 常用验证命令

```zsh
./gradlew :gps-core:testDebugUnitTest
./gradlew :car-app:lintDebug :car-app:assembleDebug
```
