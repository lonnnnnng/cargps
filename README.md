# CarGPS 车机码表

CarGPS 是面向 CASKA 横屏车机的离线 GPS 仪表。它以易读、稳定和可解释的行程统计为第一目标，不依赖地图服务或 Google Play Services。

## 已确认的目标设备

- AVD：`CASKA_1024x600`
- Android：8.1.0（API 27）
- 屏幕：1024×600、160 dpi、横屏
- 当前模拟器序列号：`emulator-5554`

## 首版目标

- 显示瞬时速度、本次行程平均速度、移动平均速度和最高速度。
- 显示时间、经纬度、海拔、方向、定位精度、卫星数量和定位更新时间。
- 支持开始、暂停、继续、结束行程，并统计里程、总时长、移动时长和停车时长。
- 解析 GNSS NMEA 报文，补充定位质量与诊断信息。
- 本地保存行程，断网可用，不把定位数据上传到服务器。
- 针对车机驾驶场景提供大字号、高对比度、少操作的日间/夜间界面。

## 文档

- [领域词汇](./CONTEXT.md)
- [产品规格](./docs/product-spec.md)
- [Android 技术设计](./docs/technical-design.md)
- [ADR-0001：采用系统 LocationManager](./docs/adr/0001-use-platform-locationmanager.md)

## 当前阶段

已完成第一个可运行 MVP 垂直切片：

- 原生 Kotlin + Jetpack Compose 横屏主仪表。
- Android 8.1 / API 27 系统定位权限与 `GPS_PROVIDER` 接入。
- API 27 可用的 NMEA、GNSS 卫星状态监听。
- 定位质量门、速度平滑、行程开始/暂停/继续/结束及实时统计。
- GGA、RMC、GSA、GSV、VTG、ZDA 基础解析与校验和验证。
- 已构建并安装到 `CASKA_1024x600` 模拟器完成定位和模拟路线验证。

下一步是活动行程持久化、历史记录和 GPX/CSV 导出。
