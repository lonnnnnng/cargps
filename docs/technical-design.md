# GPS Android 技术设计

作者：long  
验证日期：2026-08-05  
目标运行环境：Android 8.1 / API 27

## 1. 技术边界

采用原生 Android 单模块工程，以 Kotlin 和 Jetpack Compose 实现固定横屏车机仪表。目标车机可能不包含或不稳定支持 Google Play Services，因此定位核心不依赖 Fused Location Provider，也不把网络作为定位或界面的前置条件。

工程固定 `minSdk = 27`，只针对已确认车机减少兼容分支；当前使用 `compileSdk = 34`、`targetSdk = 34`，但运行路径不能把 API 30 才加入的重载直接调用在 API 27 上。

## 2. 数据流

```mermaid
flowchart LR
    LM["LocationManager / GPS_PROVIDER"] --> LS["原始 Location 样本"]
    NM["OnNmeaMessageListener"] --> NP["NMEA 校验与解析"]
    GS["GnssStatus.Callback"] --> GH["卫星与定位健康"]
    LS --> QG["样本质量门"]
    QG --> SE["速度估算与平滑"]
    QG --> TA["行程累计器"]
    NP --> GH
    SE --> VM["仪表状态"]
    TA --> DB["本地行程存储"]
    TA --> VM
    GH --> VM
```

关键原则：只有“有效定位样本”能驱动行程累计；NMEA 和卫星回调补充诊断信息，但不能绕过质量门直接修改里程。

## 3. API 27 实现方式

- 位置更新：`LocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ...)`。
- NMEA：使用 API 24 已提供的 `addNmeaListener(OnNmeaMessageListener, Handler)`，不要使用 API 30 才加入的 `Executor` 重载。
- 卫星状态：使用 `registerGnssStatusCallback(GnssStatus.Callback, Handler)`，不要继续使用 API 24 已废弃的 `GpsStatus.Listener`。
- 星座识别：使用 API 24 提供的 `GnssStatus.getConstellationType()`，分别统计 GPS 与北斗可见/参与定位数量；目标车机是否持续上报需在硬件上验收。
- 权限：NMEA 与 GNSS 详细状态需要 `android.permission.ACCESS_FINE_LOCATION`；运行时授权失败时进入“未授权”状态。
- 数据字段：在读取速度、海拔、方向和精度前分别检查 `hasSpeed()`、`hasAltitude()`、`hasBearing()` 和 `hasAccuracy()`，缺失值保持为空。

## 4. 数据优先级

| 指标 | 主来源 | 备用/诊断来源 | 说明 |
| --- | --- | --- | --- |
| 坐标 | `Location` | GGA/RMC | 所有统计只认通过质量门的 `Location` |
| 瞬时速度 | `Location.getSpeed()` | 相邻点推导、RMC/VTG | RMC/VTG 用于诊断差异，不独立累计 |
| 海拔 | `Location.getAltitude()` | GGA | 海拔缺失时不显示 `0 m` |
| 航向 | `Location.getBearing()` | RMC/VTG | 低速时航向可能不稳定 |
| 时间 | `Location.getTime()` | RMC/ZDA | 界面转换为本地时区，原始值保留 UTC 语义 |
| 卫星 | `GnssStatus` | GSA/GSV | 分开显示可见数和参与定位数 |
| 精度因子 | NMEA GSA/GGA | 无 | 设备未输出时显示“未提供” |

## 5. 工程与包边界

- `app` 是唯一 Gradle 应用模块，namespace 与 applicationId 均为 `com.lonng.cargps`。
- 根包：Activity、ViewModel、定位引擎及应用生命周期。
- `domain`：NMEA 解析、质量门、速度估算和行程统计。
- `storage`：SQLite 行程存储及写入队列。
- `ui`：1024×600 横屏仪表与主题。
- 活动行程统计通过逐点累计器维护距离、移动时长和最高速度，时钟刷新不再遍历完整轨迹；恢复行程时只重建一次累计器。
- 车机界面消费同模块 ViewModel 提供的只读仪表状态，不自行计算业务统计。
- 本地测试与设备数据库测试分别保留于 `app/src/test` 与 `app/src/androidTest`。

业务规则应写在对应模块实现附近。例如，“定位恢复后不连接失锁前后的两个点”应贴近行程累计器的断点处理代码，而不是只保留在类头说明中。代码注释作者统一为 `long`，关键注释说明业务原因和错误结果，不复述语句动作。

## 6. 前后台策略

- 只在界面可见时记录：普通前台定位即可，权限和系统负担最小。
- 用户明确开始行程且允许切到其他应用继续记录：Android 8.0 起后台定位会被限频，应使用带常驻通知的前台服务保存连续行程。
- API 27 不需要 `ACCESS_BACKGROUND_LOCATION`，该权限从 API 29 才出现；但仍需遵守前台服务和通知要求。
- 首版可以把“仅前台记录”设为 MVP 边界，把前台服务作为第二个垂直切片，避免一开始混入服务恢复和通知生命周期。

## 7. 持久化建议

- 当前使用 API 27 原生 `SQLiteOpenHelper`，由 `TripStorage` 隔离存储实现；领域与 UI 不依赖 SQLite 类型。
- SQLite 写入统一进入单线程后台队列；读取作为队列屏障等待此前写入完成，避免 GPS 主线程回调直接执行磁盘操作并保证业务顺序。
- 活动行程元数据在开始、暂停和恢复时立即更新，每个有效行程点增量写入，避免重复序列化完整轨迹。
- 路段边界点随轨迹一并持久化；暂停、进程恢复、失锁及 Activity 离开前台都会断开相邻路段，防止重启后补算间隔距离或移动时间。
- 结束行程在同一事务中先写入已结束统计并迁移活动轨迹点，再清除活动行程；中途失败时优先保留完整可恢复数据。
- 进程重建时恢复行程模式、开始时间、暂停累计和轨迹点，并主动断开恢复前后的定位点，避免补算跨进程位移。
- 原始 NMEA 默认不持久化；后续导出能力与轨迹历史继续使用独立存储边界。

## 8. 测试重点

- NMEA：合法校验和、错误校验和、缺字段、多星座 talker ID、未知语句、非数值字段。
- 质量门：时间倒退、重复时间、低精度、超时恢复、跳点、静止漂移。
- 统计：移动/停车边界、暂停期间冻结、恢复、结束、长轨迹增量累计、进程重建和零时长除法。
- UI：无权限、无数据、缺海拔、弱定位、过期、超长坐标文本，以及车机 1024×600 安全区。
- 设备：显式指定 `CASKA_1024x600`；任何安装命令都不能依赖 adb 默认设备。

## 9. 已验证的官方依据

- [`OnNmeaMessageListener`](https://developer.android.com/reference/android/location/OnNmeaMessageListener)：API 24 加入，用于接收 GNSS NMEA 语句。
- [`LocationManager`](https://developer.android.com/reference/android/location/LocationManager)：位置 API 权限、NMEA 监听和 GNSS 状态回调定义。
- [`Location`](https://developer.android.com/reference/android/location/Location)：位置对象包含坐标、时间、精度，以及可选的方向、海拔和速度。
- [后台定位](https://developer.android.com/develop/sensors-and-location/location/background)：Android 8.0 及以上后台应用的位置更新会被限制为每小时少量次数。

以上为 2026-08-05 抓取 Android Developers 页面后确认的结论。不同车机 GNSS 驱动是否实际输出全部 NMEA 语句，仍需安装到 CASKA 或真机后验证。
