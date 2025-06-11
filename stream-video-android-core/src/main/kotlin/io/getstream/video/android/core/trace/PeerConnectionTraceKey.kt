package io.getstream.video.android.core.trace

/**
 * All trace-key strings emitted by the peer-connection layer.
 */
enum class PeerConnectionTraceKey(val value: String) {

    CREATE("create"),
    JOIN_REQUEST("joinRequest"),
    ON_ICE_CANDIDATE("onicecandidate"),
    ON_TRACK("ontrack"),
    ON_SIGNALING_STATE_CHANGE("onsignalingstatechange"),
    ON_ICE_CONNECTION_STATE_CHANGE("oniceconnectionstatechange"),
    ON_ICE_GATHERING_STATE_CHANGE("onicegatheringstatechange"),
    ON_CONNECTION_STATE_CHANGE("onconnectionstatechange"),
    ON_NEGOTIATION_NEEDED("onnegotiationneeded"),
    ON_DATA_CHANNEL("ondatachannel"),
    CLOSE("close"),
    CREATE_OFFER("createOffer"),
    CREATE_ANSWER("createAnswer"),
    SET_LOCAL_DESCRIPTION("setLocalDescription"),
    SET_REMOTE_DESCRIPTION("setRemoteDescription"),
    ADD_ICE_CANDIDATE("addIceCandidate"),
    CHANGE_PUBLISH_QUALITY("changePublishQuality"),
    CHANGE_PUBLISH_OPTIONS("changePublishOptions"),
    GO_AWAY("goAway"),
    SFU_ERROR("error"),
    CALL_ENDED("callEnded");

    /** Log / serialise using the original string value. */
    override fun toString(): String = value
}

