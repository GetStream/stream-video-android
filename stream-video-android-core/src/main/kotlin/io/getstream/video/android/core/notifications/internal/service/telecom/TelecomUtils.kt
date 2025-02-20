package io.getstream.video.android.core.notifications.internal.service.telecom

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import io.getstream.video.android.core.R

fun isTelecomIntegrationAvailable(context: Context): Boolean {
    // 2. Check if we have a registered and enabled PhoneAccount.
    //    You can retrieve the PhoneAccount using TelecomManager
    val telecomManager =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false

    val phoneAccountHandle = getMyPhoneAccountHandle(context)
    val phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle)
    if (phoneAccount == null || !phoneAccount.isEnabled) {
        return false
    }

    return true
}


fun getMyPhoneAccountHandle(context: Context): PhoneAccountHandle {
    val componentName = ComponentName(
        context,
        TelecomCallService::class.java
    )
    val accountId = context.packageName
    return PhoneAccountHandle(componentName, accountId)
}

fun promptEnablePhoneAccount(context: Context) {
    try {
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to generic Settings if Telecom settings unavailable
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}


fun registerMyPhoneAccount(context: Context) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val phoneAccountHandle = getMyPhoneAccountHandle(context)

    // Build the PhoneAccount
    var capabilities = PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_VIDEO_CALLING

    if (Build.VERSION.SDK_INT > 26) {
        capabilities = PhoneAccount.CAPABILITY_SELF_MANAGED
    }

    val phoneAccount = PhoneAccount.builder(
        phoneAccountHandle,
        "Stream calls" // Visible to the user in the system settings
    )
        // Capabilities define what kind of calls this account supports
        .setCapabilities(capabilities)
        // Optionally set an icon (for display in system dialer)
        .setIcon(Icon.createWithResource(context, R.drawable.stream_video_ic_call))
        .setShortDescription("My VoIP calls")
        .build()

    telecomManager.registerPhoneAccount(phoneAccount)

    // Register the PhoneAccount with Telecom
    telecomManager.registerPhoneAccount(phoneAccount)
}


