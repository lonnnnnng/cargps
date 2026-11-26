package com.lonng.cargps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lonng.cargps.domain.NmeaFrame
import com.lonng.cargps.ui.GpsTheme
import com.lonng.cargps.ui.DashboardScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), LocationEngine.Listener {
    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(applicationContext)
    }
    private lateinit var locationEngine: LocationEngine

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationEngine.start()
        } else {
            viewModel.onPermissionRequired()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        locationEngine = LocationEngine(this, this)
        lifecycleScope.launch {
            // 作者：long｜只在仪表可见时刷新，按秒边界对齐，避免切到导航后仍在后台重算界面状态。
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val nowMillis = System.currentTimeMillis()
                    viewModel.onTick(nowMillis)
                    delay(1_000L - nowMillis % 1_000L)
                }
            }
        }
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            GpsTheme(darkTheme = state.darkTheme) {
                DashboardScreen(
                    state = state,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ),
                        )
                    },
                    onOpenLocationSettings = {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    onToggleTrip = { viewModel.toggleTrip(System.currentTimeMillis()) },
                    onEndTrip = { viewModel.endTrip(System.currentTimeMillis()) },
                    onToggleTheme = viewModel::toggleTheme,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationEngine.start()
        } else {
            viewModel.onPermissionRequired()
        }
    }

    override fun onStop() {
        locationEngine.stop()
        viewModel.onLocationMonitoringStopped(System.currentTimeMillis())
        super.onStop()
    }

    override fun onPermissionRequired() = viewModel.onPermissionRequired()
    override fun onProviderDisabled() = viewModel.onProviderDisabled()
    override fun onSearching() = viewModel.onSearching()
    override fun onLocation(location: Location) = viewModel.onLocation(location)
    override fun onSatellitesChanged(
        inView: Int,
        usedInFix: Int,
        gpsInView: Int,
        gpsUsedInFix: Int,
        beidouInView: Int,
        beidouUsedInFix: Int,
    ) = viewModel.onSatellitesChanged(
        inView = inView,
        usedInFix = usedInFix,
        gpsInView = gpsInView,
        gpsUsedInFix = gpsUsedInFix,
        beidouInView = beidouInView,
        beidouUsedInFix = beidouUsedInFix,
    )

    override fun onNmea(frame: NmeaFrame) = viewModel.onNmea(frame)
}
