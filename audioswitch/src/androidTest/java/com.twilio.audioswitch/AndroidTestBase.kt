package com.twilio.audioswitch

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule

open class AndroidTestBase {
    @get:Rule
    val bluetoothPermissionRules: GrantPermissionRule by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            GrantPermissionRule.grant(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            GrantPermissionRule.grant(Manifest.permission.BLUETOOTH)
        }
    }
}
