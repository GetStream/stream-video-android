/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.audio

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import io.getstream.video.android.audio.BluetoothHeadsetManager.HeadsetState.AudioActivated
import io.getstream.video.android.audio.BluetoothHeadsetManager.HeadsetState.AudioActivating
import io.getstream.video.android.audio.BluetoothHeadsetManager.HeadsetState.AudioActivationError
import io.getstream.video.android.audio.BluetoothHeadsetManager.HeadsetState.Connected
import io.getstream.video.android.audio.BluetoothHeadsetManager.HeadsetState.Disconnected

internal class BluetoothHeadsetManager
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    audioDeviceManager: AudioManagerAdapter,
    var headsetListener: BluetoothHeadsetConnectionListener? = null,
    bluetoothScoHandler: Handler = Handler(Looper.getMainLooper()),
    private val bluetoothIntentProcessor: BluetoothIntentProcessor = BluetoothIntentProcessorImpl(),
    private var headsetProxy: BluetoothHeadset? = null,
    private val permissionsRequestStrategy: PermissionsCheckStrategy = DefaultPermissionsCheckStrategy(
        context
    ),
    private var hasRegisteredReceivers: Boolean = false
) : BluetoothProfile.ServiceListener, BroadcastReceiver() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var headsetState: HeadsetState = Disconnected
        set(value) {
            if (field != value) {
                field = value
                if (value == Disconnected) enableBluetoothScoJob.cancelBluetoothScoJob()
            }
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val enableBluetoothScoJob: EnableBluetoothScoJob =
        EnableBluetoothScoJob(audioDeviceManager, bluetoothScoHandler)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val disableBluetoothScoJob: DisableBluetoothScoJob =
        DisableBluetoothScoJob(audioDeviceManager, bluetoothScoHandler)

    companion object {
        internal fun newInstance(
            context: Context,
            bluetoothAdapter: BluetoothAdapter?,
            audioDeviceManager: AudioManagerAdapter
        ): BluetoothHeadsetManager? {
            return bluetoothAdapter?.let { adapter ->
                BluetoothHeadsetManager(context, adapter, audioDeviceManager)
            } ?: run {
                null
            }
        }
    }

    override fun onServiceConnected(profile: Int, bluetoothProfile: BluetoothProfile) {
        headsetProxy = bluetoothProfile as BluetoothHeadset
        if (hasConnectedDevice()) {
            connect()
            headsetListener?.onBluetoothHeadsetStateChanged(getHeadsetName())
        }
    }

    override fun onServiceDisconnected(profile: Int) {
        headsetState = Disconnected
        headsetListener?.onBluetoothHeadsetStateChanged()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (isCorrectIntentAction(intent.action)) {
            intent.getHeadsetDevice()?.let { bluetoothDevice ->
                intent.getIntExtra(
                    BluetoothHeadset.EXTRA_STATE,
                    BluetoothHeadset.STATE_DISCONNECTED
                ).let { state ->
                    when (state) {
                        BluetoothHeadset.STATE_CONNECTED -> {
                            connect()
                            headsetListener?.onBluetoothHeadsetStateChanged(bluetoothDevice.name)
                        }
                        BluetoothHeadset.STATE_DISCONNECTED -> {
                            disconnect()
                            headsetListener?.onBluetoothHeadsetStateChanged()
                        }
                        BluetoothHeadset.STATE_AUDIO_CONNECTED -> {
                            enableBluetoothScoJob.cancelBluetoothScoJob()
                            headsetState = AudioActivated
                            headsetListener?.onBluetoothHeadsetStateChanged()
                        }
                        BluetoothHeadset.STATE_AUDIO_DISCONNECTED -> {
                            disableBluetoothScoJob.cancelBluetoothScoJob()
                            /*
                             * This block is needed to restart bluetooth SCO in the event that
                             * the active bluetooth headset has changed.
                             */
                            if (hasActiveHeadsetChanged()) {
                                enableBluetoothScoJob.executeBluetoothScoJob()
                            }

                            headsetListener?.onBluetoothHeadsetStateChanged()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun start(headsetListener: BluetoothHeadsetConnectionListener) {
        if (hasPermissions()) {
            this.headsetListener = headsetListener

            bluetoothAdapter.getProfileProxy(
                context,
                this,
                BluetoothProfile.HEADSET
            )
            if (!hasRegisteredReceivers) {
                context.registerReceiver(
                    this, IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                )
                context.registerReceiver(
                    this, IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
                )
                hasRegisteredReceivers = true
            }
        }
    }

    fun stop() {
        if (hasPermissions()) {
            headsetListener = null
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, headsetProxy)
            if (hasRegisteredReceivers) {
                context.unregisterReceiver(this)
                hasRegisteredReceivers = false
            }
        }
    }

    fun activate() {
        if (hasPermissions() && headsetState == Connected || headsetState == AudioActivationError) {
            enableBluetoothScoJob.executeBluetoothScoJob()
        }
    }

    fun deactivate() {
        if (headsetState == AudioActivated) {
            disableBluetoothScoJob.executeBluetoothScoJob()
        }
    }

    fun hasActivationError(): Boolean {
        return if (hasPermissions()) {
            headsetState == AudioActivationError
        } else {
            false
        }
    }

    // TODO Remove bluetoothHeadsetName param
    fun getHeadset(bluetoothHeadsetName: String?): AudioDevice.BluetoothHeadset? {
        return if (hasPermissions()) {
            if (headsetState != Disconnected) {
                val headsetName = bluetoothHeadsetName ?: getHeadsetName()
                headsetName?.let { AudioDevice.BluetoothHeadset(it) }
                    ?: AudioDevice.BluetoothHeadset()
            } else
                null
        } else {
            null
        }
    }

    private fun isCorrectIntentAction(intentAction: String?) =
        intentAction == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED || intentAction == BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED

    private fun connect() {
        if (!hasActiveHeadset()) headsetState = Connected
    }

    private fun disconnect() {
        headsetState = when {
            hasActiveHeadset() -> {
                AudioActivated
            }
            hasConnectedDevice() -> {
                Connected
            }
            else -> {
                Disconnected
            }
        }
    }

    private fun hasActiveHeadsetChanged() =
        headsetState == AudioActivated && hasConnectedDevice() && !hasActiveHeadset()

    @SuppressLint("MissingPermission")
    private fun getHeadsetName(): String? =
        headsetProxy?.let { proxy ->
            proxy.connectedDevices?.let { devices ->
                when {
                    devices.size > 1 && hasActiveHeadset() -> {
                        val device = devices.find { proxy.isAudioConnected(it) }?.name
                        device
                    }
                    devices.size == 1 -> {
                        val device = devices.first().name
                        device
                    }
                    else -> {
                        null
                    }
                }
            }
        }

    @SuppressLint("MissingPermission")
    private fun hasActiveHeadset() =
        headsetProxy?.let { proxy ->
            proxy.connectedDevices?.let { devices ->
                devices.any { proxy.isAudioConnected(it) }
            }
        } ?: false

    @SuppressLint("MissingPermission")
    private fun hasConnectedDevice() =
        headsetProxy?.let { proxy ->
            proxy.connectedDevices?.isNotEmpty()
        } ?: false

    private fun Intent.getHeadsetDevice(): BluetoothDeviceWrapper? =
        bluetoothIntentProcessor.getBluetoothDevice(this)?.let { device ->
            if (isHeadsetDevice(device)) device else null
        }

    private fun isHeadsetDevice(deviceWrapper: BluetoothDeviceWrapper): Boolean =
        deviceWrapper.deviceClass?.let { deviceClass ->
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
                deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                deviceClass == BluetoothClass.Device.Major.UNCATEGORIZED
        } ?: false

    private fun hasPermissions() = permissionsRequestStrategy.hasPermissions()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal sealed class HeadsetState {
        object Disconnected : HeadsetState()
        object Connected : HeadsetState()
        object AudioActivating : HeadsetState()
        object AudioActivationError : HeadsetState()
        object AudioActivated : HeadsetState()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal inner class EnableBluetoothScoJob(
        private val audioManager: AudioManagerAdapter,
        bluetoothScoHandler: Handler,
    ) : BluetoothScoJob(bluetoothScoHandler) {

        override fun scoAction() {
            audioManager.enableBluetoothSco(true)
            headsetState = AudioActivating
        }

        override fun scoTimeOutAction() {
            headsetState = AudioActivationError
            headsetListener?.onBluetoothHeadsetActivationError()
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal inner class DisableBluetoothScoJob(
        private val audioManager: AudioManagerAdapter,
        bluetoothScoHandler: Handler,
    ) : BluetoothScoJob(bluetoothScoHandler) {

        override fun scoAction() {
            audioManager.enableBluetoothSco(false)
            headsetState = Connected
        }

        override fun scoTimeOutAction() {
            headsetState = AudioActivationError
        }
    }

    internal class DefaultPermissionsCheckStrategy(private val context: Context) :
        PermissionsCheckStrategy {
        @SuppressLint("NewApi")
        override fun hasPermissions(): Boolean {
            return when (android.os.Build.VERSION.SDK_INT) {
                in android.os.Build.VERSION_CODES.BASE..android.os.Build.VERSION_CODES.R -> {
                    PackageManager.PERMISSION_GRANTED == context.checkPermission(
                        Manifest.permission.BLUETOOTH,
                        android.os.Process.myPid(),
                        android.os.Process.myUid()
                    )
                }
                else -> {
                    // for android 12/S or newer
                    PackageManager.PERMISSION_GRANTED == context.checkPermission(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        android.os.Process.myPid(),
                        android.os.Process.myUid()
                    )
                }
            }
        }
    }
}
