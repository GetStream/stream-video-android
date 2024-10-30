package io.getstream.video.android

import android.app.UiAutomation
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.view.KeyCharacterMap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Condition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.FileInputStream

class AutomatorTestRule : TestWatcher() {
    private var internalDevice: UiDevice? = null
    val device: UiDevice get() = internalDevice!!

    override fun starting(description: Description?) {
        internalDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    override fun finished(description: Description?) {
        internalDevice = null
    }
}