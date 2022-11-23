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

package io.getstream.video.android

import io.getstream.video.android.call.CallClient
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallEventType
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.SfuToken
import io.getstream.video.android.model.StreamCallCid
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.StreamCallType
import io.getstream.video.android.model.User
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.utils.Result
import kotlinx.coroutines.flow.StateFlow

public interface StreamVideo {

    /**
     * Represents the state of the current call, if active. If there is no call fully joined, we'll
     * keep intermediate states, such as [StreamCallState.Idle].
     */
    public val callState: StateFlow<StreamCallState>

    /**
     * Creates a call with given information. You can then use the [CallMetadata] and join it and get auth
     * information to fully connect. This is different from [getOrCreateCall] because if the
     * call already exists, we'll return an error.
     *
     * @param type The call type.
     * @param id The call ID.
     * @param participantIds List of other people to invite to the call.
     *
     * @return [Result] which contains the [CallMetadata] and its information.
     */
    public suspend fun createCall(
        type: StreamCallType,
        id: StreamCallId,
        participantIds: List<String> = emptyList(),
        ringing: Boolean
    ): Result<CallMetadata>

    /**
     * Creates a call with given information. You can then use the [CallMetadata] and join it and get auth
     * information to fully connect.
     *
     * @param type The call type.
     * @param id The call ID.
     * @param participantIds List of other people to invite to the call.
     *
     * @return [Result] which contains the [CallMetadata] and its information.
     */
    public suspend fun getOrCreateCall(
        type: StreamCallType,
        id: StreamCallId,
        participantIds: List<String> = emptyList(),
        ringing: Boolean
    ): Result<CallMetadata>

    /**
     * Creates a call with given information and then authenticates the user to join the said [CallMetadata].
     *
     * @param type The call type.
     * @param id The call ID.
     * @param participantIds List of other people to invite to the call.
     *
     * @return [Result] which contains the [JoinedCall] with the auth information required to fully
     * connect.
     */
    // TODO createAndJoin is misleading, because internally it uses getOrCreate and then join
    //  we might need to choose a better name
    public suspend fun createAndJoinCall(
        type: StreamCallType,
        id: StreamCallId,
        participantIds: List<String>,
        ringing: Boolean
    ): Result<JoinedCall>

    /**
     * Authenticates the user to join a given Call based on the [type] and [id].
     *
     * @param type The call type.
     * @param id The call ID.
     *
     * @return [Result] which contains the [JoinedCall] with the auth information required to fully
     * connect.
     */
    public suspend fun joinCall(type: StreamCallType, id: StreamCallId): Result<JoinedCall>

    /**
     * Authenticates the user to join a given Call using the [CallMetadata].
     *
     * @param call The existing call or room metadata which is used to join a Call.
     *
     * @return [Result] which contains the [JoinedCall] with the auth information required to fully
     * connect.
     */
    public suspend fun joinCall(call: CallMetadata): Result<JoinedCall>

    /**
     * Sends invite to people for an existing call.
     *
     * @param users The users to invite.
     * @param cid The call ID.
     * @return [Result] if the operation is successful or not.
     */
    public suspend fun inviteUsers(users: List<User>, cid: StreamCallCid): Result<Unit>

    /**
     * Sends a specific event related to an active [Call].
     *
     * @param eventType The event type, such as accepting or declining a call.
     * @return [Result] which contains if the event was successfully sent.
     */
    public suspend fun sendEvent(
        callCid: StreamCallCid,
        eventType: CallEventType
    ): Result<Boolean>

    /**
     * Sends a custom event related to an active [Call].
     *
     * @param callCid The CID of the channel, describing the type and id.
     * @param dataJson The data JSON encoded.
     * @return [Result] which contains if the event was successfully sent.
     */
    public suspend fun sendCustomEvent(
        callCid: StreamCallCid,
        dataJson: String
    ): Result<Boolean>

    /**
     * Leaves the currently active call and clears up all connections to it.
     */
    // TODO
    //  can be called internally when [StreamCallState.Idle] comes into the place.
    public fun clearCallState()

    /**
     * Gets the current user information.
     *
     * @return The currently logged in [User].
     */
    public fun getUser(): User

    /**
     * Adds a listener to the active socket connection, to observe various events.
     *
     * @param socketListener The listener to add.
     */
    public fun addSocketListener(socketListener: SocketListener)

    /**
     * Removes a given listener from the socket observers.
     *
     * @param socketListener The listener to remove.
     */
    public fun removeSocketListener(socketListener: SocketListener)

    /**
     * Creates an instance of the [CallClient] for the given call input, which is persisted and
     * used to communicate with the BE.
     *
     * Use it to control the track state, mute/unmute devices and listen to call events.
     *
     * @param signalUrl The URL of the server in which the call is being hosted.
     * @param sfuToken User's ticket to enter the call.
     * @param iceServers Servers required to appropriately connect to the call and receive tracks.
     * @return An instance of [CallClient] ready to connect to a call. Make sure to call
     * [CallClient.connectToCall] when you're ready to fully join a call.
     */
    public fun createCallClient(
        signalUrl: String,
        sfuToken: SfuToken,
        iceServers: List<IceServer>
    ): CallClient

    /**
     * Returns current [CallClient] instance.
     */
    public fun getActiveCallClient(): CallClient?

    /**
     * Awaits [CallClient] creation.
     */
    public suspend fun awaitCallClient(): CallClient

    /**
     * Accepts incoming call.
     */
    public suspend fun acceptCall(cid: StreamCallCid): Result<JoinedCall>

    /**
     * Rejects incoming call.
     */
    public suspend fun rejectCall(cid: StreamCallCid): Result<Boolean>

    /**
     * Cancels outgoing or active call.
     */
    public suspend fun cancelCall(cid: StreamCallCid): Result<Boolean>
}
