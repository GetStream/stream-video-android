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

package io.getstream.android.video.generated.apis

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductvideoApi {

    /**
     * Query call members
     * Query call members with filter query
     */
    @POST("/video/call/members")
    suspend fun queryCallMembers(
        @Body queryCallMembersRequest:
        io.getstream.android.video.generated.models.QueryCallMembersRequest,
    ): io.getstream.android.video.generated.models.QueryCallMembersResponse

    /**
     * Query Call Stats
     *
     */
    @POST("/video/call/stats")
    suspend fun queryCallStats(
        @Body queryCallStatsRequest:
        io.getstream.android.video.generated.models.QueryCallStatsRequest,
    ): io.getstream.android.video.generated.models.QueryCallStatsResponse

    /**
     * Get Call
     *
     */
    @GET("/video/call/{type}/{id}")
    suspend fun getCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Query("connection_id") connectionId: kotlin.String? = null,
        @Query("members_limit") membersLimit: kotlin.Int? = null,
        @Query("ring") ring: kotlin.Boolean? = null,
        @Query("notify") notify: kotlin.Boolean? = null,
        @Query("video") video: kotlin.Boolean? = null,
    ): io.getstream.android.video.generated.models.GetCallResponse

    /**
     * Update Call
     *
     */
    @PATCH("/video/call/{type}/{id}")
    suspend fun updateCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body updateCallRequest: io.getstream.android.video.generated.models.UpdateCallRequest,
    ): io.getstream.android.video.generated.models.UpdateCallResponse

    /**
     * Get or create a call
     * Gets or creates a new call
     */
    @POST("/video/call/{type}/{id}")
    suspend fun getOrCreateCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Query("connection_id") connectionId: kotlin.String? = null,
        @Body getOrCreateCallRequest:
        io.getstream.android.video.generated.models.GetOrCreateCallRequest,
    ): io.getstream.android.video.generated.models.GetOrCreateCallResponse

    /**
     * Accept Call
     *
     */
    @POST("/video/call/{type}/{id}/accept")
    suspend fun acceptCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.AcceptCallResponse

    /**
     * Block user on a call
     * Block a user, preventing them from joining the call until they are unblocked.
     */
    @POST("/video/call/{type}/{id}/block")
    suspend fun blockUser(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body blockUserRequest: io.getstream.android.video.generated.models.BlockUserRequest,
    ): io.getstream.android.video.generated.models.BlockUserResponse

    /**
     * Delete Call
     *
     */
    @POST("/video/call/{type}/{id}/delete")
    suspend fun deleteCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body deleteCallRequest: io.getstream.android.video.generated.models.DeleteCallRequest,
    ): io.getstream.android.video.generated.models.DeleteCallResponse

    /**
     * Send custom event
     * Sends custom event to the call
     */
    @POST("/video/call/{type}/{id}/event")
    suspend fun sendCallEvent(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body sendCallEventRequest:
        io.getstream.android.video.generated.models.SendCallEventRequest,
    ): io.getstream.android.video.generated.models.SendCallEventResponse

    /**
     * Collect user feedback
     *
     */
    @POST("/video/call/{type}/{id}/feedback/{session}")
    suspend fun collectUserFeedback(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String,
        @Body collectUserFeedbackRequest:
        io.getstream.android.video.generated.models.CollectUserFeedbackRequest,
    ): io.getstream.android.video.generated.models.CollectUserFeedbackResponse

    /**
     * Set call as live
     *
     */
    @POST("/video/call/{type}/{id}/go_live")
    suspend fun goLive(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body goLiveRequest: io.getstream.android.video.generated.models.GoLiveRequest,
    ): io.getstream.android.video.generated.models.GoLiveResponse

    /**
     * Join call
     * Request to join a call
     */
    @POST("/video/call/{type}/{id}/join")
    suspend fun joinCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Query("connection_id") connectionId: kotlin.String? = null,
        @Body joinCallRequest: io.getstream.android.video.generated.models.JoinCallRequest,
    ): io.getstream.android.video.generated.models.JoinCallResponse

    /**
     * End call
     *
     */
    @POST("/video/call/{type}/{id}/mark_ended")
    suspend fun endCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.EndCallResponse

    /**
     * Update Call Member
     *
     */
    @POST("/video/call/{type}/{id}/members")
    suspend fun updateCallMembers(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body updateCallMembersRequest:
        io.getstream.android.video.generated.models.UpdateCallMembersRequest,
    ): io.getstream.android.video.generated.models.UpdateCallMembersResponse

    /**
     * Mute users
     * Mutes users in a call
     */
    @POST("/video/call/{type}/{id}/mute_users")
    suspend fun muteUsers(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body muteUsersRequest: io.getstream.android.video.generated.models.MuteUsersRequest,
    ): io.getstream.android.video.generated.models.MuteUsersResponse

    /**
     * Pin
     * Pins a track for all users in the call.
     */
    @POST("/video/call/{type}/{id}/pin")
    suspend fun videoPin(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body pinRequest: io.getstream.android.video.generated.models.PinRequest,
    ): io.getstream.android.video.generated.models.PinResponse

    /**
     * Send reaction to the call
     * Sends reaction to the call
     */
    @POST("/video/call/{type}/{id}/reaction")
    suspend fun sendVideoReaction(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body sendReactionRequest: io.getstream.android.video.generated.models.SendReactionRequest,
    ): io.getstream.android.video.generated.models.SendReactionResponse

    /**
     * List recordings
     * Lists recordings
     */
    @GET("/video/call/{type}/{id}/recordings")
    suspend fun listRecordings(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.ListRecordingsResponse

    /**
     * Reject Call
     *
     */
    @POST("/video/call/{type}/{id}/reject")
    suspend fun rejectCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body rejectCallRequest: io.getstream.android.video.generated.models.RejectCallRequest,
    ): io.getstream.android.video.generated.models.RejectCallResponse

    /**
     * Request permission
     * Request permission to perform an action
     */
    @POST("/video/call/{type}/{id}/request_permission")
    suspend fun requestPermission(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body requestPermissionRequest:
        io.getstream.android.video.generated.models.RequestPermissionRequest,
    ): io.getstream.android.video.generated.models.RequestPermissionResponse

    /**
     * Start RTMP broadcasts
     * Starts RTMP broadcasts for the provided RTMP destinations
     */
    @POST("/video/call/{type}/{id}/rtmp_broadcasts")
    suspend fun startRTMPBroadcasts(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body startRTMPBroadcastsRequest:
        io.getstream.android.video.generated.models.StartRTMPBroadcastsRequest,
    ): io.getstream.android.video.generated.models.StartRTMPBroadcastsResponse

    /**
     * Stop all RTMP broadcasts for a call
     * Stop all RTMP broadcasts for the provided call
     */
    @POST("/video/call/{type}/{id}/rtmp_broadcasts/stop")
    suspend fun stopAllRTMPBroadcasts(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.StopAllRTMPBroadcastsResponse

    /**
     * Start HLS broadcasting
     * Starts HLS broadcasting
     */
    @POST("/video/call/{type}/{id}/start_broadcasting")
    suspend fun startHLSBroadcasting(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.StartHLSBroadcastingResponse

    /**
     * Start closed captions
     * Starts closed captions
     */
    @POST("/video/call/{type}/{id}/start_closed_captions")
    suspend fun startClosedCaptions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body startClosedCaptionsRequest:
        io.getstream.android.video.generated.models.StartClosedCaptionsRequest,
    ): io.getstream.android.video.generated.models.StartClosedCaptionsResponse

    /**
     * Start recording
     * Starts recording
     */
    @POST("/video/call/{type}/{id}/start_recording")
    suspend fun startRecording(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body startRecordingRequest:
        io.getstream.android.video.generated.models.StartRecordingRequest,
    ): io.getstream.android.video.generated.models.StartRecordingResponse

    /**
     * Start transcription
     * Starts transcription
     */
    @POST("/video/call/{type}/{id}/start_transcription")
    suspend fun startTranscription(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body startTranscriptionRequest:
        io.getstream.android.video.generated.models.StartTranscriptionRequest,
    ): io.getstream.android.video.generated.models.StartTranscriptionResponse

    /**
     * Get Call Stats
     *
     */
    @GET("/video/call/{type}/{id}/stats/{session}")
    suspend fun getCallStats(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String,
    ): io.getstream.android.video.generated.models.GetCallStatsResponse

    /**
     * Stop HLS broadcasting
     * Stops HLS broadcasting
     */
    @POST("/video/call/{type}/{id}/stop_broadcasting")
    suspend fun stopHLSBroadcasting(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.StopHLSBroadcastingResponse

    /**
     * Stop closed captions
     * Stops closed captions
     */
    @POST("/video/call/{type}/{id}/stop_closed_captions")
    suspend fun stopClosedCaptions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body stopClosedCaptionsRequest:
        io.getstream.android.video.generated.models.StopClosedCaptionsRequest,
    ): io.getstream.android.video.generated.models.StopClosedCaptionsResponse

    /**
     * Set call as not live
     *
     */
    @POST("/video/call/{type}/{id}/stop_live")
    suspend fun stopLive(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body stopLiveRequest: io.getstream.android.video.generated.models.StopLiveRequest,
    ): io.getstream.android.video.generated.models.StopLiveResponse

    /**
     * Stop recording
     * Stops recording
     */
    @POST("/video/call/{type}/{id}/stop_recording")
    suspend fun stopRecording(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.StopRecordingResponse

    /**
     * Stop transcription
     * Stops transcription
     */
    @POST("/video/call/{type}/{id}/stop_transcription")
    suspend fun stopTranscription(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body stopTranscriptionRequest:
        io.getstream.android.video.generated.models.StopTranscriptionRequest,
    ): io.getstream.android.video.generated.models.StopTranscriptionResponse

    /**
     * List transcriptions
     * Lists transcriptions
     */
    @GET("/video/call/{type}/{id}/transcriptions")
    suspend fun listTranscriptions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.ListTranscriptionsResponse

    /**
     * Unblocks user on a call
     * Removes the block for a user on a call. The user will be able to join the call again.
     */
    @POST("/video/call/{type}/{id}/unblock")
    suspend fun unblockUser(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body unblockUserRequest: io.getstream.android.video.generated.models.UnblockUserRequest,
    ): io.getstream.android.video.generated.models.UnblockUserResponse

    /**
     * Unpin
     * Unpins a track for all users in the call.
     */
    @POST("/video/call/{type}/{id}/unpin")
    suspend fun videoUnpin(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body unpinRequest: io.getstream.android.video.generated.models.UnpinRequest,
    ): io.getstream.android.video.generated.models.UnpinResponse

    /**
     * Update user permissions
     * Updates user permissions
     */
    @POST("/video/call/{type}/{id}/user_permissions")
    suspend fun updateUserPermissions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Body updateUserPermissionsRequest:
        io.getstream.android.video.generated.models.UpdateUserPermissionsRequest,
    ): io.getstream.android.video.generated.models.UpdateUserPermissionsResponse

    /**
     * Delete recording
     * Deletes recording
     */
    @DELETE("/video/call/{type}/{id}/{session}/recordings/{filename}")
    suspend fun deleteRecording(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String,
        @Path("filename") filename: kotlin.String,
    ): io.getstream.android.video.generated.models.DeleteRecordingResponse

    /**
     * Delete transcription
     * Deletes transcription
     */
    @DELETE("/video/call/{type}/{id}/{session}/transcriptions/{filename}")
    suspend fun deleteTranscription(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String,
        @Path("filename") filename: kotlin.String,
    ): io.getstream.android.video.generated.models.DeleteTranscriptionResponse

    /**
     * Query call
     * Query calls with filter query
     */
    @POST("/video/calls")
    suspend fun queryCalls(
        @Query("connection_id") connectionId: kotlin.String? = null,
        @Body queryCallsRequest: io.getstream.android.video.generated.models.QueryCallsRequest,
    ): io.getstream.android.video.generated.models.QueryCallsResponse

    /**
     * Delete device
     * Deletes one device
     */
    @DELETE("/video/devices")
    suspend fun deleteDevice(
        @Query("id") id: kotlin.String,
    ): io.getstream.android.video.generated.models.Response

    /**
     * List devices
     * Returns all available devices
     */
    @GET("/video/devices")
    suspend fun listDevices(): io.getstream.android.video.generated.models.ListDevicesResponse

    /**
     * Create device
     * Adds a new device to a user, if the same device already exists the call will have no effect
     */
    @POST("/video/devices")
    suspend fun createDevice(
        @Body createDeviceRequest: io.getstream.android.video.generated.models.CreateDeviceRequest,
    ): io.getstream.android.video.generated.models.Response

    /**
     * Get Edges
     * Returns the list of all edges available for video calls.
     */
    @GET("/video/edges")
    suspend fun getEdges(): io.getstream.android.video.generated.models.GetEdgesResponse

    /**
     * Create Guest
     *
     */
    @POST("/video/guest")
    suspend fun createGuest(
        @Body createGuestRequest: io.getstream.android.video.generated.models.CreateGuestRequest,
    ): io.getstream.android.video.generated.models.CreateGuestResponse

    /**
     * Video Connect (WebSocket)
     * Establishes WebSocket connection for user to video
     */
    @GET("/video/longpoll")
    suspend fun videoConnect()

    /**
     * Query Aggregate call Stats
     *
     */
    @POST("/video/stats")
    suspend fun queryAggregateCallStats(
        @Body queryAggregateCallStatsRequest:
        io.getstream.android.video.generated.models.QueryAggregateCallStatsRequest,
    ): io.getstream.android.video.generated.models.QueryAggregateCallStatsResponse
}
