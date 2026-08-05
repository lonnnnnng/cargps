# CarGPS 双端 GPS 码表

CarGPS 是一套同时覆盖横屏车机与竖屏手机的离线 GPS 仪表。两端共用定位质量、NMEA、速度平滑和行程统计核心，不依赖地图服务或 Google Play Services。

## 工程结构

```text
cargps/
├── gps-core/      # 两端共用的定位、NMEA、速度和行程领域逻辑
├── car-app/       # CASKA 横屏车机版，包名 com.cargps
├── mobile-app/    # 竖屏手机版，包名 com.cargps.mobile
├── docs/
├── gradle/
├── gradlew
└── settings.gradle.kts
```

两个应用共用 Gradle Wrapper，但分别生成 APK，包名不同，可同时安装。

## 车机版目标设备

- AVD：`CASKA_1024x600`
- Android：8.1.0（API 27）
- 屏幕：1024×600、160 dpi、横屏
- 当前模拟器序列号：`emulator-5554`

## 手机版验证设备

- AVD：`Pixel_9`
- 方向：竖屏
- 最低运行版本：Android 8.1（API 27）
- 应用包名：`com.cargps.mobile`

## 双端首版能力

- 显示瞬时速度、本次行程平均速度、移动平均速度和最高速度。
- 显示时间、经纬度、海拔、方向、定位精度、卫星数量和定位更新时间。
- 支持开始、暂停、继续、结束行程，并统计里程、总时长、移动时长和停车时长。
- 解析 GNSS NMEA 报文，补充定位质量与诊断信息。
- 本地保存行程，断网可用，不把定位数据上传到服务器。
- 车机版提供 1024×600 横屏、大字号、高对比度、少操作的驾驶仪表。
- 手机版提供单列可滚动仪表、固定底部行程控制和渐进展开的定位详情。

## 文档

- [领域词汇](./CONTEXT.md)
- [产品规格](./docs/product-spec.md)
- [手机版产品规格](./docs/mobile-product-spec.md)
- [Android 技术设计](./docs/technical-design.md)
- [ADR-0001：采用系统 LocationManager](./docs/adr/0001-use-platform-locationmanager.md)

## 当前阶段

已完成两个可独立构建的应用模块：

- `car-app`：原生 Kotlin + Jetpack Compose 横屏主仪表。
- `mobile-app`：独立包名的竖屏单列主仪表。
- `gps-core`：两端复用的定位、NMEA、质量门、速度平滑和行程统计。
- Android 8.1 / API 27 系统定位权限与 `GPS_PROVIDER` 接入。
- API 27 可用的 NMEA、GNSS 卫星状态监听。
- 定位质量门、速度平滑、行程开始/暂停/继续/结束及实时统计。
- GGA、RMC、GSA、GSV、VTG、ZDA 基础解析与校验和验证。
- 使用本地 SQLite 增量保存活动行程点、暂停状态和已结束行程统计/轨迹；进程重建后可恢复活动行程。
- 手机版展示最近 3 条本地行程摘要，车机版在恢复后明确标记“已恢复”。
- 已构建并安装到 `CASKA_1024x600` 模拟器完成定位和模拟路线验证。
- 手机版 Debug APK 已安装到 `Pixel_9`：完成权限状态、定位恢复、模拟坐标和行程开始后的速度/里程/统计联动验证。

## 常用验证命令

```zsh
./gradlew :gps-core:testDebugUnitTest
./gradlew :car-app:lintDebug :car-app:assembleDebug
./gradlew :mobile-app:lintDebug :mobile-app:assembleDebug
```

下一步是完整历史列表、单条删除、设置页和 GPX/CSV 导出。
