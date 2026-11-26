package com.cargps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.cargps.domain.NmeaFrame
import com.cargps.domain.NmeaParser

class LocationEngine(
    context: Context,
    private val listener: Listener,
) {
    interface Listener {
        fun onPermissionRequired()
        fun onProviderDisabled()
        fun onSearching()
        fun onLocation(location: Location)
        fun onSatellitesChanged(
            inView: Int,
            usedInFix: Int,
            gpsInView: Int,
            gpsUsedInFix: Int,
            beidouInView: Int,
            beidouUsedInFix: Int,
        )
        fun onNmea(frame: NmeaFrame)
    }

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = listener.onLocation(location)

        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) listener.onProviderDisabled()
        }

        override fun onProviderEnabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) listener.onSearching()
        }

        @Deprecated("Android 8.1 仍可能回调该方法，保留空实现以兼容 API 27")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    private val nmeaListener = OnNmeaMessageListener { message, _ ->
        NmeaParser.parse(message)?.let(listener::onNmea)
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var usedInFix = 0
            var gpsInView = 0
            var gpsUsedInFix = 0
            var beidouInView = 0
            var beidouUsedInFix = 0
            repeat(status.satelliteCount) { index ->
                val used = status.usedInFix(index)
                if (used) usedInFix += 1
                when (status.getConstellationType(index)) {
                    GnssStatus.CONSTELLATION_GPS -> {
                        gpsInView += 1
                        if (used) gpsUsedInFix += 1
                    }

                    GnssStatus.CONSTELLATION_BEIDOU -> {
                        beidouInView += 1
                        if (used) beidouUsedInFix += 1
                    }
                }
            }
            // 作者：long｜分别汇报星座数量，避免混合 GNSS 的总数掩盖 GPS 与北斗各自的参与情况。
            listener.onSatellitesChanged(
                inView = status.satelliteCount,
                usedInFix = usedInFix,
                gpsInView = gpsInView,
                gpsUsedInFix = gpsUsedInFix,
                beidouInView = beidouInView,
                beidouUsedInFix = beidouUsedInFix,
            )
        }
    }

    fun start() {
        if (started) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            listener.onPermissionRequired()
            return
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            listener.onProviderDisabled()
            return
        }

        try {
            // 作者：long｜API 27 使用 Handler 重载，避免误用 API 30 才提供的 Executor 版本。
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,
                0f,
                locationListener,
                Looper.getMainLooper(),
            )
            locationManager.addNmeaListener(nmeaListener, handler)
            locationManager.registerGnssStatusCallback(gnssCallback, handler)
            started = true
            listener.onSearching()
        } catch (_: SecurityException) {
            listener.onPermissionRequired()
        }
    }

    fun stop() {
        if (!started) return
        locationManager.removeUpdates(locationListener)
        locationManager.removeNmeaListener(nmeaListener)
        locationManager.unregisterGnssStatusCallback(gnssCallback)
        started = false
    }
}
