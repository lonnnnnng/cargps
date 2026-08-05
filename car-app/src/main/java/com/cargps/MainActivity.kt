package com.cargps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cargps.domain.NmeaFrame
import com.cargps.ui.CarGpsTheme
import com.cargps.ui.DashboardScreen

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
        locationEngine = LocationEngine(this, this)
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            CarGpsTheme(darkTheme = state.darkTheme) {
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
                    onToggleTrip = { viewModel.toggleTrip(System.currentTimeMillis()) },
                    onEndTrip = { viewModel.endTrip(System.currentTimeMillis()) },
                    onToggleTheme = viewModel::toggleTheme,
                    onTick = viewModel::onTick,
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
        super.onStop()
    }

    override fun onPermissionRequired() = viewModel.onPermissionRequired()
    override fun onProviderDisabled() = viewModel.onProviderDisabled()
    override fun onSearching() = viewModel.onSearching()
    override fun onLocation(location: Location) = viewModel.onLocation(location)
    override fun onSatellitesChanged(inView: Int, usedInFix: Int) =
        viewModel.onSatellitesChanged(inView, usedInFix)

    override fun onNmea(frame: NmeaFrame) = viewModel.onNmea(frame)
}
