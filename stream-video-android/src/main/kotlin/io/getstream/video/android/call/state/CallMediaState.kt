package io.getstream.video.android.call.state

public data class CallMediaState(
    val isMicrophoneEnabled: Boolean = false,
    val isSpeakerphoneEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false
)