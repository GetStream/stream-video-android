package io.getstream.video.android.tests.sample

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.getstream.video.android.MainActivity
import io.getstream.video.android.pages.SamplePage
import org.junit.Rule
import org.junit.Test

class SampleTest : TestCase() {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @Test
    fun test() = run {
        step("First step") {
            SamplePage {

            }
        }
    }

}