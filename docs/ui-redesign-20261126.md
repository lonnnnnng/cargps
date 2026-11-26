# 车机仪表界面改造与验证

日期：2026-11-26

## 页面改动

- 主屏按“瞬时速度、时分秒、年月日”排序；日期与时间各占独立区域，使用琥珀色与青绿色区分。
- 行程区保留里程、行程时长、移动/停车时长、两类平均速度和最高速度；方向、海拔、坐标和最近定位时间留在主屏底部。
- GPS、北斗、HDOP、PDOP、VDOP、NMEA 与 km/h、km、m 保留常用写法，卫星与 DOP 数值集中在“定位详情”。
- 顶部改为内容驱动高度，主读数按可用尺寸测量和缩放；系统栏仍由 Activity 窗口预留，不隐藏系统栏。
- 夜间/日间主题使用同一套 Material 语义角色；定位关闭时可以直接打开系统定位设置，仪表在前台期间保持屏幕常亮。
- 仓库内没有手机版源码、模块或依赖；README 已说明此仓库只维护车机应用及其定位核心。

## 定位和行程改进

- `GnssStatus.getConstellationType()` 分别统计 GPS 与北斗可见数、参与定位数。Android API 参考标注这些常量和方法从 API 24 提供，项目最低 API 27 可用：<https://developer.android.com/reference/android/location/GnssStatus>。
- HDOP、PDOP、VDOP 和 NMEA 卫星数分别跟踪更新时间；字段过期后清空。近期 GNSS 回调优先于 NMEA 总数，避免多星座报文互相覆盖。
- 活动行程改为逐定位点累计，时钟刷新不再扫描全部轨迹；恢复、暂停、失锁与前后台切换都会关闭路段边界，避免跨间隔计算里程或移动时间。
- 暂停、定位过期或丢失后结束行程时，最终统计采用冻结时刻；存储队列中的边界点让进程重建后保留相同行程口径。

## 本机验证

执行命令：

```zsh
./gradlew :car-app:testDebugUnitTest :gps-core:testDebugUnitTest :car-app:lintDebug :car-app:assembleDebug
```

- **已验证**：车机界面 10 个离线 Compose 用例覆盖 API 27/28、1024×528 应用内容区、1.0/1.3 字号、日间/夜间、搜星、定位详情、权限入口和行程结束确认；另有 3 个 API 27 定位/前后台回归用例。
- **已验证**：`gps-core` 27 个单元测试通过，覆盖长轨迹增量结果、3 秒断点、暂停/恢复、NMEA 字段独立过期及 GPS/北斗计数缓存一致性。
- **已验证**：`lintDebug` 0 errors；APK 构建成功。Lint 仍报告 targetSdk 和依赖版本较旧提醒，本次没有为兼容 Android 8.1 批量升级它们。
- **未验证**：本机没有 CASKA 模拟器程序或目标车机连接，未在实车安装；实际系统栏高度、GNSS 驱动上报及路测表现需要目标设备验收。

## 本轮产物

- `../artifacts/ui-redesign-20261126/CarGPS-Car-ui-20261126-debug.apk`：Debug APK，`com.cargps`，版本 `0.1.1`，最低 API 27。
- `../artifacts/ui-redesign-20261126/SHA256SUMS`：APK SHA-256 校验值。
- `../artifacts/ui-redesign-20261126/night.png`、`day-searching.png`、`large-font.png`：裁取后的 1024×528 内容预览，使用测试样例数据，不是道路实测记录。
- `../artifacts/ui-redesign-20261126/api-27/`、`api-28/`：原始原生渲染图；`details.png` 是定位详情弹窗。
- 单元测试 HTML/XML 与 Lint 报告一并存放在本轮归档目录。
