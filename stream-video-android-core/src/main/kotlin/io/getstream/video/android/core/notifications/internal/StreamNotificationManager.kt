/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import io.getstream.android.push.PushDevice
import io.getstream.android.push.PushProvider
import io.getstream.android.push.permissions.NotificationPermissionManager
import io.getstream.android.push.permissions.NotificationPermissionStatus
import io.getstream.android.push.permissions.NotificationPermissionStatus.DENIED
import io.getstream.android.push.permissions.NotificationPermissionStatus.GRANTED
import io.getstream.android.push.permissions.NotificationPermissionStatus.RATIONALE_NEEDED
import io.getstream.android.push.permissions.NotificationPermissionStatus.REQUESTED
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.flatMapSuspend
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.DefaultNotificationHandler
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.models.CreateDeviceRequest

internal class StreamNotificationManager private constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notificationConfig: NotificationConfig,
    private val api: DefaultApi,
    private val dataStore: StreamUserDataStore,
    private val notificationPermissionManager: NotificationPermissionManager?,
) : NotificationHandler by notificationConfig.notificationHandler {

    suspend fun registerPushDevice() {
        logger.d { "[registerPushDevice] no args" }
        // first get a push device generator that works for this device
        notificationConfig.pushDeviceGenerators
            .firstOrNull { it.isValidForThisDevice(context) }
            ?.let { generator ->
                generator.onPushDeviceGeneratorSelected()
                generator.asyncGeneratePushDevice { generatedDevice ->
                    logger.d { "[registerPushDevice] pushDevice gnerated: $generatedDevice" }
                    scope.launch { createDevice(generatedDevice) }
                }
                notificationPermissionManager?.start()
            }
    }

    suspend fun createDevice(pushDevice: PushDevice): Result<Device> {
        logger.d { "[createDevice] pushDevice: $pushDevice" }
        val newDevice = pushDevice.toDevice()
        return pushDevice
            .takeUnless { newDevice == dataStore.userDevice.firstOrNull() }
            ?.toCreateDeviceRequest()
            ?.flatMapSuspend { createDeviceRequest ->
                try {
                    api.createDevice(createDeviceRequest)
                    dataStore.updateUserDevice(pushDevice.toDevice())
                    Result.Success(newDevice)
                } catch (e: Exception) {
                    Result.Failure(Error.ThrowableError("Device couldn't be created", e))
                }
            }
            ?: Result.Success(newDevice)
    }

    private fun removeStoredDeivce(device: Device) {
        logger.d { "[storeDevice] device: device" }
        scope.launch {
            dataStore.userDevice.firstOrNull()
                .takeIf { it == device }
                ?.let { dataStore.updateUserDevice(null) }
        }
    }

    /**
     * @see StreamVideo.deleteDevice
     */
    suspend fun deleteDevice(device: Device): Result<Unit> {
        logger.d { "[deleteDevice] device: $device" }
        val userId = dataStore.user.firstOrNull()?.id
        return try {
            api.deleteDevice(device.id, userId)
            removeStoredDeivce(device)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Error.ThrowableError("Device couldn't be deleted", e))
        }
    }

    private fun PushDevice.toDevice(): Device =
        Device(
            id = this.token,
            pushProvider = this.pushProvider.key,
            pushProviderName = this.providerName ?: "",
        )
    private fun PushDevice.toCreateDeviceRequest(): Result<CreateDeviceRequest> =
        when (pushProvider) {
            PushProvider.FIREBASE -> Result.Success(CreateDeviceRequest.PushProvider.Firebase)
            PushProvider.HUAWEI -> Result.Success(CreateDeviceRequest.PushProvider.Huawei)
            PushProvider.XIAOMI -> Result.Success(CreateDeviceRequest.PushProvider.Xiaomi)
            PushProvider.UNKNOWN -> Result.Failure(Error.GenericError("Unsupported PushProvider"))
        }.map {
            CreateDeviceRequest(
                id = token,
                pushProvider = it,
                pushProviderName = providerName,
            )
        }

    internal companion object {
        private val logger: TaggedLogger by taggedLogger("StreamVideo:Notifications")

        @SuppressLint("StaticFieldLeak")
        private lateinit var internalStreamNotificationManager: StreamNotificationManager
        internal fun install(
            context: Context,
            scope: CoroutineScope,
            notificationConfig: NotificationConfig,
            api: DefaultApi,
            streamUserDataStore: StreamUserDataStore,
        ): StreamNotificationManager {
            synchronized(this) {
                if (Companion::internalStreamNotificationManager.isInitialized) {
                    logger.e {
                        "The $internalStreamNotificationManager is already installed but you've " +
                            "tried to install a new one."
                    }
                } else {
                    val application = context.applicationContext as? Application
                    val updatedNotificationConfig =
                        notificationConfig.overrideDefault(application)
                    val onPermissionStatus: (NotificationPermissionStatus) -> Unit = { nps ->
                        with(updatedNotificationConfig.notificationHandler) {
                            when (nps) {
                                REQUESTED -> onPermissionRequested()
                                GRANTED -> onPermissionGranted()
                                DENIED -> onPermissionDenied()
                                RATIONALE_NEEDED -> onPermissionRationale()
                            }
                        }
                    }
                    val notificationPermissionManager = application?.let {
                        NotificationPermissionManager.createNotificationPermissionsManager(
                            it,
                            notificationConfig.requestPermissionOnAppLaunch,
                            onPermissionStatus = onPermissionStatus,
                        )
                    }
                    internalStreamNotificationManager = StreamNotificationManager(
                        context,
                        scope,
                        updatedNotificationConfig,
                        api,
                        streamUserDataStore,
                        notificationPermissionManager,
                    )
                }
                return internalStreamNotificationManager
            }
        }

        private fun NotificationConfig.overrideDefault(application: Application?): NotificationConfig {
            return application?.let {
                val notificationHandler = notificationHandler
                    .takeUnless { it == NoOpNotificationHandler }
                    ?: DefaultNotificationHandler(application)
                this.copy(notificationHandler = notificationHandler)
            } ?: this
        }
    }
}
