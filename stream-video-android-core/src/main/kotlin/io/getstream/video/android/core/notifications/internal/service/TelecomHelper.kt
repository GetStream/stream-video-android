/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.notifications.internal.service

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import io.getstream.log.taggedLogger

// TODO Rahul remove hardcoded text from this helper class, remove object as well
class TelecomHelper {

    private val logger by taggedLogger("TelecomHelper")

    fun getPhoneAccountHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, TelecomVoipService::class.java),
            "MyAppPhoneAccount", // TODO Rahul remove hardcoded id (maybe we can use user-id)
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
                            PhoneAccount.CAPABILITY_SELF_MANAGED,
                    )
                    //                .setHighlightColor(ContextCompat.getColor(context, R.color.app_primary))
                    .setShortDescription("VoIP Calls")
                    .addSupportedUriScheme("voip")
                    .addSupportedUriScheme("sip")
                    .build()
            } else {
                PhoneAccount.builder(phoneAccountHandle, "MyApp Calls")
                    .setCapabilities(
                        PhoneAccount.CAPABILITY_CALL_PROVIDER or
                            PhoneAccount.CAPABILITY_VIDEO_CALLING,
                    )
                    //                .setHighlightColor(ContextCompat.getColor(context, R.color.app_primary))
                    .setShortDescription("VoIP Calls")
                    .addSupportedUriScheme("voip")
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
                "MyAppPhoneAccount",
            )

            val v = telecomManager.getPhoneAccount(phoneAccountHandle) != null
            logger.d { "[isPhoneAccountRegistered] success" }
            v
        } catch (e: Exception) {
            logger.e { "[isPhoneAccountRegistered] throws: ${e.message}" }
            e.printStackTrace() // TODO Rahul getting exception of READ_PHONE_NUMBERS
            false
        }
    }

    fun canUseJetpackTelecom(): Boolean {
        return true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
