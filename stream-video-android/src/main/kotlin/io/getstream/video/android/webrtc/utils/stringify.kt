package io.getstream.video.android.webrtc.utils

import org.webrtc.MediaStreamTrack
import org.webrtc.SessionDescription

internal fun SessionDescription.stringify(): String = "SessionDescription(type=$type, description=$description)"

internal fun MediaStreamTrack.stringify(): String {
    return "MediaStreamTrack(id=${id()}, kind=${kind()}, enabled: ${enabled()}, state=${state()})"
}