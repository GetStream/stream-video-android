package io.getstream.video.android.core.model

import io.getstream.video.android.model.SfuToken

public data class Credentials(
    val token: SfuToken,
    val server: Server,
    val iceServers: List<IceServer>,
)