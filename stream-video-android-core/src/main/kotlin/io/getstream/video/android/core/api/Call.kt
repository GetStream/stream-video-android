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

package io.getstream.video.android.core.api

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import io.getstream.android.video.generated.models.AcceptCallResponse
import io.getstream.android.video.generated.models.BlockUserResponse
import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.GoLiveResponse
import io.getstream.android.video.generated.models.KickUserResponse
import io.getstream.android.video.generated.models.ListRecordingsResponse
import io.getstream.android.video.generated.models.ListTranscriptionsResponse
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.android.video.generated.models.PinResponse
import io.getstream.android.video.generated.models.RejectCallResponse
import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.android.video.generated.models.RingCallResponse
import io.getstream.android.video.generated.models.SendCallEventResponse
import io.getstream.android.video.generated.models.SendReactionResponse
import io.getstream.android.video.generated.models.StartClosedCaptionsResponse
import io.getstream.android.video.generated.models.StartTranscriptionResponse
import io.getstream.android.video.generated.models.StopClosedCaptionsResponse
import io.getstream.android.video.generated.models.StopLiveResponse
import io.getstream.android.video.generated.models.StopTranscriptionResponse
import io.getstream.android.video.generated.models.UnpinResponse
import io.getstream.android.video.generated.models.UpdateCallMembersResponse
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdateUserPermissionsResponse
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.result.Result
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.CallStatsReport
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.EventSubscription
import io.getstream.video.android.core.call.audio.InputAudioFilter
import io.getstream.video.android.core.call.video.VideoFilter
import io.getstream.video.android.core.closedcaptions.ClosedCaptionsSettings
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.recording.RecordingType
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import io.getstream.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.ClientCapability
import stream.video.sfu.models.TrackType

/**
 * The public interface for a video call.
 *
 * Provides access to call lifecycle, media controls, member management, recording,
 * streaming, and event observation. Sub-interfaces [CameraManager], [MicrophoneManager],
 * [SpeakerManager], and [ScreenShareManager] group related media functionality.
 *
 * Internal methods (fastReconnect, rejoin, migrate, handleEvent, fireEvent) and
 * deprecated methods (subscribe, unsubscribe) are intentionally excluded.
 *
 * This interface is not intended for external implementation. The SDK provides
 * the only supported implementation. New members may be added in minor releases.
 */
@Stable
public interface Call {

    // -- Identity --

    /** The call type (e.g., "default", "livestream"). */
    public val type: String

    /** The call ID. */
    public val id: String

    /** The combined call identifier in "type:id" format. */
    public val cid: String

    /** The current user. */
    public val user: User

    /** The unique session ID for this call participant. */
    public val sessionId: String

    // -- State --

    /** The call state containing participant list, connection status, settings, etc. */
    public val state: CallState

    // -- Events --

    /**
     * A shared flow of video events for this call.
     * Consumers should collect this flow instead of using the deprecated subscribe/unsubscribe API.
     */
    public val events: SharedFlow<VideoEvent>

    // -- Stats --

    /** The latest call stats report. */
    public val statsReport: StateFlow<CallStatsReport?>

    /** History of latency measurements. */
    public val statLatencyHistory: StateFlow<List<Int>>

    // -- Audio level --

    /**
     * The local microphone audio level as a smoothed value between 0 (silent) and 1 (maximum).
     * Does not return values until the session is established.
     */
    public val localMicrophoneAudioLevel: StateFlow<Float>

    // -- Filters --

    /** A custom video filter applied to the outgoing video stream. */
    public var videoFilter: VideoFilter?

    /** A custom audio filter applied to the outgoing audio stream. */
    public var audioFilter: InputAudioFilter?

    // -- Sub-interface accessors --

    /** Camera controls for this call. */
    public val camera: CameraManager

    /** Microphone controls for this call. */
    public val microphone: MicrophoneManager

    /** Speaker controls for this call. */
    public val speaker: SpeakerManager

    /** Screen share controls for this call. */
    public val screenShare: ScreenShareManager

    // -- Lifecycle --

    /**
     * Creates a call on the server.
     *
     * @return Result containing the response with call details.
     */
    public suspend fun create(
        memberIds: List<String>? = null,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        settings: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        video: Boolean? = null,
    ): Result<GetOrCreateCallResponse>

    /**
     * Fetches the call details from the server.
     */
    public suspend fun get(): Result<GetCallResponse>

    /**
     * Updates call properties on the server.
     */
    public suspend fun update(
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
    ): Result<UpdateCallResponse>

    /**
     * Joins the call, establishing a real-time audio/video session.
     *
     * @return Result<Unit> on success. Observe [state] for connection status.
     */
    public suspend fun join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<Unit>

    /**
     * Leaves the call without ending it for other participants.
     *
     * @param reason The reason for leaving.
     */
    public fun leave(reason: String = "user")

    /**
     * Ends the call for all participants.
     */
    public suspend fun end(): Result<Unit>

    /**
     * Cleans up call resources.
     */
    public fun cleanup()

    // -- Ringing --

    /**
     * Rings the call (simple form).
     */
    public suspend fun ring(): Result<GetCallResponse>

    /**
     * Rings the call with a specific request.
     */
    public suspend fun ring(ringCallRequest: RingCallRequest): Result<RingCallResponse>

    /**
     * Sends a notification for the call.
     */
    public suspend fun notify(): Result<GetCallResponse>

    /**
     * Accepts an incoming call.
     */
    public suspend fun accept(): Result<AcceptCallResponse>

    /**
     * Rejects an incoming call.
     *
     * @param reason The reason for rejecting.
     */
    public suspend fun reject(reason: RejectReason? = null): Result<RejectCallResponse>

    // -- Members & Permissions --

    /**
     * Queries call members with filtering and pagination.
     */
    public suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 25,
        prev: String? = null,
        next: String? = null,
    ): Result<QueriedMembers>

    /**
     * Updates call members.
     */
    public suspend fun updateMembers(
        memberRequests: List<MemberRequest>,
    ): Result<UpdateCallMembersResponse>

    /**
     * Removes members from the call.
     */
    public suspend fun removeMembers(userIds: List<String>): Result<UpdateCallMembersResponse>

    /**
     * Blocks a user from the call.
     */
    public suspend fun blockUser(userId: String): Result<BlockUserResponse>

    /**
     * Kicks a user from the call.
     *
     * @param userId The user to kick.
     * @param block If true, the user will be blocked from rejoining.
     */
    public suspend fun kickUser(
        userId: String,
        block: Boolean = false,
    ): Result<KickUserResponse>

    /**
     * Mutes a single user.
     */
    public suspend fun muteUser(
        userId: String,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse>

    /**
     * Mutes multiple users.
     */
    public suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse>

    /**
     * Mutes all users in the call.
     */
    public suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse>

    /**
     * Grants permissions to a user.
     */
    public suspend fun grantPermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse>

    /**
     * Revokes permissions from a user.
     */
    public suspend fun revokePermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse>

    /**
     * Requests permissions for the current user.
     */
    public suspend fun requestPermissions(vararg permission: String): Result<Unit>

    // -- Media control --

    /**
     * Starts screen sharing.
     *
     * @param mediaProjectionPermissionResultData Intent data from screen capture permission.
     * @param includeAudio Whether to include audio in the screen share.
     */
    public fun startScreenSharing(
        mediaProjectionPermissionResultData: Intent,
        includeAudio: Boolean = false,
    )

    /** Stops screen sharing. */
    public fun stopScreenSharing()

    /**
     * Sets the preferred incoming video resolution for participants.
     *
     * @param resolution The preferred resolution, or null for auto.
     * @param sessionIds Participant session IDs to apply to, or null for all.
     */
    public fun setPreferredIncomingVideoResolution(
        resolution: PreferredVideoResolution?,
        sessionIds: List<String>? = null,
    )

    /**
     * Enables or disables incoming video.
     *
     * @param enabled Whether video should be enabled, or null for auto.
     * @param sessionIds Participant session IDs to apply to, or null for all.
     */
    public fun setIncomingVideoEnabled(enabled: Boolean?, sessionIds: List<String>? = null)

    /**
     * Enables or disables incoming audio.
     *
     * @param enabled Whether audio should be enabled.
     * @param sessionIds Participant session IDs to apply to, or null for all.
     */
    public fun setIncomingAudioEnabled(enabled: Boolean, sessionIds: List<String>? = null)

    /**
     * Sets the visibility of a participant's track.
     */
    public fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
    )

    /**
     * Sets the visibility of a participant's track with specific dimensions.
     */
    public fun setVisibility(
        sessionId: String,
        trackType: TrackType,
        visible: Boolean,
        viewportId: String = sessionId,
        width: Int,
        height: Int,
    )

    /**
     * Initializes a video renderer for a participant's track.
     */
    public fun initRenderer(
        videoRenderer: VideoTextureViewRenderer,
        sessionId: String,
        trackType: TrackType,
        onRendered: (VideoTextureViewRenderer) -> Unit = {},
        viewportId: String = sessionId,
    )

    /**
     * Processes an audio sample for speaking-while-muted detection.
     */
    public fun processAudioSample(audioSample: AudioSamples)

    // -- Recording & Streaming --

    /** Starts recording with the default (composite) type. */
    public suspend fun startRecording(): Result<Unit>

    /** Starts recording with a specific type. */
    public suspend fun startRecording(recordingType: RecordingType): Result<Unit>

    /** Stops recording with the default (composite) type. */
    public suspend fun stopRecording(): Result<Unit>

    /** Stops recording with a specific type. */
    public suspend fun stopRecording(recordingType: RecordingType): Result<Unit>

    /**
     * Lists recordings for this call.
     *
     * @param sessionId If provided, only recordings for that session are returned.
     */
    public suspend fun listRecordings(sessionId: String? = null): Result<ListRecordingsResponse>

    /** Starts HLS broadcasting. */
    public suspend fun startHLS(): Result<Unit>

    /** Stops HLS broadcasting. */
    public suspend fun stopHLS(): Result<Unit>

    /**
     * Transitions the call to live mode.
     */
    public suspend fun goLive(
        startHls: Boolean = false,
        startRecording: Boolean = false,
        startTranscription: Boolean = false,
    ): Result<GoLiveResponse>

    /** Stops live mode. */
    public suspend fun stopLive(): Result<StopLiveResponse>

    /** Starts transcription. */
    public suspend fun startTranscription(): Result<StartTranscriptionResponse>

    /** Stops transcription. */
    public suspend fun stopTranscription(): Result<StopTranscriptionResponse>

    /** Lists transcriptions for this call. */
    public suspend fun listTranscription(): Result<ListTranscriptionsResponse>

    /** Starts closed captions. */
    public suspend fun startClosedCaptions(): Result<StartClosedCaptionsResponse>

    /** Stops closed captions. */
    public suspend fun stopClosedCaptions(): Result<StopClosedCaptionsResponse>

    /** Updates closed captions settings. */
    public fun updateClosedCaptionsSettings(closedCaptionsSettings: ClosedCaptionsSettings)

    // -- Pinning --

    /** Pins a participant for all users. */
    public suspend fun pinForEveryone(sessionId: String, userId: String): Result<PinResponse>

    /** Unpins a participant for all users. */
    public suspend fun unpinForEveryone(sessionId: String, userId: String): Result<UnpinResponse>

    /** Returns whether the participant is pinned (server or local). */
    public fun isPinnedParticipant(sessionId: String): Boolean

    /** Returns whether the participant has a server-side pin. */
    public fun isServerPin(sessionId: String): Boolean

    /** Returns whether the participant has a local pin. */
    public fun isLocalPin(sessionId: String): Boolean

    // -- Events --

    /**
     * Subscribes to specific event types on this call.
     */
    public fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription

    // -- Utility --

    /** Returns whether the current user has all the specified capabilities. */
    public fun hasCapability(vararg capability: OwnCapability): Boolean

    /** Returns whether video is enabled in the call settings. */
    public fun isVideoEnabled(): Boolean

    /** Returns whether audio processing is currently enabled. */
    public fun isAudioProcessingEnabled(): Boolean

    /** Enables or disables audio processing. */
    public fun setAudioProcessingEnabled(enabled: Boolean)

    /** Toggles audio processing and returns the new state. */
    public fun toggleAudioProcessing(): Boolean

    /** Enables the provided client capabilities. */
    public fun enableClientCapabilities(capabilities: List<ClientCapability>)

    /** Disables the provided client capabilities. */
    public fun disableClientCapabilities(capabilities: List<ClientCapability>)

    /**
     * Collects user feedback for the call.
     *
     * @param rating Rating value.
     * @param reason Optional text reason.
     * @param custom Optional custom data.
     */
    public fun collectUserFeedback(
        rating: Int,
        reason: String? = null,
        custom: Map<String, Any>? = null,
    )

    /**
     * Takes a screenshot of a video track.
     *
     * @param track The video track to capture.
     * @return The captured bitmap, or null on failure.
     */
    public suspend fun takeScreenshot(track: VideoTrack): Bitmap?

    /**
     * Sends a reaction to the call.
     */
    public suspend fun sendReaction(
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse>

    /**
     * Sends a custom event to the call.
     */
    public suspend fun sendCustomEvent(data: Map<String, Any>): Result<SendCallEventResponse>
}
