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
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.CreateDeviceRequest
import io.getstream.log.TaggedLogger
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.flatMapSuspend
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.handlers.CompatibilityStreamNotificationHandler
import io.getstream.video.android.core.notifications.internal.storage.DeviceTokenStorage
import io.getstream.video.android.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal class StreamNotificationManager private constructor(
    private val context: Context,
    private var scope: CoroutineScope,
    internal val notificationConfig: NotificationConfig,
    private var api: ProductvideoApi,
    internal val deviceTokenStorage: DeviceTokenStorage,
    private val notificationPermissionManager: NotificationPermissionManager?,
) : NotificationHandler by notificationConfig.notificationHandler {

    suspend fun registerPushDevice() {
        logger.d { "[registerPushDevice] no args" }
        // first get a push device generator that works for this device

        logger.d {
            "[registerPushDevice] no args, push device generator: ${notificationConfig.pushDeviceGenerators.size}"
        }
        notificationConfig.pushDeviceGenerators.forEach { it ->
            logger.d {
                "[registerPushDevice] no args, push device generator name: $it, valid: ${it.isValidForThisDevice()}"
            }
        }
        notificationConfig.pushDeviceGenerators
            .firstOrNull { it.isValidForThisDevice() }
            ?.let { generator ->
                generator.onPushDeviceGeneratorSelected()
                generator.asyncGeneratePushDevice { generatedDevice ->
                    logger.d { "[registerPushDevice] pushDevice generated: $generatedDevice" }
                    scope.launch { createDevice(generatedDevice) }
                }
                if (notificationConfig.requestPermissionOnDeviceRegistration()) {
                    notificationPermissionManager?.start()
                }
            }
    }

    suspend fun createDevice(pushDevice: PushDevice): Result<Device> {
        logger.d { "[createDevice] pushDevice: $pushDevice" }
        val newDevice = pushDevice.toDevice()
        return pushDevice
            .takeUnless {
                if (!notificationConfig.autoRegisterPushDevice) {
                    false
                } else {
                    val equal = newDevice == getDevice().firstOrNull()
                    logger.d { "[createDevice] Device equal to stored: $equal" }
                    equal
                }
            }
            ?.toCreateDeviceRequest()
            ?.flatMapSuspend { createDeviceRequest ->
                try {
                    val result = api.createDevice(createDeviceRequest)
                    updateDevice(pushDevice.toDevice())
                    Result.Success(newDevice)
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.e { "Failed to register device for push notifications with ${e.message}" }
                    logger.e(e) {
                        "Failed to register device for push notifications " +
                            "(PN will not work!). Does the push provider key " +
                            "(${pushDevice.pushProvider.key}) match the key in the Stream Dashboard?"
                    }
                    Result.Failure(Error.ThrowableError("Device couldn't be created", e))
                }
            }
            ?: Result.Success(newDevice)
    }

    private suspend fun removeStoredDevice(device: Device) {
        logger.d { "[removeStoredDevice] device: device" }
        getDevice().firstOrNull()
            .takeIf {
                val equal = it == device
                logger.d { "[removeStoredDevice] Device equal to stored: $equal" }
                equal
            }
            ?.let { updateDevice(null) }
    }

    fun getDevice(): Flow<Device?> = deviceTokenStorage.userDevice

    suspend fun updateDevice(device: Device?) {
        logger.d { "[updateUserDevice] device: $device" }
        deviceTokenStorage.updateUserDevice(device)
    }

    /**
     * @see StreamVideo.deleteDevice
     */
    suspend fun deleteDevice(device: Device): Result<Unit> {
        logger.d { "[deleteDevice] device: $device" }
        return try {
            api.deleteDevice(device.id)
            removeStoredDevice(device)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Error.ThrowableError("Device couldn't be deleted", e))
        }
    }

    private fun PushDevice.toDevice(): Device =
        Device(
            id = this.token,
            pushProvider = this.pushProvider.key,
            pushProviderName = this.providerName,
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
            api: ProductvideoApi,
            deviceTokenStorage: DeviceTokenStorage,
        ): StreamNotificationManager {
            synchronized(this) {
                if (Companion::internalStreamNotificationManager.isInitialized) {
                    internalStreamNotificationManager.scope = scope
                    internalStreamNotificationManager.api = api
                } else {
                    val application = context.applicationContext as? Application
                    val updatedNotificationConfig =
                        notificationConfig.overrideDefault(
                            application = application,
                            hideRingingNotificationInForeground = notificationConfig.hideRingingNotificationInForeground,
                        )
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
                        deviceTokenStorage,
                        notificationPermissionManager,
                    )
                }
                return internalStreamNotificationManager
            }
        }

        private fun NotificationConfig.overrideDefault(
            application: Application?,
            hideRingingNotificationInForeground: Boolean,
        ): NotificationConfig {
            return application?.let {
                val notificationHandler = notificationHandler
                    .takeUnless { it == NoOpNotificationHandler }
                    ?: CompatibilityStreamNotificationHandler(
                        application = application,
                        hideRingingNotificationInForeground = hideRingingNotificationInForeground,
                    )
                this.copy(notificationHandler = notificationHandler)
            } ?: this
        }
    }
}
