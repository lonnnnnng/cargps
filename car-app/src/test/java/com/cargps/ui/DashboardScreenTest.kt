package com.cargps.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cargps.DashboardState
import com.cargps.DashboardViewModel
import com.cargps.FixStatus
import com.cargps.TripMode
import com.cargps.domain.NmeaSentenceType
import com.cargps.domain.TripStats
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 28], qualifiers = "w1024dp-h600dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class DashboardScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun nightLayoutFitsAndDetailsUseFamiliarLabels() {
        render()
        assertLayoutFits()
        compose.onNodeWithText(DashboardViewModel.formatTime(sample.nowMillis)).assertIsDisplayed()
        compose.onNodeWithText(DashboardViewModel.formatDateChinese(sample.nowMillis)).assertIsDisplayed()
        capture("night")
        compose.onNodeWithText("定位详情").performClick()
        compose.onNodeWithText("GPS    7 / 12").assertExists()
        compose.onNodeWithText("北斗    5 / 8").assertExists()
        compose.onNodeWithText("HDOP 0.8    PDOP 1.3    VDOP 1.0").assertExists()
        capture("details")
        compose.onNodeWithText("返回仪表").performClick()
        compose.onNodeWithTag("speed").assertIsDisplayed()
    }

    @Test fun enlargedTextFitsAndEndRequiresConfirmation() {
        var ended = 0
        render(fontScale = 1.3f, state = sample.copy(speedKmh = 128.0), onEnd = { ended++ })
        assertLayoutFits()
        capture("large-font")
        compose.onNodeWithText("结束").performClick()
        assertEquals(0, ended)
        compose.onNodeWithText("确认结束").performClick()
        assertEquals(1, ended)
    }

    @Test fun dayLayoutAndEmptyStateFit() {
        render(state = sample.copy(darkTheme = false, fixStatus = FixStatus.SEARCHING, speedKmh = null))
        assertLayoutFits()
        capture("day-searching")
    }

    @Test fun permissionAndLocationActionsReachTheirCallbacks() {
        var requested = 0
        render(state = sample.copy(fixStatus = FixStatus.PERMISSION_REQUIRED, tripMode = TripMode.IDLE),
            onPermission = { requested++ })
        assertLayoutFits()
        compose.onNodeWithText("授权定位").performClick()
        assertEquals(1, requested)
        compose.onNodeWithText("结束").assertIsNotEnabled()
    }

    @Test fun disabledLocationOpensSettings() {
        var settingsOpened = false
        render(state = sample.copy(fixStatus = FixStatus.LOCATION_DISABLED),
            onSettings = { settingsOpened = true })
        compose.onNodeWithText("开启定位").performClick()
        assertTrue(settingsOpened)
    }

    private fun render(
        fontScale: Float = 1f,
        state: DashboardState = sample,
        onEnd: () -> Unit = {},
        onPermission: () -> Unit = {},
        onSettings: () -> Unit = {},
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                CarGpsTheme(state.darkTheme) {
                    // 作者：long｜使用实际可用内容高度，防止 600 像素全屏预览掩盖车机系统栏造成的压缩。
                    Box(Modifier.requiredSize(1024.dp, 528.dp)) {
                        DashboardScreen(state, onPermission, onSettings, {}, onEnd, {})
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertLayoutFits() {
        val header = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
        val speed = compose.onNodeWithTag("speed").fetchSemanticsNode().boundsInRoot
        val date = compose.onNodeWithTag("date").fetchSemanticsNode().boundsInRoot
        val time = compose.onNodeWithTag("time").fetchSemanticsNode().boundsInRoot
        val controls = compose.onNodeWithTag("controls").fetchSemanticsNode().boundsInRoot
        assertTrue(header.bottom <= speed.top)
        assertTrue(date.bottom <= time.top)
        assertTrue(speed.right <= time.left)
        assertTrue(time.bottom <= controls.top)
        assertTrue(controls.height >= 56f)
        // 作者：long｜检查布局边界与数字单行排版；字体字形溢出另以原生渲染图核验。
        var fittedTextCount = 0
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.filter { it.layoutInput.style.fontFeatureSettings == "tnum" }.forEach { result ->
                fittedTextCount++
                assertEquals(1, result.lineCount)
                assertTrue(result.size.width <= result.layoutInput.constraints.maxWidth)
                assertTrue(result.size.height <= result.layoutInput.constraints.maxHeight)
            }
        }
        assertTrue("应检测到速度、时间和行程数字", fittedTextCount >= 3)
    }

    private fun capture(name: String) {
        compose.runOnIdle {
            val view = if (name == "details") {
                requireNotNull(ShadowDialog.getLatestDialog().window).decorView
            } else compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val target = File("build/reports/dashboard/api-${Build.VERSION.SDK_INT}/$name.png")
            target.parentFile?.mkdirs()
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private val sample = DashboardState(
        nowMillis = Instant.parse("2026-11-26T13:55:01Z").toEpochMilli(),
        fixStatus = FixStatus.FIXED,
        speedKmh = 86.0,
        latitude = 31.394179,
        longitude = 121.476947,
        altitudeMeters = 17.0,
        bearingDegrees = 346f,
        accuracyMeters = 4f,
        lastFixAtMillis = Instant.parse("2026-11-26T13:55:00Z").toEpochMilli(),
        satellitesInView = 20, satellitesUsed = 12,
        gpsSatellitesInView = 12, gpsSatellitesUsed = 7,
        beidouSatellitesInView = 8, beidouSatellitesUsed = 5,
        hdop = 0.8, pdop = 1.3, vdop = 1.0,
        lastNmeaType = NmeaSentenceType.VTG,
        tripMode = TripMode.RECORDING,
        tripStats = TripStats(23_530.0, 2_472_000L, 1_775_000L, 697_000L, 9.52, 13.25, 23.89),
    )
}
