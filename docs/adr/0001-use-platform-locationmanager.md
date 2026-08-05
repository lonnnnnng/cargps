# 使用系统 LocationManager 作为唯一定位主来源

CarGPS 采用 Android `LocationManager` 的 `GPS_PROVIDER` 作为坐标、速度和行程统计的唯一主来源，并用 NMEA 与 `GnssStatus` 补充诊断信息。相比依赖 Google Play Services 的融合定位，这一选择更适合可能缺少 GMS、需要断网运行的 CASKA 车机；代价是应用必须自行处理样本质量、速度平滑、定位过期和后台记录生命周期，且不得让 NMEA 形成第二套独立统计时序。
