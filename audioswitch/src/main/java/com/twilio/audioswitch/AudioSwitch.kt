package com.twilio.audioswitch

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.twilio.audioswitch.AudioDevice.BluetoothHeadset
import com.twilio.audioswitch.AudioDevice.Earpiece
import com.twilio.audioswitch.AudioDevice.Speakerphone
import com.twilio.audioswitch.AudioDevice.WiredHeadset
import com.twilio.audioswitch.AudioSwitch.State.ACTIVATED
import com.twilio.audioswitch.AudioSwitch.State.STARTED
import com.twilio.audioswitch.AudioSwitch.State.STOPPED
import com.twilio.audioswitch.android.Logger
import com.twilio.audioswitch.android.PermissionsCheckStrategy
import com.twilio.audioswitch.android.ProductionLogger
import com.twilio.audioswitch.bluetooth.BluetoothHeadsetConnectionListener
import com.twilio.audioswitch.bluetooth.BluetoothHeadsetManager
import com.twilio.audioswitch.wired.WiredDeviceConnectionListener
import com.twilio.audioswitch.wired.WiredHeadsetReceiver

private const val TAG = "AudioSwitch"
private const val PERMISSION_ERROR_MESSAGE = "Bluetooth unsupported, permissions not granted"

/**
 * This class enables developers to enumerate available audio devices and select which device audio
 * should be routed to. It is strongly recommended that instances of this class are created and
 * accessed from a single application thread. Accessing an instance from multiple threads may cause
 * synchronization problems.
 *
 * @property bluetoothHeadsetConnectionListener Requires bluetooth permission. Listener to notify if Bluetooth device state has
 * changed (connect, disconnect, audio connect, audio disconnect) or failed to connect. Null by default.
 * @property loggingEnabled A property to configure AudioSwitch logging behavior. AudioSwitch logging is disabled by
 * default.
 * @property selectedAudioDevice Retrieves the selected [AudioDevice] from [AudioSwitch.selectDevice].
 * @property availableAudioDevices Retrieves the current list of available [AudioDevice]s.
**/
class AudioSwitch {
    private val context: Context
    private var logger: Logger = ProductionLogger()
    private val audioDeviceManager: AudioDeviceManager
    private val wiredHeadsetReceiver: WiredHeadsetReceiver
    internal var audioDeviceChangeListener: AudioDeviceChangeListener? = null
    private var selectedDevice: AudioDevice? = null
    private var userSelectedDevice: AudioDevice? = null
    private var wiredHeadsetAvailable = false
    private val mutableAudioDevices = ArrayList<AudioDevice>()
    private var bluetoothHeadsetManager: BluetoothHeadsetManager? = null
    private val preferredDeviceList: List<Class<out AudioDevice>>
    private var bluetoothHeadsetConnectionListener: BluetoothHeadsetConnectionListener? = null
    private val permissionsRequestStrategy: PermissionsCheckStrategy

    internal var state: State = STOPPED
    internal enum class State {
        STARTED, ACTIVATED, STOPPED
    }

    internal val bluetoothDeviceConnectionListener = object : BluetoothHeadsetConnectionListener {
        override fun onBluetoothHeadsetStateChanged(headsetName: String?, state: Int) {
            enumerateDevices(headsetName)
            bluetoothHeadsetConnectionListener?.onBluetoothHeadsetStateChanged(headsetName, state)
        }

        override fun onBluetoothScoStateChanged(state: Int) {
            bluetoothHeadsetConnectionListener?.onBluetoothScoStateChanged(state)
        }

        override fun onBluetoothHeadsetActivationError() {
            if (userSelectedDevice is BluetoothHeadset) userSelectedDevice = null
            enumerateDevices()
            bluetoothHeadsetConnectionListener?.onBluetoothHeadsetActivationError()
        }
    }

    internal val wiredDeviceConnectionListener = object : WiredDeviceConnectionListener {
        override fun onDeviceConnected() {
            wiredHeadsetAvailable = true
            enumerateDevices()
        }

        override fun onDeviceDisconnected() {
            wiredHeadsetAvailable = false
            enumerateDevices()
        }
    }

    internal val applicationLifecycleListener = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }

        override fun onActivityStarted(activity: Activity) { }

        override fun onActivityResumed(activity: Activity) {
            if (STOPPED != state && null == bluetoothHeadsetManager) {
                getBluetoothHeadsetManager()?.start(bluetoothDeviceConnectionListener)
            }
        }

        override fun onActivityPaused(activity: Activity) { }

        override fun onActivityStopped(activity: Activity) { }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }
        override fun onActivityDestroyed(activity: Activity) { }
    }

    var loggingEnabled: Boolean
        get() = logger.loggingEnabled

        set(value) {
            logger.loggingEnabled = value
        }
    val selectedAudioDevice: AudioDevice? get() = selectedDevice
    val availableAudioDevices: List<AudioDevice> = mutableAudioDevices

    /**
     * Constructs a new AudioSwitch instance.
     * - [context] - An Android Context.
     * - [bluetoothHeadsetConnectionListener] - A listener to notify if Bluetooth device state has
     * changed (connect, disconnect, audio connect, audio disconnect) or failed to connect. Null by default
     * - [loggingEnabled] - Toggle whether logging is enabled. This argument is false by default.
     * - [audioFocusChangeListener] - A listener that is invoked when the system audio focus is updated.
     * Note that updates are only sent to the listener after [activate] has been called.
     * - [preferredDeviceList] - The order in which [AudioSwitch] automatically selects and activates
     * an [AudioDevice]. This parameter is ignored if the [selectedAudioDevice] is not `null`.
     * The default preferred [AudioDevice] order is the following:
     * [BluetoothHeadset], [WiredHeadset], [Earpiece], [Speakerphone]
     * . The [preferredDeviceList] is added to the front of the default list. For example, if [preferredDeviceList]
     * is [Speakerphone] and [BluetoothHeadset], then the new preferred audio
     * device list will be:
     * [Speakerphone], [BluetoothHeadset], [WiredHeadset], [Earpiece].
     * An [IllegalArgumentException] is thrown if the [preferredDeviceList] contains duplicate [AudioDevice] elements.
     */
    @JvmOverloads
    constructor(
        context: Context,
        bluetoothHeadsetConnectionListener: BluetoothHeadsetConnectionListener? = null,
        loggingEnabled: Boolean = false,
        audioFocusChangeListener: OnAudioFocusChangeListener = OnAudioFocusChangeListener {},
        preferredDeviceList: List<Class<out AudioDevice>> = defaultPreferredDeviceList,
    ) : this(
        context.applicationContext,
        bluetoothHeadsetConnectionListener,
        ProductionLogger(loggingEnabled),
        audioFocusChangeListener,
        preferredDeviceList,
    )

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal constructor(
        context: Context,
        bluetoothHeadsetConnectionListener: BluetoothHeadsetConnectionListener?,
        logger: Logger,
        audioFocusChangeListener: OnAudioFocusChangeListener,
        preferredDeviceList: List<Class<out AudioDevice>>,
        audioDeviceManager: AudioDeviceManager = AudioDeviceManager(
            context,
            logger,
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
            audioFocusChangeListener = audioFocusChangeListener,
        ),
        wiredHeadsetReceiver: WiredHeadsetReceiver = WiredHeadsetReceiver(context, logger),
        permissionsCheckStrategy: PermissionsCheckStrategy = DefaultPermissionsCheckStrategy(context),
        bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager,
        bluetoothHeadsetManager: BluetoothHeadsetManager? = BluetoothHeadsetManager.newInstance(
            context,
            logger,
            bluetoothManager?.adapter,
            audioDeviceManager,
        ),
    ) {
        this.context = context
        this.logger = logger
        this.bluetoothHeadsetConnectionListener = bluetoothHeadsetConnectionListener
        this.audioDeviceManager = audioDeviceManager
        this.wiredHeadsetReceiver = wiredHeadsetReceiver
        this.preferredDeviceList = getPreferredDeviceList(preferredDeviceList)
        this.permissionsRequestStrategy = permissionsCheckStrategy
        this.bluetoothHeadsetManager = if (hasPermissions()) {
            bluetoothHeadsetManager
        } else {
            logger.w(TAG, PERMISSION_ERROR_MESSAGE)
            bluetoothHeadsetManager
            null
        }
        logger.d(TAG, "AudioSwitch($VERSION)")
        logger.d(TAG, "Preferred device list = ${this.preferredDeviceList.map { it.simpleName }}")
    }

    private fun getPreferredDeviceList(preferredDeviceList: List<Class<out AudioDevice>>): List<Class<out AudioDevice>> {
        require(hasNoDuplicates(preferredDeviceList))

        return if (preferredDeviceList.isEmpty() || preferredDeviceList == defaultPreferredDeviceList) {
            defaultPreferredDeviceList
        } else {
            val result = defaultPreferredDeviceList.toMutableList()
            result.removeAll(preferredDeviceList)
            preferredDeviceList.forEachIndexed { index, device ->
                result.add(index, device)
            }
            result
        }
    }

    /**
     * Starts listening for audio device changes and calls the [listener] upon each change if listener provided.
     * **Note:** When audio device listening is no longer needed, [AudioSwitch.stop] should be
     * called in order to prevent a memory leak.
     */
    fun start(listener: AudioDeviceChangeListener? = null) {
        listener?.let {
            audioDeviceChangeListener = it
        }
        when (state) {
            STOPPED -> {
                state = STARTED
                enumerateDevices()
                getBluetoothHeadsetManager()?.start(bluetoothDeviceConnectionListener)
                wiredHeadsetReceiver.start(wiredDeviceConnectionListener)
                (context.applicationContext as Application)
                    .registerActivityLifecycleCallbacks(applicationLifecycleListener)
            }
            else -> {
                logger.d(TAG, "Redundant start() invocation while already in the started or activated state")
            }
        }
    }

    /**
     * Adds [AudioDeviceChangeListener] and starts listening for audio devices changes and calls,
     * or null to stop listening for audio device changes.
     */
    fun setAudioDeviceChangeListener(listener: AudioDeviceChangeListener?) {
        audioDeviceChangeListener = listener
    }

    /**
     * Stops listening for audio device changes if [AudioSwitch.start] has already been
     * invoked. [AudioSwitch.deactivate] will also get called if a device has been activated
     * with [AudioSwitch.activate].
     */
    fun stop() {
        when (state) {
            ACTIVATED -> {
                deactivate()
                closeListeners()
            }
            STARTED -> {
                closeListeners()
            }
            STOPPED -> {
                logger.d(TAG, "Redundant stop() invocation while already in the stopped state")
            }
        }
    }

    /**
     * Performs audio routing and unmuting on the selected device from
     * [AudioSwitch.selectDevice]. Audio focus is also acquired for the client application.
     * **Note:** [AudioSwitch.deactivate] should be invoked to restore the prior audio
     * state.
     */
    fun activate() {
        when (state) {
            STARTED -> {
                state = ACTIVATED
                audioDeviceManager.cacheAudioState()

                // Always set mute to false for WebRTC
                audioDeviceManager.mute(false)
                audioDeviceManager.setAudioFocus()
                selectedDevice?.let { activate(it) }
            }
            ACTIVATED -> selectedDevice?.let { activate(it) }
            STOPPED -> throw IllegalStateException()
        }
    }

    /**
     * Restores the audio state prior to calling [AudioSwitch.activate] and removes
     * audio focus from the client application.
     */
    fun deactivate() {
        when (state) {
            ACTIVATED -> {
                state = STARTED
                getBluetoothHeadsetManager()?.deactivate()

                // Restore stored audio state
                audioDeviceManager.restoreAudioState()
            }
            STARTED, STOPPED -> {
            }
        }
    }

    /**
     * Selects the desired [audioDevice]. If the provided [AudioDevice] is not
     * available, no changes are made. If the provided device is null, one is chosen based on the
     * specified preferred device list or the following default list:
     * [BluetoothHeadset], [WiredHeadset], [Earpiece], [Speakerphone].
     */
    fun selectDevice(audioDevice: AudioDevice?) {
        if (selectedDevice != audioDevice) {
            logger.d(TAG, "Selected AudioDevice = $audioDevice")
            userSelectedDevice = audioDevice
            enumerateDevices()
        }
    }

    private fun hasNoDuplicates(list: List<Class<out AudioDevice>>) =
        list.groupingBy { it }.eachCount().filter { it.value > 1 }.isEmpty()

    private fun activate(audioDevice: AudioDevice) {
        when (audioDevice) {
            is BluetoothHeadset -> {
                audioDeviceManager.enableSpeakerphone(false)
                getBluetoothHeadsetManager()?.activate()
            }
            is Earpiece, is WiredHeadset -> {
                audioDeviceManager.enableSpeakerphone(false)
                getBluetoothHeadsetManager()?.deactivate()
            }
            is Speakerphone -> {
                audioDeviceManager.enableSpeakerphone(true)
                getBluetoothHeadsetManager()?.deactivate()
            }
        }
    }

    internal data class AudioDeviceState(
        val audioDeviceList: List<AudioDevice>,
        val selectedAudioDevice: AudioDevice?,
    )

    /**
     * Updates the list of available audio devices and determines the audio device to be selected.
     *
     * This function performs several key tasks:
     * 1. Refreshes the `mutableAudioDevices` list by calling [addAvailableAudioDevices],
     *    optionally using the provided `bluetoothHeadsetName` to identify a specific Bluetooth device.
     * 2. Validates if a previously user-selected device (`userSelectedDevice`) is still present
     *    in the updated list. If not, the user's selection is cleared.
     * 3. Determines the `selectedDevice` based on the following priority:
     *     - The `userSelectedDevice`, if it's valid and available.
     *     - Otherwise, the first device from the `mutableAudioDevices` list (which respects
     *       the preferred device order).
     *     - A fallback mechanism is included: if the top preferred device is a Bluetooth headset
     *       and it has experienced an SCO activation error, the next available device is chosen.
     * 4. If the `AudioSwitch` is currently in the [State.ACTIVATED] state, this function
     *    will re-trigger the internal `activate()` method to ensure the newly determined
     *    `selectedDevice` becomes the active audio route.
     * 5. If the list of available devices or the `selectedDevice` has changed compared to the
     *    previous state, it invokes the [audioDeviceChangeListener] to notify listeners.
     *
     * This function is typically called internally in response to events like device connections/
     * disconnections or when a user explicitly calls [selectDevice].
     *
     * @param bluetoothHeadsetName The name of a Bluetooth headset, which can be used to help
     *                             identify a specific headset during device enumeration, especially
     *                             when a new Bluetooth device is connecting. This is optional.
     */
    private fun enumerateDevices(bluetoothHeadsetName: String? = null) {
        // save off the old state and 'semi'-deep copy the list of audio devices
        val oldAudioDeviceState = AudioDeviceState(mutableAudioDevices.map { it }, selectedDevice)
        // update audio device list and selected device
        addAvailableAudioDevices(bluetoothHeadsetName)

        if (!userSelectedDevicePresent(mutableAudioDevices)) {
            userSelectedDevice = null
        }

        // Select the audio device
        logger.d(TAG, "Current user selected AudioDevice = $userSelectedDevice")
        selectedDevice = if (userSelectedDevice != null) {
            userSelectedDevice
        } else if (mutableAudioDevices.size > 0) {
            val firstAudioDevice = mutableAudioDevices[0]
            /*
             * If there was an error starting bluetooth sco, then the selected AudioDevice should
             * be the next valid device in the list.
             */
            if (firstAudioDevice is BluetoothHeadset &&
                getBluetoothHeadsetManager()?.hasActivationError() == true
            ) {
                mutableAudioDevices[1]
            } else {
                firstAudioDevice
            }
        } else {
            null
        }

        // Activate the device if in the active state
        if (state == ACTIVATED) {
            activate()
        }
        // trigger audio device change listener if there has been a change
        val newAudioDeviceState = AudioDeviceState(mutableAudioDevices, selectedDevice)
        if (newAudioDeviceState != oldAudioDeviceState) {
            audioDeviceChangeListener?.invoke(mutableAudioDevices, selectedDevice)
        }
    }

    private fun addAvailableAudioDevices(bluetoothHeadsetName: String?) {
        mutableAudioDevices.clear()
        preferredDeviceList.forEach { audioDevice ->
            when (audioDevice) {
                BluetoothHeadset::class.java -> {
                /*
                 * Since the there is a delay between receiving the ACTION_ACL_CONNECTED event and receiving
                 * the name of the connected device from querying the BluetoothHeadset proxy class, the
                 * headset name received from the ACTION_ACL_CONNECTED intent needs to be passed into this
                 * function.
                 */
                    getBluetoothHeadsetManager()?.getHeadset(bluetoothHeadsetName)?.let {
                        mutableAudioDevices.add(it)
                    }
                }
                WiredHeadset::class.java -> {
                    if (wiredHeadsetAvailable) {
                        mutableAudioDevices.add(WiredHeadset())
                    }
                }
                Earpiece::class.java -> {
                    if (audioDeviceManager.hasEarpiece() && !wiredHeadsetAvailable) {
                        mutableAudioDevices.add(Earpiece())
                    }
                }
                Speakerphone::class.java -> {
                    if (audioDeviceManager.hasSpeakerphone()) {
                        mutableAudioDevices.add(Speakerphone())
                    }
                }
            }
        }

        logger.d(TAG, "Available AudioDevice list updated: $availableAudioDevices")
    }

    private fun userSelectedDevicePresent(audioDevices: List<AudioDevice>) =
        userSelectedDevice?.let { selectedDevice ->
            if (selectedDevice is BluetoothHeadset) {
                // Match any bluetooth headset as a new one may have been connected
                audioDevices.find { it is BluetoothHeadset }?.let { newHeadset ->
                    userSelectedDevice = newHeadset
                    true
                } ?: false
            } else {
                audioDevices.contains(selectedDevice)
            }
        } ?: false

    private fun closeListeners() {
        state = STOPPED
        getBluetoothHeadsetManager()?.stop()
        wiredHeadsetReceiver.stop()
        (context.applicationContext as Application)
            .unregisterActivityLifecycleCallbacks(applicationLifecycleListener)
        audioDeviceChangeListener = null
    }

    private fun getBluetoothHeadsetManager(): BluetoothHeadsetManager? {
        if (bluetoothHeadsetManager == null && hasPermissions()) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothHeadsetManager = BluetoothHeadsetManager.newInstance(
                context,
                logger,
                bluetoothManager?.adapter,
                audioDeviceManager,
            )
        }
        return bluetoothHeadsetManager
    }

    internal fun hasPermissions() = permissionsRequestStrategy.hasPermissions()

    internal class DefaultPermissionsCheckStrategy(private val context: Context) : PermissionsCheckStrategy {

        @SuppressLint("NewApi")
        override fun hasPermissions(): Boolean {
            return if (context.applicationInfo.targetSdkVersion <= android.os.Build.VERSION_CODES.R ||
                android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R
            ) {
                PERMISSION_GRANTED == context.checkPermission(
                    Manifest.permission.BLUETOOTH,
                    android.os.Process.myPid(),
                    android.os.Process.myUid(),
                )
            } else {
                // for android 12/S or newer
                PERMISSION_GRANTED == context.checkPermission(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    android.os.Process.myPid(),
                    android.os.Process.myUid(),
                )
            }
        }
    }

    companion object {
        /**
         * The version of the AudioSwitch library.
         */
        const val VERSION = "1.0.0-rahul"

        private val defaultPreferredDeviceList by lazy {
            listOf(
                BluetoothHeadset::class.java,
                WiredHeadset::class.java,
                Earpiece::class.java,
                Speakerphone::class.java,
            )
        }
    }
}
