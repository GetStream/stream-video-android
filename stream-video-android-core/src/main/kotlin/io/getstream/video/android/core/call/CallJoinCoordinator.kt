/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call

import android.annotation.SuppressLint
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.utils.AtomicUnitCall
import io.getstream.video.android.core.utils.StreamSingleFlightProcessorImpl
import kotlinx.coroutines.delay
import kotlin.collections.plusAssign

private const val PERMISSION_ERROR = "\n[Call.join()] called without having the required permissions.\n" +
    "This will work only if you have [runForegroundServiceForCalls = false] in the StreamVideoBuilder.\n" +
    "The reason is that [Call.join()] will by default start an ongoing call foreground service,\n" +
    "To start this service and send the appropriate audio/video tracks the permissions are required,\n" +
    "otherwise the service will fail to start, resulting in a crash.\n" +
    "You can re-define your permissions and their expected state by overriding the [permissionCheck] in [StreamVideoBuilder]\n"

internal class CallJoinCoordinator(
    private val call: Call,
    private val client: StreamVideoClient,
    private val callReInitializer: CallReInitializer,
    private val onJoinFail: () -> Unit,
    private val createJoinSession: suspend (
        create: Boolean,
        createOptions: CreateCallOptions?,
        ring: Boolean,
        notify: Boolean,
    ) -> Result<RtcSession>,
    private val onRejoin: suspend (reason: String) -> Unit,
) : CallJoinContract {
    private val streamSingleFlightProcessorImpl = StreamSingleFlightProcessorImpl(call.scope)

    private val logger by taggedLogger("CallJoinCoordinator")

    override suspend fun join(
        create: Boolean,
        createOptions: CreateCallOptions?,
        ring: Boolean,
        notify: Boolean,
    ): Result<RtcSession> {
        logger.d {
            "[join] #ringing; #track; create: $create, ring: $ring, notify: $notify, createOptions: $createOptions"
        }

        callReInitializer.waitFromCleanup()
        callReInitializer.reinitialiseCoroutinesIfNeeded()

        // CRITICAL: Reset isDestroyed for new session
        call.isDestroyed.set(false)
        logger.d { "[join] isDestroyed reset to false for new session" }

        val permissionPass =
            client.permissionCheck.checkAndroidPermissionsGroup(client.context, call)
        // Check android permissions and log a warning to make sure developers requested adequate permissions prior to using the call.
        if (!permissionPass.first) {
            logger.w { PERMISSION_ERROR }
        }
        // if we are a guest user, make sure we wait for the token before running the join flow
        client.guestUserJob?.await()

        // Ensure factory is created with the current audioBitrateProfile before joining
        call.ensureFactoryMatchesAudioProfile()

        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        call.state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        call.atomicLeave = AtomicUnitCall()
        while (retryCount < 3) {
            result = createJoinSession(create, createOptions, ring, notify)
            if (result is Success) {
                // we initialise the camera, mic and other according to local + backend settings
                // only when the call is joined to make sure we don't switch and override
                // the settings during a call.
                val settings = call.state.settings.value
                if (settings != null) {
                    call.updateMediaManagerFromSettings(settings)
                } else {
                    logger.w {
                        "[join] Call settings were null - this should never happen after a call" +
                            "is joined. MediaManager will not be initialised with server settings."
                    }
                }
                return result
            }
            if (result is Failure) {
                onJoinFail()
//                session = null
                logger.e { "Join failed with error $result" }
                if (isPermanentError(result.value)) {
                    call.state._connection.value = RealtimeConnection.Failed(result.value)
                    return result
                } else {
                    retryCount += 1
                }
            }
            delay(retryCount - 1 * 1000L)
        }
        return onJoinFailAfterAllRetries()
    }

    private fun onJoinFailAfterAllRetries(): Result<RtcSession> {
        onJoinFail()
//        session = null
        val errorMessage = "Join failed after 3 retries"
        call.state._connection.value = RealtimeConnection.Failed(errorMessage)
        return Failure(value = io.getstream.result.Error.GenericError(errorMessage))
    }

    @SuppressLint("VisibleForTests")
    override suspend fun rejoin(reason: String) = schedule("rejoin") {
        logger.d { "[rejoin] Rejoining" }
        onRejoin(reason)
    }

    private suspend fun schedule(key: String, block: suspend () -> Unit) {
        logger.d { "[schedule] #reconnect; no args" }
        streamSingleFlightProcessorImpl.run(key, block)
    }
    internal fun isPermanentError(error: Any): Boolean {
        if (error is Error.ThrowableError) {
            if (error.message.contains("Unable to resolve host")) {
                return false
            }
        }
        return true
    }
}
