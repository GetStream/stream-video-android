package io.getstream.video.android.core

import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public data class SFUConnection(internal val callUrl: String,
                                internal val sfuToken: SfuToken,
                                internal val iceServers: List<IceServer>) {

}

/**
 * Related see
 * - CallMetadata
 * - JoinedCall
 * - CallViewModel
 * - Compose lib Call.kt object
 *
 * Sometimes you want the raw server response
 * Usually you want state that updates though..
 */
public class Call2(
    private val client: StreamVideo,
    private val type: String,
    private val id: String,
    private val token: String="",

    ) {
    private val cid = "$type:$id"
    public var custom : Map<String, Any>? = null

    private val _participants: MutableStateFlow<List<CallParticipantState>> =
        MutableStateFlow(emptyList())
    public val participants: StateFlow<List<CallParticipantState>> = _participants

    private val _members: MutableStateFlow<List<CallParticipantState>> =
        MutableStateFlow(emptyList())
    public val members: StateFlow<List<CallParticipantState>> = _members


    // should be a stateflow
    private var sfuConnection: SFUConnection? = null

    suspend fun join(): Result<JoinedCall> {
        return client.joinCall(
            type,
            id,
            emptyList(),
            false
        )
    }

    suspend fun goLive(): Result<CallInfo> {
        return client.goLive(cid)
    }

    fun leave() {

    }

    suspend fun end(): Result<Unit> {
        return client.endCall(cid)
    }

    /** Basic crud operations */
    suspend fun get(): Result<CallMetadata> {
        return client.getOrCreateCall(type, id)
    }
    suspend fun create(): Result<CallMetadata> {
        return client.getOrCreateCall(type, id)
    }
    suspend fun update(): Result<CallInfo> {
        return client.updateCall(type, id, custom ?: emptyMap())
    }

    /** Permissions */
    suspend fun requestPermissions(permissions: List<String>): Result<Unit> {
        return client.requestPermissions("$id:$type", permissions)
    }
}

/** send custom and regular events */

