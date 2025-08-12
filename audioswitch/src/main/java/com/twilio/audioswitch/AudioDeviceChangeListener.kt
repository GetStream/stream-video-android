package com.twilio.audioswitch

/**
 * Receives a list of the most recently available [AudioDevice]s. Also provides the
 * currently selected [AudioDevice] from [AudioSwitch].
 * - audioDevices - The list of [AudioDevice]s or an empty list if none are available.
 * - selectedAudioDevice - The currently selected device or null if no device has been selected.
 */
typealias AudioDeviceChangeListener = (
    audioDevices: List<AudioDevice>,
    selectedAudioDevice: AudioDevice?,
) -> Unit
