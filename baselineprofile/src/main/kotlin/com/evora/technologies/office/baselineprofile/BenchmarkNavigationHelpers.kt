package com.evora.technologies.office.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.evora.technologies.office"

internal fun MacrobenchmarkScope.completeOnboardingIfShown() {
    grantAllFilesAppOp(device)
    if (isHomeVisible(device)) return

    var triedAllFiles = false
    val deadline = System.currentTimeMillis() + 20_000L

    while (System.currentTimeMillis() < deadline) {
        if (isHomeVisible(device)) return

        device.findObject(By.res("language_continue_btn"))?.let { btn ->
            btn.click()
            device.wait(Until.gone(By.res("language_continue_btn")), 3_000L)
            continue
        }
        device.findObject(By.res("onboarding_skip_btn"))?.let { btn ->
            btn.click()
            device.wait(Until.gone(By.res("onboarding_skip_btn")), 3_000L)
            continue
        }
        device.findObject(By.res("paywall_skip_btn"))?.let { btn ->
            btn.click()
            device.wait(Until.gone(By.res("paywall_skip_btn")), 3_000L)
            continue
        }
        if (!triedAllFiles) {
            device.findObject(By.res("storage_all_files_btn"))?.let { btn ->
                triedAllFiles = true
                grantAllFilesAppOp(device)
                btn.click()
                device.wait(Until.gone(By.res("storage_all_files_btn")), 3_000L)
                returnToTargetPackage(device)
                continue
            }
        }
        device.findObject(By.res("storage_saf_btn"))?.let { btn ->
            btn.click()
            device.wait(Until.gone(By.res("storage_saf_btn")), 3_000L)
            returnToTargetPackage(device)
            if (device.currentPackageName == TARGET_PACKAGE && device.hasObject(By.res("storage_saf_btn"))) {
                device.pressBack()
                device.waitForIdle()
            }
            continue
        }
        Thread.sleep(200L)
    }
    waitForHomeScreen(device)
}

private fun grantAllFilesAppOp(device: UiDevice) {
    runCatching {
        device.executeShellCommand("appops set $TARGET_PACKAGE MANAGE_EXTERNAL_STORAGE allow")
    }
}

private fun returnToTargetPackage(device: UiDevice) {
    var backTries = 0
    while (device.currentPackageName != TARGET_PACKAGE && backTries < 4) {
        device.pressBack()
        device.waitForIdle()
        backTries++
    }
}

internal fun waitForHomeScreen(device: UiDevice, timeoutMs: Long = 10_000L) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (isHomeVisible(device)) return
        Thread.sleep(100L)
    }
}

private fun isHomeVisible(device: UiDevice): Boolean =
    device.hasObject(By.res("home_documents_list")) ||
        device.hasObject(By.res("home_documents_grid")) ||
        device.hasObject(By.res("home_documents_empty"))

internal fun MacrobenchmarkScope.exerciseHomeAndToolsScroll() {
    waitForHomeScreen(device)
    val homeScroller = device.findObject(By.res("home_documents_list"))
        ?: device.findObject(By.res("home_documents_grid"))
    homeScroller?.flingIfScrollable(device)

    device.findObject(By.res("nav_tab_tools"))?.let { toolsTab ->
        toolsTab.click()
        device.wait(Until.hasObject(By.res("tools_hub_list")), 4_000L)
        device.findObject(By.res("tools_hub_list"))?.flingIfScrollable(device)
    }

    device.findObject(By.res("nav_tab_home"))?.let { homeTab ->
        homeTab.click()
        waitForHomeScreen(device, 4_000L)
    }
}

private fun UiObject2.flingIfScrollable(device: UiDevice) {
    if (!isScrollable) return
    runCatching {
        setGestureMargin(device.displayWidth / 5)
        fling(Direction.DOWN)
        device.waitForIdle()
        fling(Direction.UP)
        device.waitForIdle()
    }
}
