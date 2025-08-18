package io.getstream.video.android.core.notifications.internal.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import io.getstream.log.taggedLogger

//TODO Rahul remove hardcoded text from this helper class, remove object as well
object TelecomHelper {

    private val logger by taggedLogger("TelecomHelper")

    fun getPhoneAccountHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, TelecomVoipService::class.java),
            "MyAppPhoneAccount" //TODO Rahul remove hardcoded id (maybe we can use user-id)
        )
    }

    fun registerPhoneAccount(context: Context): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccountHandle = getPhoneAccountHandle(context)
        return try {

            val phoneAccount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PhoneAccount.builder(phoneAccountHandle, "MyApp Calls")
                    .setCapabilities(
//                        PhoneAccount.CAPABILITY_CALL_PROVIDER or //Cannot work with CAPABILITY_SELF_MANAGED
                                PhoneAccount.CAPABILITY_VIDEO_CALLING or
                                PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING or
                                PhoneAccount.CAPABILITY_SELF_MANAGED
                    )
    //                .setHighlightColor(ContextCompat.getColor(context, R.color.app_primary))
                    .setShortDescription("VoIP Calls")
                    .addSupportedUriScheme("tel")
                    .addSupportedUriScheme("sip")
                    .build()
            } else {
                PhoneAccount.builder(phoneAccountHandle, "MyApp Calls")
                    .setCapabilities(
                        PhoneAccount.CAPABILITY_CALL_PROVIDER or
                                PhoneAccount.CAPABILITY_VIDEO_CALLING
                    )
                    //                .setHighlightColor(ContextCompat.getColor(context, R.color.app_primary))
                    .setShortDescription("VoIP Calls")
                    .addSupportedUriScheme("tel")
                    .addSupportedUriScheme("sip")
                    .build()
            }

            telecomManager.registerPhoneAccount(phoneAccount)
            logger.d { "Phone account registered successfully" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Failed to register phone account" }
            false
        }
    }

    fun isPhoneAccountRegistered(context: Context): Boolean {
        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val phoneAccountHandle = PhoneAccountHandle(
                ComponentName(context, TelecomVoipService::class.java),
                "MyAppPhoneAccount"
            )
            telecomManager.getPhoneAccount(phoneAccountHandle) != null
        } catch (e: Exception) {
            e.printStackTrace() //TODO Rahul getting exception of READ_PHONE_NUMBERS
            false
        }
    }

    /**
     * Show incoming call using Telecom API - this is the key method
     * This will display the native Android incoming call UI
     */
//    fun showIncomingCall(
//        context: Context,
//        callId: String,
//        callerNumber: String,
//        callerName: String,
//        isVideoCall: Boolean = false
//    ) {
//        try {
//            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
//            val phoneAccountHandle = PhoneAccountHandle(
//                ComponentName(context, TelecomVoipService::class.java),
//                "MyAppPhoneAccount"
//            )
//            val extras = Bundle().apply {
//                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
//                putParcelable(
//                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
//                    Uri.fromParts("tel", callerNumber, null)
//                )
//                // Pass call ID to ConnectionService
//                putString(EXTRA_CALL_ID, callId)
//                putString(EXTRA_CALLER_NAME, callerName)
//
//                if (isVideoCall) {
//                    putInt(
//                        TelecomManager.EXTRA_INCOMING_VIDEO_STATE,
//                        VideoProfile.STATE_BIDIRECTIONAL
//                    )
//                }
//            }
//
//            logger.d{"Adding new incoming call via Telecom API"}
//            telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
//
//        } catch (e: SecurityException) {
//            logger.e(e) { "Permission denied for adding incoming call" }
//            // Fallback to custom notification
//            fallbackToCustomNotification(callId, callerNumber, callerName, isVideoCall)
//        } catch (e: Exception) {
//            logger.e(e) { "Failed to add incoming call"}
//            fallbackToCustomNotification(callId, callerNumber, callerName, isVideoCall)
//        }
//    }
}