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

import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.internal.VideoPushDelegate.Companion.DEFAULT_CALL_TEXT
import io.getstream.video.android.core.notifications.internal.service.triggers.IncomingCallPresenter
import io.getstream.video.android.core.notifications.internal.service.triggers.ServiceLauncher
import io.getstream.video.android.core.notifications.internal.telecom.TelecomConnectionIncomingCallData
import io.getstream.video.android.core.notifications.internal.telecom.TelecomConnectionOutgoingCallData
import io.getstream.video.android.core.notifications.internal.telecom.connection.ErrorTelecomConnection
import io.getstream.video.android.core.notifications.internal.telecom.connection.SuccessIncomingTelecomConnection
import io.getstream.video.android.core.notifications.internal.telecom.connection.SuccessTelecomOutgoingConnection
import io.getstream.video.android.model.StreamCallId
import kotlinx.coroutines.CoroutineExceptionHandler

internal class TelecomVoipService : ConnectionService() {

    internal open val logger by taggedLogger("TelecomVoipService")
    lateinit var serviceLauncher: ServiceLauncher

    val handler = CoroutineExceptionHandler { _, exception ->
        logger.e(exception) { "[TelecomVoipService#Scope] Uncaught exception: $exception" }
    }

    override fun onCreate() {
        super.onCreate()
        serviceLauncher = ServiceLauncher(applicationContext)
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection {
        super.onCreateOutgoingConnection(connectionManagerPhoneAccount, request)
        val streamVideoClient = (StreamVideo.instanceOrNull() as? StreamVideoClient)
        if (streamVideoClient != null) {
            val callCid =
                request?.address?.toString() ?: return ErrorTelecomConnection(applicationContext)

            val displayName = request.extras?.getString(TelecomManager.EXTRA_CALL_SUBJECT)
                ?: request.address?.schemeSpecificPart?.takeIf { it.isNotBlank() }
                ?: DEFAULT_CALL_TEXT
            val isVideo =
                request.extras?.getBoolean(NotificationHandler.INTENT_EXTRA_IS_VIDEO) == true

            logger.d { "Creating new YourVoipConnection for call_cid: $callCid, displayName: $displayName" }
            val callType = callCid.split(":")[0]
            val streamCallId =
                StreamCallId(type = callType, id = callCid.split(":")[1])
            val callServiceConfiguration =
                streamVideoClient.callServiceConfigRegistry.get(streamCallId.type)
            // Create your custom Connection class instance
            val connection = SuccessTelecomOutgoingConnection(
                applicationContext,
                StreamVideo.instance(),
                TelecomConnectionOutgoingCallData(
                    streamCallId,
                    displayName,
                    callServiceConfiguration,
                    isVideo,
                    null,
                ),
            )

            with(connection) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setConnectionProperties(Connection.PROPERTY_SELF_MANAGED)
                }
                setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
                setCallerDisplayName(
                    displayName,
                    TelecomManager.PRESENTATION_ALLOWED,
                ) // Or your app's name as caller for outgoing
                if (isVideo) {
                    setVideoState(VideoProfile.STATE_BIDIRECTIONAL)
                } else {
                    setVideoState(VideoProfile.STATE_AUDIO_ONLY)
                }

                setRinging() // Indicate that the call is now attempting to connect (ringing out)
            }
            val call = streamVideoClient.call(streamCallId.type, streamCallId.id)
            call.state.updateTelecomConnection(connection)

            // --- START YOUR VOIP SIGNALING AND MEDIA (from within YourVoipConnection.onCallAudioStateChanged or after setRinging/setActive) ---
            logger.d { "Successfully created Telecom outgoing connection" }
            return connection
        } else {
            logger.e { "Failed to create Telecom outgoing connection" }
            val failedConn = ErrorTelecomConnection(applicationContext)
            failedConn.setDisconnected(
                DisconnectCause(
                    DisconnectCause.ERROR,
                    "StreamVideoClient is null",
                ),
            )
            return failedConn
        }
    }

    /**
     * This is invoked when
     */
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection {
        logger.d { "[onCreateIncomingConnection]" }
        try {
            super.onCreateIncomingConnection(connectionManagerPhoneAccount, request)
            val streamVideoClient = (StreamVideo.instanceOrNull() as? StreamVideoClient)
            if (streamVideoClient != null) {
                val callCid = request?.extras?.getString(NotificationHandler.INTENT_EXTRA_CALL_CID)
                val displayName =
                    request?.extras?.getString(NotificationHandler.INTENT_EXTRA_CALL_DISPLAY_NAME)
                        ?.takeIf { it.isNotBlank() } ?: DEFAULT_CALL_TEXT
                val isVideo =
                    request?.extras?.getBoolean(NotificationHandler.INTENT_EXTRA_IS_VIDEO) ?: false

                val callType = callCid?.split(":")?.get(0) ?: ""
                val streamCallId =
                    StreamCallId(type = callType, id = callCid?.split(":")?.get(1) ?: "")
                logger.d { "[onCreateIncomingConnection], streamCallId = $streamCallId" }
                val callServiceConfiguration =
                    streamVideoClient.callServiceConfigRegistry.get(streamCallId.type)
                val call = streamVideoClient.call(streamCallId.type, streamCallId.id)
                val telecomConnectionIncomingCallData = TelecomConnectionIncomingCallData(
                    streamCallId,
                    callDisplayName = displayName,
                    callServiceConfiguration = callServiceConfiguration,
                    isVideo = isVideo,
                    notification = call.state.atomicNotification.get(),
                )

                val connection = SuccessIncomingTelecomConnection(
                    applicationContext,
                    streamVideoClient,
                    IncomingCallPresenter(ServiceIntentBuilder()),
                    telecomConnectionIncomingCallData,
                )
                val address = Uri.fromParts(PhoneAccount.SCHEME_TEL, "0000", null)
                /**
                 * Current status, the wearable is displaying unknown as caller name.
                 * Unable to find a way to render the caller name at the moment
                 * The wearable expects a phone number on the address variable
                 */
//                val address = Uri.fromParts(PhoneAccount.SCHEME_SIP, displayName, null)
                logger.d { "Request address = ${request?.address}, address = $address, displayName = $displayName" }

                with(connection) {
                    setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
                    setCallerDisplayName(displayName, TelecomManager.PRESENTATION_ALLOWED)
                }

                call.state.updateTelecomConnection(connection)
                connection.setRinging()
                return connection
            } else {
                val failedConn = ErrorTelecomConnection(applicationContext)
                failedConn.setDisconnected(
                    DisconnectCause(
                        DisconnectCause.ERROR,
                        "StreamVideoClient is null",
                    ),
                )
                return failedConn
            }
        } catch (e: Exception) {
            val failedConn = ErrorTelecomConnection(applicationContext)
            failedConn.setDisconnected(DisconnectCause(DisconnectCause.ERROR, e.message))
            return failedConn
        }
    }

    // TODO Rahul for missed call
    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }
}
