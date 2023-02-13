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

package io.getstream.video.android.core

import io.getstream.video.android.core.call.CallClient
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.CallEventType
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.Device
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.JoinedCall
import io.getstream.video.android.core.model.SfuToken
import io.getstream.video.android.core.model.StreamCallCid
import io.getstream.video.android.core.model.StreamCallGuid
import io.getstream.video.android.core.model.StreamCallId
import io.getstream.video.android.core.model.StreamCallType
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.socket.SocketListener
import io.getstream.video.android.core.utils.Result
import kotlinx.coroutines.flow.StateFlow

public interface StreamVideo {

    /**
     * Represents the state of the current call, if active. If there is no call fully joined, we'll
     * keep intermediate states, such as [StreamCallState.Idle].
     */
    public val callState: StateFlow<StreamCallState>

    /**
     * Represents the default call config when starting a call.
     */
    public val config: StreamVideoConfig

    /**
     * Create a device that will be used to receive push notifications.
     *
     * @param token The Token obtained from the selected push provider.
     * @param pushProvider The selected push provider.
     *
     * @return [Result] containing the [Device].
     */
    public suspend fun createDevice(
        token: String,
        pushProvider: String,
    ): Result<Device>

    /**
     * Remove a device used to receive push notifications.
     *
     * @param id The ID of the device, previously provided by [createDevice].
     * @return Result if the operation was successful or not.
     */
    public suspend fun deleteDevice(id: String): Result<Unit>

    /**
     * Removes the given devices from the current user's list of registered push devices.
     *
     * @param devices The list of devices to remove.
     */
    public fun removeDevices(devices: List<Device>)

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
        ring: Boolean
    ): Result<CallMetadata>

    /**
     * Queries or creates a call with given information and then authenticates the user to join the
     * said [CallMetadata].
     *
     * @param type The call type.
     * @param id The call ID.
     * @param participantIds List of other people to invite to the call.
     * @param ring If we should ring any of the participants. This doesn't work if we're joining
     * an existing call.
     *
     * @return [Result] which contains the [JoinedCall] with the auth information required to fully
     * connect.
     */
    public suspend fun joinCall(
        type: StreamCallType,
        id: StreamCallId,
        participantIds: List<String> = emptyList(),
        ring: Boolean = false
    ): Result<JoinedCall>

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
        dataJson: Map<String, Any>,
        eventType: String
    ): Result<Boolean>

    /**
     * Leaves the currently active call and clears up all connections to it.
     */
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
        callGuid: StreamCallGuid,
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

    /**
     * Used to process push notification payloads.
     */
    public suspend fun handlePushMessage(payload: Map<String, Any>): Result<Unit>
}
