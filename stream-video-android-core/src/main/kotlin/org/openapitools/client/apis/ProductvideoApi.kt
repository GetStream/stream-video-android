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

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package org.openapitools.client.apis

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
        @Body queryCallMembersRequest : org.openapitools.client.models.QueryCallMembersRequest
    ): org.openapitools.client.models.QueryCallMembersResponse

    /**
     * Query Call Stats
     *
     */
    @POST("/video/call/stats")
    suspend fun queryCallStats(
        @Body queryCallStatsRequest : org.openapitools.client.models.QueryCallStatsRequest? = null
    ): org.openapitools.client.models.QueryCallStatsResponse

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
        @Query("video") video: kotlin.Boolean? = null
    ): org.openapitools.client.models.GetCallResponse

    /**
     * Update Call
     *
     */
    @PATCH("/video/call/{type}/{id}")
    suspend fun updateCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body updateCallRequest : org.openapitools.client.models.UpdateCallRequest? = null
    ): org.openapitools.client.models.UpdateCallResponse

    /**
     * Get or create a call
     * Gets or creates a new call
     */
    @POST("/video/call/{type}/{id}")
    suspend fun getOrCreateCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Query("connection_id") connectionId: kotlin.String? = null ,
        @Body getOrCreateCallRequest : org.openapitools.client.models.GetOrCreateCallRequest? = null
    ): org.openapitools.client.models.GetOrCreateCallResponse

    /**
     * Accept Call
     *
     */
    @POST("/video/call/{type}/{id}/accept")
    suspend fun acceptCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.AcceptCallResponse

    /**
     * Block user on a call
     * Block a user, preventing them from joining the call until they are unblocked.
     */
    @POST("/video/call/{type}/{id}/block")
    suspend fun blockUser(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body blockUserRequest : org.openapitools.client.models.BlockUserRequest
    ): org.openapitools.client.models.BlockUserResponse

    /**
     * Delete Call
     *
     */
    @POST("/video/call/{type}/{id}/delete")
    suspend fun deleteCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body deleteCallRequest : org.openapitools.client.models.DeleteCallRequest? = null
    ): org.openapitools.client.models.DeleteCallResponse

    /**
     * Send custom event
     * Sends custom event to the call
     */
    @POST("/video/call/{type}/{id}/event")
    suspend fun sendCallEvent(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body sendCallEventRequest : org.openapitools.client.models.SendCallEventRequest? = null
    ): org.openapitools.client.models.SendCallEventResponse

    /**
     * Collect user feedback
     *
     */
    @POST("/video/call/{type}/{id}/feedback/{session}")
    suspend fun collectUserFeedback(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String ,
        @Body collectUserFeedbackRequest : org.openapitools.client.models.CollectUserFeedbackRequest
    ): org.openapitools.client.models.CollectUserFeedbackResponse

    /**
     * Set call as live
     *
     */
    @POST("/video/call/{type}/{id}/go_live")
    suspend fun goLive(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body goLiveRequest : org.openapitools.client.models.GoLiveRequest? = null
    ): org.openapitools.client.models.GoLiveResponse

    /**
     * Join call
     * Request to join a call
     */
    @POST("/video/call/{type}/{id}/join")
    suspend fun joinCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Query("connection_id") connectionId: kotlin.String? = null ,
        @Body joinCallRequest : org.openapitools.client.models.JoinCallRequest
    ): org.openapitools.client.models.JoinCallResponse

    /**
     * End call
     *
     */
    @POST("/video/call/{type}/{id}/mark_ended")
    suspend fun endCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.EndCallResponse

    /**
     * Update Call Member
     *
     */
    @POST("/video/call/{type}/{id}/members")
    suspend fun updateCallMembers(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body updateCallMembersRequest : org.openapitools.client.models.UpdateCallMembersRequest? = null
    ): org.openapitools.client.models.UpdateCallMembersResponse

    /**
     * Mute users
     * Mutes users in a call
     */
    @POST("/video/call/{type}/{id}/mute_users")
    suspend fun muteUsers(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body muteUsersRequest : org.openapitools.client.models.MuteUsersRequest? = null
    ): org.openapitools.client.models.MuteUsersResponse

    /**
     * Pin
     * Pins a track for all users in the call.
     */
    @POST("/video/call/{type}/{id}/pin")
    suspend fun videoPin(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body pinRequest : org.openapitools.client.models.PinRequest
    ): org.openapitools.client.models.PinResponse

    /**
     * Send reaction to the call
     * Sends reaction to the call
     */
    @POST("/video/call/{type}/{id}/reaction")
    suspend fun sendVideoReaction(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body sendReactionRequest : org.openapitools.client.models.SendReactionRequest
    ): org.openapitools.client.models.SendReactionResponse

    /**
     * List recordings
     * Lists recordings
     */
    @GET("/video/call/{type}/{id}/recordings")
    suspend fun listRecordings(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.ListRecordingsResponse

    /**
     * Reject Call
     *
     */
    @POST("/video/call/{type}/{id}/reject")
    suspend fun rejectCall(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body rejectCallRequest : org.openapitools.client.models.RejectCallRequest? = null
    ): org.openapitools.client.models.RejectCallResponse

    /**
     * Request permission
     * Request permission to perform an action
     */
    @POST("/video/call/{type}/{id}/request_permission")
    suspend fun requestPermission(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body requestPermissionRequest : org.openapitools.client.models.RequestPermissionRequest
    ): org.openapitools.client.models.RequestPermissionResponse

    /**
     * Start RTMP broadcasts
     * Starts RTMP broadcasts for the provided RTMP destinations
     */
    @POST("/video/call/{type}/{id}/rtmp_broadcasts")
    suspend fun startRTMPBroadcasts(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body startRTMPBroadcastsRequest : org.openapitools.client.models.StartRTMPBroadcastsRequest
    ): org.openapitools.client.models.StartRTMPBroadcastsResponse

    /**
     * Stop all RTMP broadcasts for a call
     * Stop all RTMP broadcasts for the provided call
     */
    @POST("/video/call/{type}/{id}/rtmp_broadcasts/stop")
    suspend fun stopAllRTMPBroadcasts(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.StopAllRTMPBroadcastsResponse

    /**
     * Start HLS broadcasting
     * Starts HLS broadcasting
     */
    @POST("/video/call/{type}/{id}/start_broadcasting")
    suspend fun startHLSBroadcasting(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.StartHLSBroadcastingResponse

    /**
     * Start closed captions
     * Starts closed captions
     */
    @POST("/video/call/{type}/{id}/start_closed_captions")
    suspend fun startClosedCaptions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body startClosedCaptionsRequest : org.openapitools.client.models.StartClosedCaptionsRequest? = null
    ): org.openapitools.client.models.StartClosedCaptionsResponse

    /**
     * Start recording
     * Starts recording
     */
    @POST("/video/call/{type}/{id}/start_recording")
    suspend fun startRecording(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body startRecordingRequest : org.openapitools.client.models.StartRecordingRequest? = null
    ): org.openapitools.client.models.StartRecordingResponse

    /**
     * Start transcription
     * Starts transcription
     */
    @POST("/video/call/{type}/{id}/start_transcription")
    suspend fun startTranscription(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body startTranscriptionRequest : org.openapitools.client.models.StartTranscriptionRequest? = null
    ): org.openapitools.client.models.StartTranscriptionResponse

    /**
     * Get Call Stats
     *
     */
    @GET("/video/call/{type}/{id}/stats/{session}")
    suspend fun getCallStats(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String
    ): org.openapitools.client.models.GetCallStatsResponse

    /**
     * Stop HLS broadcasting
     * Stops HLS broadcasting
     */
    @POST("/video/call/{type}/{id}/stop_broadcasting")
    suspend fun stopHLSBroadcasting(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.StopHLSBroadcastingResponse

    /**
     * Stop closed captions
     * Stops closed captions
     */
    @POST("/video/call/{type}/{id}/stop_closed_captions")
    suspend fun stopClosedCaptions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body stopClosedCaptionsRequest : org.openapitools.client.models.StopClosedCaptionsRequest? = null
    ): org.openapitools.client.models.StopClosedCaptionsResponse

    /**
     * Set call as not live
     *
     */
    @POST("/video/call/{type}/{id}/stop_live")
    suspend fun stopLive(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body stopLiveRequest : org.openapitools.client.models.StopLiveRequest? = null
    ): org.openapitools.client.models.StopLiveResponse

    /**
     * Stop recording
     * Stops recording
     */
    @POST("/video/call/{type}/{id}/stop_recording")
    suspend fun stopRecording(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.StopRecordingResponse

    /**
     * Stop transcription
     * Stops transcription
     */
    @POST("/video/call/{type}/{id}/stop_transcription")
    suspend fun stopTranscription(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body stopTranscriptionRequest : org.openapitools.client.models.StopTranscriptionRequest? = null
    ): org.openapitools.client.models.StopTranscriptionResponse

    /**
     * List transcriptions
     * Lists transcriptions
     */
    @GET("/video/call/{type}/{id}/transcriptions")
    suspend fun listTranscriptions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String
    ): org.openapitools.client.models.ListTranscriptionsResponse

    /**
     * Unblocks user on a call
     * Removes the block for a user on a call. The user will be able to join the call again.
     */
    @POST("/video/call/{type}/{id}/unblock")
    suspend fun unblockUser(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body unblockUserRequest : org.openapitools.client.models.UnblockUserRequest
    ): org.openapitools.client.models.UnblockUserResponse

    /**
     * Unpin
     * Unpins a track for all users in the call.
     */
    @POST("/video/call/{type}/{id}/unpin")
    suspend fun videoUnpin(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body unpinRequest : org.openapitools.client.models.UnpinRequest
    ): org.openapitools.client.models.UnpinResponse

    /**
     * Update user permissions
     * Updates user permissions
     */
    @POST("/video/call/{type}/{id}/user_permissions")
    suspend fun updateUserPermissions(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String ,
        @Body updateUserPermissionsRequest : org.openapitools.client.models.UpdateUserPermissionsRequest
    ): org.openapitools.client.models.UpdateUserPermissionsResponse

    /**
     * Delete recording
     * Deletes recording
     */
    @DELETE("/video/call/{type}/{id}/{session}/recordings/{filename}")
    suspend fun deleteRecording(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String,
        @Path("filename") filename: kotlin.String
    ): org.openapitools.client.models.DeleteRecordingResponse

    /**
     * Delete transcription
     * Deletes transcription
     */
    @DELETE("/video/call/{type}/{id}/{session}/transcriptions/{filename}")
    suspend fun deleteTranscription(
        @Path("type") type: kotlin.String,
        @Path("id") id: kotlin.String,
        @Path("session") session: kotlin.String,
        @Path("filename") filename: kotlin.String
    ): org.openapitools.client.models.DeleteTranscriptionResponse

    /**
     * Query call
     * Query calls with filter query
     */
    @POST("/video/calls")
    suspend fun queryCalls(
        @Query("connection_id") connectionId: kotlin.String? = null ,
        @Body queryCallsRequest : org.openapitools.client.models.QueryCallsRequest? = null
    ): org.openapitools.client.models.QueryCallsResponse

    /**
     * Delete device
     * Deletes one device
     */
    @DELETE("/video/devices")
    suspend fun deleteDevice(
        @Query("id") id: kotlin.String
    ): org.openapitools.client.models.Response

    /**
     * List devices
     * Returns all available devices
     */
    @GET("/video/devices")
    suspend fun listDevices(
    ): org.openapitools.client.models.ListDevicesResponse

    /**
     * Create device
     * Adds a new device to a user, if the same device already exists the call will have no effect
     */
    @POST("/video/devices")
    suspend fun createDevice(
        @Body createDeviceRequest : org.openapitools.client.models.CreateDeviceRequest
    ): org.openapitools.client.models.Response

    /**
     * Get Edges
     * Returns the list of all edges available for video calls.
     */
    @GET("/video/edges")
    suspend fun getEdges(
    ): org.openapitools.client.models.GetEdgesResponse

    /**
     * Create Guest
     *
     */
    @POST("/video/guest")
    suspend fun createGuest(
        @Body createGuestRequest : org.openapitools.client.models.CreateGuestRequest
    ): org.openapitools.client.models.CreateGuestResponse

    /**
     * Video Connect (WebSocket)
     * Establishes WebSocket connection for user to video
     */
    @GET("/video/longpoll")
    suspend fun videoConnect(
    )

    /**
     * Query Aggregate call Stats
     *
     */
    @POST("/video/stats")
    suspend fun queryAggregateCallStats(
        @Body queryAggregateCallStatsRequest : org.openapitools.client.models.QueryAggregateCallStatsRequest? = null
    ): org.openapitools.client.models.QueryAggregateCallStatsResponse

}
