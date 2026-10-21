
/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.android.video.internal.generated.apis

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT

public interface ProductvideoApi {
    
    /**
     * Query call members
     * Query call members with filter query
     */
    @POST("/video/call/members")
    public suspend fun queryCallMembers(
        @Body queryCallMembersRequest: io.getstream.android.video.api.generated.models.QueryCallMembersRequest
    ): io.getstream.android.video.api.generated.models.QueryCallMembersResponse
    
    /**
     * Query Call Stats
     * 
     */
    @POST("/video/call/stats")
    public suspend fun queryCallStats(
        @Body queryCallStatsRequest: io.getstream.android.video.api.generated.models.QueryCallStatsRequest
    ): io.getstream.android.video.api.generated.models.QueryCallStatsResponse
    
    /**
     * Query Call Stats
     * 
     */
    @POST("/video/call/stats")
    public suspend fun queryCallStats(
    ): io.getstream.android.video.api.generated.models.QueryCallStatsResponse
    
    /**
     * Get Call
     * 
     */
    @GET("/video/call/{type}/{id}")
    public suspend fun getCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("connection_id") connectionId: String? = null,
        @Query("members_limit") membersLimit: Int? = null,
        @Query("ring") ring: Boolean? = null,
        @Query("notify") notify: Boolean? = null,
        @Query("video") video: Boolean? = null
    ): io.getstream.android.video.api.generated.models.GetCallResponse
    
    /**
     * Update Call
     * 
     */
    @PATCH("/video/call/{type}/{id}")
    public suspend fun updateCall(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body updateCallRequest: io.getstream.android.video.api.generated.models.UpdateCallRequest
    ): io.getstream.android.video.api.generated.models.UpdateCallResponse
    
    /**
     * Update Call
     * 
     */
    @PATCH("/video/call/{type}/{id}")
    public suspend fun updateCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.UpdateCallResponse
    
    /**
     * Get or create a call
     * Gets or creates a new call
     */
    @POST("/video/call/{type}/{id}")
    public suspend fun getOrCreateCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("connection_id") connectionId: String? = null ,
        @Body getOrCreateCallRequest: io.getstream.android.video.api.generated.models.GetOrCreateCallRequest
    ): io.getstream.android.video.api.generated.models.GetOrCreateCallResponse
    
    /**
     * Get or create a call
     * Gets or creates a new call
     */
    @POST("/video/call/{type}/{id}")
    public suspend fun getOrCreateCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("connection_id") connectionId: String? = null
    ): io.getstream.android.video.api.generated.models.GetOrCreateCallResponse
    
    /**
     * Accept Call
     * 
     */
    @POST("/video/call/{type}/{id}/accept")
    public suspend fun acceptCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.AcceptCallResponse
    
    /**
     * Block user on a call
     * Block a user, preventing them from joining the call until they are unblocked.
     */
    @POST("/video/call/{type}/{id}/block")
    public suspend fun blockUser(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body blockUserRequest: io.getstream.android.video.api.generated.models.BlockUserRequest
    ): io.getstream.android.video.api.generated.models.BlockUserResponse
    
    /**
     * Delete Call
     * 
     */
    @POST("/video/call/{type}/{id}/delete")
    public suspend fun deleteCall(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body deleteCallRequest: io.getstream.android.video.api.generated.models.DeleteCallRequest
    ): io.getstream.android.video.api.generated.models.DeleteCallResponse
    
    /**
     * Delete Call
     * 
     */
    @POST("/video/call/{type}/{id}/delete")
    public suspend fun deleteCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.DeleteCallResponse
    
    /**
     * Send custom event
     * Sends custom event to the call
     */
    @POST("/video/call/{type}/{id}/event")
    public suspend fun sendCallEvent(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body sendCallEventRequest: io.getstream.android.video.api.generated.models.SendCallEventRequest
    ): io.getstream.android.video.api.generated.models.SendCallEventResponse
    
    /**
     * Send custom event
     * Sends custom event to the call
     */
    @POST("/video/call/{type}/{id}/event")
    public suspend fun sendCallEvent(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.SendCallEventResponse
    
    /**
     * Collect user feedback
     * 
     */
    @POST("/video/call/{type}/{id}/feedback")
    public suspend fun collectUserFeedback(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body collectUserFeedbackRequest: io.getstream.android.video.api.generated.models.CollectUserFeedbackRequest
    ): io.getstream.android.video.api.generated.models.CollectUserFeedbackResponse
    
    /**
     * Set call as live
     * 
     */
    @POST("/video/call/{type}/{id}/go_live")
    public suspend fun goLive(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body goLiveRequest: io.getstream.android.video.api.generated.models.GoLiveRequest
    ): io.getstream.android.video.api.generated.models.GoLiveResponse
    
    /**
     * Set call as live
     * 
     */
    @POST("/video/call/{type}/{id}/go_live")
    public suspend fun goLive(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.GoLiveResponse
    
    /**
     * Join call
     * Request to join a call
     */
    @POST("/video/call/{type}/{id}/join")
    public suspend fun joinCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("connection_id") connectionId: String? = null ,
        @Body joinCallRequest: io.getstream.android.video.api.generated.models.JoinCallRequest
    ): io.getstream.android.video.api.generated.models.JoinCallResponse
    
    /**
     * Kick user from a call
     * Kicks a user from the call. Optionally block the user from rejoining by setting block=true.
     */
    @POST("/video/call/{type}/{id}/kick")
    public suspend fun kickUser(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body kickUserRequest: io.getstream.android.video.api.generated.models.KickUserRequest
    ): io.getstream.android.video.api.generated.models.KickUserResponse
    
    /**
     * End call
     * 
     */
    @POST("/video/call/{type}/{id}/mark_ended")
    public suspend fun endCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.EndCallResponse
    
    /**
     * Update Call Member
     * 
     */
    @POST("/video/call/{type}/{id}/members")
    public suspend fun updateCallMembers(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body updateCallMembersRequest: io.getstream.android.video.api.generated.models.UpdateCallMembersRequest
    ): io.getstream.android.video.api.generated.models.UpdateCallMembersResponse
    
    /**
     * Update Call Member
     * 
     */
    @POST("/video/call/{type}/{id}/members")
    public suspend fun updateCallMembers(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.UpdateCallMembersResponse
    
    /**
     * Mute users
     * Mutes users in a call
     */
    @POST("/video/call/{type}/{id}/mute_users")
    public suspend fun muteUsers(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body muteUsersRequest: io.getstream.android.video.api.generated.models.MuteUsersRequest
    ): io.getstream.android.video.api.generated.models.MuteUsersResponse
    
    /**
     * Mute users
     * Mutes users in a call
     */
    @POST("/video/call/{type}/{id}/mute_users")
    public suspend fun muteUsers(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.MuteUsersResponse
    
    /**
     * Query call participants
     * Returns a list of participants connected to the call
     */
    @POST("/video/call/{type}/{id}/participants")
    public suspend fun queryCallParticipants(
        @Path("id") id: String,
        @Path("type") type: String,
        @Query("limit") limit: Int? = null ,
        @Body queryCallParticipantsRequest: io.getstream.android.video.api.generated.models.QueryCallParticipantsRequest
    ): io.getstream.android.video.api.generated.models.QueryCallParticipantsResponse
    
    /**
     * Query call participants
     * Returns a list of participants connected to the call
     */
    @POST("/video/call/{type}/{id}/participants")
    public suspend fun queryCallParticipants(
        @Path("id") id: String,
        @Path("type") type: String,
        @Query("limit") limit: Int? = null
    ): io.getstream.android.video.api.generated.models.QueryCallParticipantsResponse
    
    /**
     * Pin
     * Pins a track for all users in the call.
     */
    @POST("/video/call/{type}/{id}/pin")
    public suspend fun videoPin(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body pinRequest: io.getstream.android.video.api.generated.models.PinRequest
    ): io.getstream.android.video.api.generated.models.PinResponse
    
    /**
     * Send reaction to the call
     * Sends reaction to the call
     */
    @POST("/video/call/{type}/{id}/reaction")
    public suspend fun sendVideoReaction(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body sendVideoReactionRequest: io.getstream.android.video.api.generated.models.SendVideoReactionRequest
    ): io.getstream.android.video.api.generated.models.SendVideoReactionResponse
    
    /**
     * List recordings
     * Lists recordings
     */
    @GET("/video/call/{type}/{id}/recordings")
    public suspend fun listRecordings(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.ListRecordingsResponse
    
    /**
     * Start recording
     * Starts recording
     */
    @POST("/video/call/{type}/{id}/recordings/{recording_type}/start")
    public suspend fun startRecording(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("recording_type") recordingType: String ,
        @Body startRecordingRequest: io.getstream.android.video.api.generated.models.StartRecordingRequest
    ): io.getstream.android.video.api.generated.models.StartRecordingResponse
    
    /**
     * Start recording
     * Starts recording
     */
    @POST("/video/call/{type}/{id}/recordings/{recording_type}/start")
    public suspend fun startRecording(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("recording_type") recordingType: String
    ): io.getstream.android.video.api.generated.models.StartRecordingResponse
    
    /**
     * Stop recording
     * Stops recording
     */
    @POST("/video/call/{type}/{id}/recordings/{recording_type}/stop")
    public suspend fun stopRecording(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("recording_type") recordingType: String
    ): io.getstream.android.video.api.generated.models.StopRecordingResponse
    
    /**
     * Reject Call
     * 
     */
    @POST("/video/call/{type}/{id}/reject")
    public suspend fun rejectCall(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body rejectCallRequest: io.getstream.android.video.api.generated.models.RejectCallRequest
    ): io.getstream.android.video.api.generated.models.RejectCallResponse
    
    /**
     * Reject Call
     * 
     */
    @POST("/video/call/{type}/{id}/reject")
    public suspend fun rejectCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.RejectCallResponse
    
    /**
     * Get call report
     * 
     */
    @GET("/video/call/{type}/{id}/report")
    public suspend fun getCallReport(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("session_id") sessionId: String? = null
    ): io.getstream.android.video.api.generated.models.GetCallReportResponse
    
    /**
     * Request permission
     * Request permission to perform an action
     */
    @POST("/video/call/{type}/{id}/request_permission")
    public suspend fun requestPermission(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body requestPermissionRequest: io.getstream.android.video.api.generated.models.RequestPermissionRequest
    ): io.getstream.android.video.api.generated.models.RequestPermissionResponse
    
    /**
     * Ring Call Users
     * Sends a ring notification to the provided users who are not already in the call. All users should be members of the call
     */
    @POST("/video/call/{type}/{id}/ring")
    public suspend fun ringCall(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body ringCallRequest: io.getstream.android.video.api.generated.models.RingCallRequest
    ): io.getstream.android.video.api.generated.models.RingCallResponse
    
    /**
     * Ring Call Users
     * Sends a ring notification to the provided users who are not already in the call. All users should be members of the call
     */
    @POST("/video/call/{type}/{id}/ring")
    public suspend fun ringCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.RingCallResponse
    
    /**
     * Get Call Ring State
     * Returns who accepted, rejected, or missed the ring for a call session. Safe to poll: it performs no writes and emits no events. Use it to reconcile after a dropped call.accepted, call.rejected, or call.missed websocket event. call_session_id identifies the session, so the state stays readable after the call has ended.
     */
    @GET("/video/call/{type}/{id}/ring_state")
    public suspend fun getCallRingState(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("call_session_id") callSessionId: String
    ): io.getstream.android.video.api.generated.models.GetCallRingStateResponse
    
    /**
     * Start RTMP broadcasts
     * Starts RTMP broadcasts for the provided RTMP destinations
     */
    @POST("/video/call/{type}/{id}/rtmp_broadcasts")
    public suspend fun startRTMPBroadcasts(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body startRTMPBroadcastsRequest: io.getstream.android.video.api.generated.models.StartRTMPBroadcastsRequest
    ): io.getstream.android.video.api.generated.models.StartRTMPBroadcastsResponse
    
    /**
     * Stop all RTMP broadcasts for a call
     * Stop all RTMP broadcasts for the provided call
     */
    @POST("/video/call/{type}/{id}/rtmp_broadcasts/stop")
    public suspend fun stopAllRTMPBroadcasts(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StopAllRTMPBroadcastsResponse
    
    /**
     * Get call participant session metrics
     * 
     */
    @GET("/video/call/{type}/{id}/session/{session}/participant/{user}/{user_session}/details/track")
    public suspend fun getCallParticipantSessionMetrics(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
        @Path("user") user: String,
        @Path("user_session") userSession: String,
        @Query("since") since: kotlin.time.Instant? = null,
        @Query("until") until: kotlin.time.Instant? = null
    ): io.getstream.android.video.api.generated.models.GetCallParticipantSessionMetricsResponse
    
    /**
     * Query call participant sessions
     * 
     */
    @GET("/video/call/{type}/{id}/session/{session}/participant_sessions")
    public suspend fun queryCallParticipantSessions(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
        @Query("limit") limit: Int? = null,
        @Query("prev") prev: String? = null,
        @Query("next") next: String? = null,
        @Query("filter_conditions") filterConditions: Map<String, Any?>? = null
    ): io.getstream.android.video.api.generated.models.QueryCallParticipantSessionsResponse
    
    /**
     * Start HLS broadcasting
     * Starts HLS broadcasting
     */
    @POST("/video/call/{type}/{id}/start_broadcasting")
    public suspend fun startHLSBroadcasting(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StartHLSBroadcastingResponse
    
    /**
     * Start closed captions
     * Starts closed captions
     */
    @POST("/video/call/{type}/{id}/start_closed_captions")
    public suspend fun startClosedCaptions(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body startClosedCaptionsRequest: io.getstream.android.video.api.generated.models.StartClosedCaptionsRequest
    ): io.getstream.android.video.api.generated.models.StartClosedCaptionsResponse
    
    /**
     * Start closed captions
     * Starts closed captions
     */
    @POST("/video/call/{type}/{id}/start_closed_captions")
    public suspend fun startClosedCaptions(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StartClosedCaptionsResponse
    
    /**
     * Start frame recording
     * Starts frame by frame recording
     */
    @POST("/video/call/{type}/{id}/start_frame_recording")
    public suspend fun startFrameRecording(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body startFrameRecordingRequest: io.getstream.android.video.api.generated.models.StartFrameRecordingRequest
    ): io.getstream.android.video.api.generated.models.StartFrameRecordingResponse
    
    /**
     * Start frame recording
     * Starts frame by frame recording
     */
    @POST("/video/call/{type}/{id}/start_frame_recording")
    public suspend fun startFrameRecording(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StartFrameRecordingResponse
    
    /**
     * Start transcription
     * Starts transcription
     */
    @POST("/video/call/{type}/{id}/start_transcription")
    public suspend fun startTranscription(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body startTranscriptionRequest: io.getstream.android.video.api.generated.models.StartTranscriptionRequest
    ): io.getstream.android.video.api.generated.models.StartTranscriptionResponse
    
    /**
     * Start transcription
     * Starts transcription
     */
    @POST("/video/call/{type}/{id}/start_transcription")
    public suspend fun startTranscription(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StartTranscriptionResponse
    
    /**
     * Stop HLS broadcasting
     * Stops HLS broadcasting
     */
    @POST("/video/call/{type}/{id}/stop_broadcasting")
    public suspend fun stopHLSBroadcasting(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StopHLSBroadcastingResponse
    
    /**
     * Stop closed captions
     * Stops closed captions
     */
    @POST("/video/call/{type}/{id}/stop_closed_captions")
    public suspend fun stopClosedCaptions(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body stopClosedCaptionsRequest: io.getstream.android.video.api.generated.models.StopClosedCaptionsRequest
    ): io.getstream.android.video.api.generated.models.StopClosedCaptionsResponse
    
    /**
     * Stop closed captions
     * Stops closed captions
     */
    @POST("/video/call/{type}/{id}/stop_closed_captions")
    public suspend fun stopClosedCaptions(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StopClosedCaptionsResponse
    
    /**
     * Stop frame recording
     * Stops frame recording
     */
    @POST("/video/call/{type}/{id}/stop_frame_recording")
    public suspend fun stopFrameRecording(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StopFrameRecordingResponse
    
    /**
     * Set call as not live
     * 
     */
    @POST("/video/call/{type}/{id}/stop_live")
    public suspend fun stopLive(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body stopLiveRequest: io.getstream.android.video.api.generated.models.StopLiveRequest
    ): io.getstream.android.video.api.generated.models.StopLiveResponse
    
    /**
     * Set call as not live
     * 
     */
    @POST("/video/call/{type}/{id}/stop_live")
    public suspend fun stopLive(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StopLiveResponse
    
    /**
     * Stop transcription
     * Stops transcription
     */
    @POST("/video/call/{type}/{id}/stop_transcription")
    public suspend fun stopTranscription(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body stopTranscriptionRequest: io.getstream.android.video.api.generated.models.StopTranscriptionRequest
    ): io.getstream.android.video.api.generated.models.StopTranscriptionResponse
    
    /**
     * Stop transcription
     * Stops transcription
     */
    @POST("/video/call/{type}/{id}/stop_transcription")
    public suspend fun stopTranscription(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.StopTranscriptionResponse
    
    /**
     * List transcriptions
     * Lists transcriptions
     */
    @GET("/video/call/{type}/{id}/transcriptions")
    public suspend fun listTranscriptions(
        @Path("type") type: String,
        @Path("id") id: String
    ): io.getstream.android.video.api.generated.models.ListTranscriptionsResponse
    
    /**
     * Unblocks user on a call
     * Removes the block for a user on a call. The user will be able to join the call again.
     */
    @POST("/video/call/{type}/{id}/unblock")
    public suspend fun unblockUser(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body unblockUserRequest: io.getstream.android.video.api.generated.models.UnblockUserRequest
    ): io.getstream.android.video.api.generated.models.UnblockUserResponse
    
    /**
     * Unpin
     * Unpins a track for all users in the call.
     */
    @POST("/video/call/{type}/{id}/unpin")
    public suspend fun videoUnpin(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body unpinRequest: io.getstream.android.video.api.generated.models.UnpinRequest
    ): io.getstream.android.video.api.generated.models.UnpinResponse
    
    /**
     * Update user permissions
     * Updates user permissions
     */
    @POST("/video/call/{type}/{id}/user_permissions")
    public suspend fun updateUserPermissions(
        @Path("type") type: String,
        @Path("id") id: String ,
        @Body updateUserPermissionsRequest: io.getstream.android.video.api.generated.models.UpdateUserPermissionsRequest
    ): io.getstream.android.video.api.generated.models.UpdateUserPermissionsResponse
    
    /**
     * Delete recording
     * Deletes recording
     */
    @DELETE("/video/call/{type}/{id}/{session}/recordings/{filename}")
    public suspend fun deleteRecording(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
        @Path("filename") filename: String
    ): io.getstream.android.video.api.generated.models.DeleteRecordingResponse
    
    /**
     * Delete transcription
     * Deletes transcription
     */
    @DELETE("/video/call/{type}/{id}/{session}/transcriptions/{filename}")
    public suspend fun deleteTranscription(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
        @Path("filename") filename: String
    ): io.getstream.android.video.api.generated.models.DeleteTranscriptionResponse
    
    /**
     * Report client-side events
     * Reports a batch of client-side telemetry events. Events are processed independently; one invalid event does not block the rest of the batch, but the request fails if any event is invalid.
     */
    @POST("/video/call_client_event")
    public suspend fun reportClientCallEvent(
        @Body reportClientEventRequest: io.getstream.android.video.api.generated.models.ReportClientEventRequest
    ): io.getstream.android.video.api.generated.models.ReportClientEventResponse
    
    /**
     * Query call session stats
     * 
     */
    @POST("/video/call_stats")
    public suspend fun queryCallSessionStats(
        @Body queryCallSessionStatsRequest: io.getstream.android.video.api.generated.models.QueryCallSessionStatsRequest
    ): io.getstream.android.video.api.generated.models.QueryCallSessionStatsResponse
    
    /**
     * Query call session stats
     * 
     */
    @POST("/video/call_stats")
    public suspend fun queryCallSessionStats(
    ): io.getstream.android.video.api.generated.models.QueryCallSessionStatsResponse
    
    /**
     * Map call participants by location
     * 
     */
    @GET("/video/call_stats/{call_type}/{call_id}/{session}/map")
    public suspend fun getCallStatsMap(
        @Path("call_type") callType: String,
        @Path("call_id") callId: String,
        @Path("session") session: String,
        @Query("start_time") startTime: kotlin.time.Instant? = null,
        @Query("end_time") endTime: kotlin.time.Instant? = null,
        @Query("exclude_publishers") excludePublishers: Boolean? = null,
        @Query("exclude_subscribers") excludeSubscribers: Boolean? = null,
        @Query("exclude_sfus") excludeSfus: Boolean? = null
    ): io.getstream.android.video.api.generated.models.QueryCallStatsMapResponse
    
    /**
     * Get call session participant stats details
     * 
     */
    @GET("/video/call_stats/{call_type}/{call_id}/{session}/participant/{user}/{user_session}/details")
    public suspend fun getCallSessionParticipantStatsDetails(
        @Path("call_type") callType: String,
        @Path("call_id") callId: String,
        @Path("session") session: String,
        @Path("user") user: String,
        @Path("user_session") userSession: String,
        @Query("since") since: String? = null,
        @Query("until") until: String? = null,
        @Query("max_points") maxPoints: Int? = null
    ): io.getstream.android.video.api.generated.models.GetCallSessionParticipantStatsDetailsResponse
    
    /**
     * Query call participant statistics
     * 
     */
    @GET("/video/call_stats/{call_type}/{call_id}/{session}/participants")
    public suspend fun queryCallSessionParticipantStats(
        @Path("call_type") callType: String,
        @Path("call_id") callId: String,
        @Path("session") session: String,
        @Query("limit") limit: Int? = null,
        @Query("prev") prev: String? = null,
        @Query("next") next: String? = null,
        @Query("sort") sort: List<io.getstream.android.video.api.generated.models.SortParamRequest>? = null,
        @Query("filter_conditions") filterConditions: Map<String, Any?>? = null
    ): io.getstream.android.video.api.generated.models.QueryCallSessionParticipantStatsResponse
    
    /**
     * Get participant timeline events
     * 
     */
    @GET("/video/call_stats/{call_type}/{call_id}/{session}/participants/{user}/{user_session}/timeline")
    public suspend fun getCallSessionParticipantStatsTimeline(
        @Path("call_type") callType: String,
        @Path("call_id") callId: String,
        @Path("session") session: String,
        @Path("user") user: String,
        @Path("user_session") userSession: String,
        @Query("start_time") startTime: String? = null,
        @Query("end_time") endTime: String? = null,
        @Query("severity") severity: List<String>? = null
    ): io.getstream.android.video.api.generated.models.QueryCallSessionParticipantStatsTimelineResponse
    
    /**
     * Query call
     * Query calls with filter query
     */
    @POST("/video/calls")
    public suspend fun queryCalls(
        @Query("connection_id") connectionId: String? = null ,
        @Body queryCallsRequest: io.getstream.android.video.api.generated.models.QueryCallsRequest
    ): io.getstream.android.video.api.generated.models.QueryCallsResponse
    
    /**
     * Query call
     * Query calls with filter query
     */
    @POST("/video/calls")
    public suspend fun queryCalls(
        @Query("connection_id") connectionId: String? = null
    ): io.getstream.android.video.api.generated.models.QueryCallsResponse
    
    /**
     * Delete device
     * Deletes one device
     */
    @DELETE("/video/devices")
    public suspend fun deleteDevice(
        @Query("id") id: String
    ): io.getstream.android.video.api.generated.models.Response
    
    /**
     * List devices
     * Returns all available devices
     */
    @GET("/video/devices")
    public suspend fun listDevices(
    ): io.getstream.android.video.api.generated.models.ListDevicesResponse
    
    /**
     * Create device
     * Adds a new device to a user, if the same device already exists the call will have no effect
     */
    @POST("/video/devices")
    public suspend fun createDevice(
        @Body createDeviceRequest: io.getstream.android.video.api.generated.models.CreateDeviceRequest
    ): io.getstream.android.video.api.generated.models.Response
    
    /**
     * Get Edges
     * Returns the list of all edges available for video calls.
     */
    @GET("/video/edges")
    public suspend fun getEdges(
    ): io.getstream.android.video.api.generated.models.GetEdgesResponse
    
    /**
     * Create Guest
     * 
     */
    @POST("/video/guest")
    public suspend fun createGuest(
        @Body createGuestRequest: io.getstream.android.video.api.generated.models.CreateGuestRequest
    ): io.getstream.android.video.api.generated.models.CreateGuestResponse
    
    /**
     * Video Connect (WebSocket)
     * Establishes WebSocket connection for user to video
     */
    @GET("/video/longpoll")
    public suspend fun videoConnect(
    )
    
    /**
     * Resolve SIP Auth
     * Determine authentication requirements for an inbound SIP call before sending a digest challenge
     */
    @POST("/video/sip/auth")
    public suspend fun resolveSipAuth(
        @Body resolveSipAuthRequest: io.getstream.android.video.api.generated.models.ResolveSipAuthRequest
    ): io.getstream.android.video.api.generated.models.ResolveSipAuthResponse
    
    /**
     * Resolve SIP Inbound Routing
     * Resolve SIP inbound routing based on trunk number, caller number, and challenge authentication
     */
    @POST("/video/sip/resolve")
    public suspend fun resolveSipInbound(
        @Body resolveSipInboundRequest: io.getstream.android.video.api.generated.models.ResolveSipInboundRequest
    ): io.getstream.android.video.api.generated.models.ResolveSipInboundResponse
    
    /**
     * Query Aggregate call Stats
     * 
     */
    @POST("/video/stats")
    public suspend fun queryAggregateCallStats(
        @Body queryAggregateCallStatsRequest: io.getstream.android.video.api.generated.models.QueryAggregateCallStatsRequest
    ): io.getstream.android.video.api.generated.models.QueryAggregateCallStatsResponse
    
    /**
     * Query Aggregate call Stats
     * 
     */
    @POST("/video/stats")
    public suspend fun queryAggregateCallStats(
    ): io.getstream.android.video.api.generated.models.QueryAggregateCallStatsResponse
    
}
