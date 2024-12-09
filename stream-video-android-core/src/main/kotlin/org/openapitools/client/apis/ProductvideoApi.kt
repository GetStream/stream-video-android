/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package org.openapitools.client.apis


import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query






import org.openapitools.client.models.AcceptCallResponse
import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.CollectUserFeedbackRequest
import org.openapitools.client.models.CollectUserFeedbackResponse
import org.openapitools.client.models.CreateDeviceRequest
import org.openapitools.client.models.CreateGuestRequest
import org.openapitools.client.models.CreateGuestResponse
import org.openapitools.client.models.DeleteRecordingResponse
import org.openapitools.client.models.DeleteTranscriptionResponse
import org.openapitools.client.models.EndCallResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetCallStatsResponse
import org.openapitools.client.models.GetEdgesResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveRequest
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallRequest
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.ListDevicesResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.ListTranscriptionsResponse
import org.openapitools.client.models.MuteUsersRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.PinRequest
import org.openapitools.client.models.PinResponse
import org.openapitools.client.models.QueryCallMembersRequest
import org.openapitools.client.models.QueryCallMembersResponse
import org.openapitools.client.models.QueryCallStatsRequest
import org.openapitools.client.models.QueryCallStatsResponse
import org.openapitools.client.models.QueryCallsRequest
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.RejectCallRequest
import org.openapitools.client.models.RejectCallResponse
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.RequestPermissionResponse
import org.openapitools.client.models.Response
import org.openapitools.client.models.SendCallEventRequest
import org.openapitools.client.models.SendCallEventResponse
import org.openapitools.client.models.SendReactionRequest
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.StartHLSBroadcastingResponse
import org.openapitools.client.models.StartRecordingRequest
import org.openapitools.client.models.StartRecordingResponse
import org.openapitools.client.models.StartTranscriptionRequest
import org.openapitools.client.models.StartTranscriptionResponse
import org.openapitools.client.models.StopHLSBroadcastingResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.StopRecordingResponse
import org.openapitools.client.models.StopTranscriptionResponse
import org.openapitools.client.models.UnblockUserRequest
import org.openapitools.client.models.UnblockUserResponse
import org.openapitools.client.models.UnpinRequest
import org.openapitools.client.models.UnpinResponse
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdateUserPermissionsRequest
import org.openapitools.client.models.UpdateUserPermissionsResponse

interface ProductvideoApi {
    /**
     * Accept Call
     *   Sends events: - call.accepted  Required permissions: - JoinCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [AcceptCallResponse]
     */
    @POST("/video/call/{type}/{id}/accept")
    suspend fun acceptCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): AcceptCallResponse

    /**
     * Block user on a call
     * Block a user, preventing them from joining the call until they are unblocked.  Sends events: - call.blocked_user  Required permissions: - BlockUser
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param blockUserRequest
     * @return [BlockUserResponse]
     */
    @POST("/video/call/{type}/{id}/block")
    suspend fun blockUser(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body blockUserRequest: BlockUserRequest
    ): BlockUserResponse

    /**
     * Collect user feedback
     *   Required permissions: - JoinCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param session
     * @param collectUserFeedbackRequest
     * @return [CollectUserFeedbackResponse]
     */
    @POST("/video/call/{type}/{id}/feedback/{session}")
    suspend fun collectUserFeedback(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
        @Body collectUserFeedbackRequest: CollectUserFeedbackRequest
    ): CollectUserFeedbackResponse

    /**
     * Create device
     * Adds a new device to a user, if the same device already exists the call will have no effect
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createDeviceRequest
     * @return [Response]
     */
    @POST("/video/devices")
    suspend fun createDevice(
        @Body createDeviceRequest: CreateDeviceRequest
    ): Response

    /**
     * Create Guest
     *
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createGuestRequest
     * @return [CreateGuestResponse]
     */
    @POST("/video/guest")
    suspend fun createGuest(
        @Body createGuestRequest: CreateGuestRequest
    ): CreateGuestResponse

    /**
     * Delete device
     * Deletes one device
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param id
     * @param userId  (optional)
     * @return [Response]
     */
    @DELETE("/video/devices")
    suspend fun deleteDevice(
        @Query("id") id: String,
        @Query("user_id") userId: String? = null
    ): Response

    /**
     * Delete recording
     * Deletes recording  Required permissions: - DeleteRecording
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param session
     * @param filename
     * @return [DeleteRecordingResponse]
     */
    @DELETE("/video/call/{type}/{id}/{session}/recordings/{filename}")
    suspend fun deleteRecording(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
        @Path("filename") filename: String
    ): DeleteRecordingResponse

    /**
     * Delete transcription
     * Deletes transcription  Required permissions: - DeleteTranscription
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param session
     * @param filename
     * @return [DeleteTranscriptionResponse]
     */
    @DELETE("/video/call/{type}/{id}/{session}/transcriptions/{filename}")
    suspend fun deleteTranscription(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
        @Path("filename") filename: String
    ): DeleteTranscriptionResponse

    /**
     * End call
     *   Sends events: - call.ended  Required permissions: - EndCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [EndCallResponse]
     */
    @POST("/video/call/{type}/{id}/mark_ended")
    suspend fun endCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): EndCallResponse

    /**
     * Get Call
     *   Required permissions: - ReadCall
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param connectionId  (optional)
     * @param membersLimit  (optional)
     * @param ring  (optional)
     * @param notify  (optional)
     * @return [GetCallResponse]
     */
    @GET("/video/call/{type}/{id}")
    suspend fun getCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("connection_id") connectionId: String? = null,
        @Query("members_limit") membersLimit: Int? = null,
        @Query("ring") ring: Boolean? = null,
        @Query("notify") notify: Boolean? = null
    ): GetCallResponse

    /**
     * Get Call Stats
     *   Required permissions: - ReadCallStats
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param session
     * @return [GetCallStatsResponse]
     */
    @GET("/video/call/{type}/{id}/stats/{session}")
    suspend fun getCallStats(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String
    ): GetCallStatsResponse

    /**
     * Get Edges
     * Returns the list of all edges available for video calls.
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @return [GetEdgesResponse]
     */
    @GET("/video/edges")
    suspend fun getEdges(
    ): GetEdgesResponse

    /**
     * Get or create a call
     * Gets or creates a new call  Sends events: - call.created - call.notification - call.ring  Required permissions: - CreateCall - ReadCall - UpdateCallSettings
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param getOrCreateCallRequest
     * @param connectionId  (optional)
     * @return [GetOrCreateCallResponse]
     */
    @POST("/video/call/{type}/{id}")
    suspend fun getOrCreateCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body getOrCreateCallRequest: GetOrCreateCallRequest,
        @Query("connection_id") connectionId: String? = null
    ): GetOrCreateCallResponse

    /**
     * Set call as live
     *   Sends events: - call.live_started  Required permissions: - UpdateCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param goLiveRequest
     * @return [GoLiveResponse]
     */
    @POST("/video/call/{type}/{id}/go_live")
    suspend fun goLive(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body goLiveRequest: GoLiveRequest
    ): GoLiveResponse

    /**
     * Join call
     * Request to join a call  Required permissions: - CreateCall - JoinCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param joinCallRequest
     * @param connectionId  (optional)
     * @return [JoinCallResponse]
     */
    @POST("/video/call/{type}/{id}/join")
    suspend fun joinCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body joinCallRequest: JoinCallRequest,
        @Query("connection_id") connectionId: String? = null
    ): JoinCallResponse

    /**
     * List devices
     * Returns all available devices
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param userId  (optional)
     * @return [ListDevicesResponse]
     */
    @GET("/video/devices")
    suspend fun listDevices(
        @Query("user_id") userId: String? = null
    ): ListDevicesResponse

    /**
     * List recordings
     * Lists recordings  Required permissions: - ListRecordings
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [ListRecordingsResponse]
     */
    @GET("/video/call/{type}/{id}/recordings")
    suspend fun listRecordings(
        @Path("type") type: String,
        @Path("id") id: String
    ): ListRecordingsResponse

    /**
     * List transcriptions
     * Lists transcriptions  Required permissions: - ListTranscriptions
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [ListTranscriptionsResponse]
     */
    @GET("/video/call/{type}/{id}/transcriptions")
    suspend fun listTranscriptions(
        @Path("type") type: String,
        @Path("id") id: String
    ): ListTranscriptionsResponse

    /**
     * Mute users
     * Mutes users in a call  Required permissions: - MuteUsers
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param muteUsersRequest
     * @return [MuteUsersResponse]
     */
    @POST("/video/call/{type}/{id}/mute_users")
    suspend fun muteUsers(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body muteUsersRequest: MuteUsersRequest
    ): MuteUsersResponse

    /**
     * Query call members
     * Query call members with filter query  Required permissions: - ReadCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param queryCallMembersRequest
     * @return [QueryCallMembersResponse]
     */
    @POST("/video/call/members")
    suspend fun queryCallMembers(
        @Body queryCallMembersRequest: QueryCallMembersRequest
    ): QueryCallMembersResponse

    /**
     * Query Call Stats
     *   Required permissions: - ReadCallStats
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param queryCallStatsRequest
     * @return [QueryCallStatsResponse]
     */
    @POST("/video/call/stats")
    suspend fun queryCallStats(
        @Body queryCallStatsRequest: QueryCallStatsRequest
    ): QueryCallStatsResponse

    /**
     * Query call
     * Query calls with filter query  Required permissions: - ReadCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param queryCallsRequest
     * @param connectionId  (optional)
     * @return [QueryCallsResponse]
     */
    @POST("/video/calls")
    suspend fun queryCalls(
        @Body queryCallsRequest: QueryCallsRequest,
        @Query("connection_id") connectionId: String? = null
    ): QueryCallsResponse

    /**
     * Reject Call
     *   Sends events: - call.rejected  Required permissions: - JoinCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param rejectCallRequest
     * @return [RejectCallResponse]
     */
    @POST("/video/call/{type}/{id}/reject")
    suspend fun rejectCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body rejectCallRequest: RejectCallRequest
    ): RejectCallResponse

    /**
     * Request permission
     * Request permission to perform an action  Sends events: - call.permission_request
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param requestPermissionRequest
     * @return [RequestPermissionResponse]
     */
    @POST("/video/call/{type}/{id}/request_permission")
    suspend fun requestPermission(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body requestPermissionRequest: RequestPermissionRequest
    ): RequestPermissionResponse

    /**
     * Send custom event
     * Sends custom event to the call  Sends events: - custom  Required permissions: - SendEvent
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param sendCallEventRequest
     * @return [SendCallEventResponse]
     */
    @POST("/video/call/{type}/{id}/event")
    suspend fun sendCallEvent(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body sendCallEventRequest: SendCallEventRequest
    ): SendCallEventResponse

    /**
     * Send reaction to the call
     * Sends reaction to the call  Sends events: - call.reaction_new  Required permissions: - CreateCallReaction
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param sendReactionRequest
     * @return [SendReactionResponse]
     */
    @POST("/video/call/{type}/{id}/reaction")
    suspend fun sendVideoReaction(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body sendReactionRequest: SendReactionRequest
    ): SendReactionResponse

    /**
     * Start HLS broadcasting
     * Starts HLS broadcasting  Required permissions: - StartBroadcasting
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StartHLSBroadcastingResponse]
     */
    @POST("/video/call/{type}/{id}/start_broadcasting")
    suspend fun startHLSBroadcasting(
        @Path("type") type: String,
        @Path("id") id: String
    ): StartHLSBroadcastingResponse

    /**
     * Start recording
     * Starts recording  Sends events: - call.recording_started  Required permissions: - StartRecording
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param startRecordingRequest
     * @return [StartRecordingResponse]
     */
    @POST("/video/call/{type}/{id}/start_recording")
    suspend fun startRecording(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body startRecordingRequest: StartRecordingRequest
    ): StartRecordingResponse

    /**
     * Start transcription
     * Starts transcription  Required permissions: - StartTranscription
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param startTranscriptionRequest
     * @return [StartTranscriptionResponse]
     */
    @POST("/video/call/{type}/{id}/start_transcription")
    suspend fun startTranscription(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body startTranscriptionRequest: StartTranscriptionRequest
    ): StartTranscriptionResponse

    /**
     * Stop HLS broadcasting
     * Stops HLS broadcasting  Required permissions: - StopBroadcasting
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StopHLSBroadcastingResponse]
     */
    @POST("/video/call/{type}/{id}/stop_broadcasting")
    suspend fun stopHLSBroadcasting(
        @Path("type") type: String,
        @Path("id") id: String
    ): StopHLSBroadcastingResponse

    /**
     * Set call as not live
     *   Sends events: - call.updated  Required permissions: - UpdateCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StopLiveResponse]
     */
    @POST("/video/call/{type}/{id}/stop_live")
    suspend fun stopLive(
        @Path("type") type: String,
        @Path("id") id: String
    ): StopLiveResponse

    /**
     * Stop recording
     * Stops recording  Sends events: - call.recording_stopped  Required permissions: - StopRecording
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StopRecordingResponse]
     */
    @POST("/video/call/{type}/{id}/stop_recording")
    suspend fun stopRecording(
        @Path("type") type: String,
        @Path("id") id: String
    ): StopRecordingResponse

    /**
     * Stop transcription
     * Stops transcription  Sends events: - call.transcription_stopped  Required permissions: - StopTranscription
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StopTranscriptionResponse]
     */
    @POST("/video/call/{type}/{id}/stop_transcription")
    suspend fun stopTranscription(
        @Path("type") type: String,
        @Path("id") id: String
    ): StopTranscriptionResponse

    /**
     * Unblocks user on a call
     * Removes the block for a user on a call. The user will be able to join the call again.  Sends events: - call.unblocked_user  Required permissions: - BlockUser
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param unblockUserRequest
     * @return [UnblockUserResponse]
     */
    @POST("/video/call/{type}/{id}/unblock")
    suspend fun unblockUser(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body unblockUserRequest: UnblockUserRequest
    ): UnblockUserResponse

    /**
     * Update Call
     *   Sends events: - call.updated  Required permissions: - UpdateCall
     * Responses:
     *  - 200: Call
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param updateCallRequest
     * @return [UpdateCallResponse]
     */
    @PATCH("/video/call/{type}/{id}")
    suspend fun updateCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body updateCallRequest: UpdateCallRequest
    ): UpdateCallResponse

    /**
     * Update Call Member
     *   Sends events: - call.member_added - call.member_removed - call.member_updated  Required permissions: - RemoveCallMember - UpdateCallMember - UpdateCallMemberRole
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param updateCallMembersRequest
     * @return [UpdateCallMembersResponse]
     */
    @POST("/video/call/{type}/{id}/members")
    suspend fun updateCallMembers(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body updateCallMembersRequest: UpdateCallMembersRequest
    ): UpdateCallMembersResponse

    /**
     * Update user permissions
     * Updates user permissions  Sends events: - call.permissions_updated  Required permissions: - UpdateCallPermissions
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param updateUserPermissionsRequest
     * @return [UpdateUserPermissionsResponse]
     */
    @POST("/video/call/{type}/{id}/user_permissions")
    suspend fun updateUserPermissions(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body updateUserPermissionsRequest: UpdateUserPermissionsRequest
    ): UpdateUserPermissionsResponse

    /**
     * Video Connect (WebSocket)
     * Establishes WebSocket connection for user to video  Sends events: - connection.ok - health.check
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @return [Unit]
     */
    @GET("/video/longpoll")
    suspend fun videoConnect(
    ): Unit

    /**
     * Pin
     * Pins a track for all users in the call.  Required permissions: - PinCallTrack
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param pinRequest
     * @return [PinResponse]
     */
    @POST("/video/call/{type}/{id}/pin")
    suspend fun videoPin(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body pinRequest: PinRequest
    ): PinResponse

    /**
     * Unpin
     * Unpins a track for all users in the call.  Required permissions: - PinCallTrack
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param unpinRequest
     * @return [UnpinResponse]
     */
    @POST("/video/call/{type}/{id}/unpin")
    suspend fun videoUnpin(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body unpinRequest: UnpinRequest
    ): UnpinResponse

    @POST("/video/call/{type}/{id}/start_closed_captions")
    suspend fun startClosedCaptions(
        @Path("type") type: String,
        @Path("id") id: String,
    ): ResponseBody

    @POST("/video/call/{type}/{id}/stop_closed_captions")
    suspend fun stopClosedCaptions(
        @Path("type") type: String,
        @Path("id") id: String,
    ): ResponseBody
}
