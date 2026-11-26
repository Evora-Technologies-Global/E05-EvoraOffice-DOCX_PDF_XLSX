package com.evora.technologies.office.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() {
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            maxIterations = 6,
            stableIterations = 2,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            completeOnboardingIfShown()
            waitForHomeScreen(device)
        }
    }

    @Test
    fun generateNavigationAndScrollProfile() {
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            maxIterations = 5,
            stableIterations = 2,
            includeInStartupProfile = false,
        ) {
            pressHome()
            startActivityAndWait()
            completeOnboardingIfShown()
            exerciseHomeAndToolsScroll()
        }
    }
}
