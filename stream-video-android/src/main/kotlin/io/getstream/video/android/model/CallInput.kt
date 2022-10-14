package io.getstream.video.android.model

public data class CallInput(
    internal val callId: String,
    internal val callUrl: String,
    internal val userToken: String,
    internal val iceServers: List<IceServer>,
) : java.io.Serializable