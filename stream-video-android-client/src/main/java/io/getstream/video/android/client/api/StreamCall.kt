package io.getstream.video.android.client.api

import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.android.video.generated.models.PinResponse
import io.getstream.android.video.generated.models.SendReactionResponse
import io.getstream.android.video.generated.models.UnpinResponse
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.video.android.client.api.listeners.StreamVideoClientListener
import io.getstream.video.android.client.api.listeners.StreamVideoEventListener
import io.getstream.video.android.client.api.subscribe.StreamSubscription
import io.getstream.video.android.client.model.StreamCallCreateOptions
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.SortField
import org.threeten.bp.OffsetDateTime

/**
 * The main interface to control the Video calls.
 */
public interface StreamCall {

    /**
     * Get the call.
     *
     * @param type The type of the call
     * @param id The id of the call
     * @return The call response from the backend
     */
    public suspend fun get(): Result<GetCallResponse>

    /**
     * Create the call.
     *
     * @param memberIds The list of member ids to add to the call
     * @param members The list of member requests to add to the call
     * @param custom The custom data to add to the call
     * @param settings The call settings to add to the call
     * @param startsAt The start time of the call
     * @param team The team to add to the call
     * @param ring If the call should ring for other participants (sends push)
     * @param notify If the call should notify other participants (sends push)
     * @return The call response from the backend
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
    ): Result<GetOrCreateCallResponse>

    /**
     * Update the call.
     *
     * @param custom The custom data to add to the call
     * @param settingsOverride The call settings to add to the call
     * @param startsAt The start time of the call
     * @return The call response from the backend
     */
    public suspend fun update(
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
    ): Result<UpdateCallResponse>

    /**
     * Join the call.
     *
     * @param create If the call should be created if it doesn't exist
     * @param createOptions The create options to use if the call is created
     * @param ring If the call should ring for other participants (sends push)
     * @param notify If the call should notify other participants (sends push)
     * @return The call response from the backend
     */
    public suspend fun join(
        create: Boolean = false,
        createOptions: StreamCallCreateOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<StreamCall>

    /**
     * Leave the call.
     *
     * @return The call response from the backend
     */
    public suspend fun leave() : Result<Unit>

    /**
     * End the call.
     *
     * @return The call response from the backend
     */
    public suspend fun end(): Result<Unit>

    /**
     * Pin a track for all users in the call.
     *
     * @param sessionId The session id of the track to pin
     * @param userId The user id of the track to pin
     * @return The call response from the backend
     */
    public suspend fun pinForEveryone(sessionId: String, userId: String): Result<PinResponse>

    /**
     * Unpin a track for all users in the call.
     *
     * @param sessionId The session id of the track to unpin
     * @param userId The user id of the track to unpin
     * @return The call response from the backend
     */
    public suspend fun unpinForEveryone(sessionId: String, userId: String): Result<UnpinResponse>

    /**
     * Send a reaction to the call.
     *
     * @param type The type of reaction
     * @param emoji The emoji to send
     * @param custom The custom data to send
     * @return The call response from the backend
     */
    public suspend fun sendReaction(
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse>

    /**
     * Query the members of the call.
     *
     * @param filter The filter to use
     * @param sort The sort to use
     * @param limit The limit to use
     * @param prev The prev to use
     * @param next The next to use
     * @return The call response from the backend
     */
    public suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 25,
        prev: String? = null,
        next: String? = null,
    ): Result<QueriedMembers>

    /**
     * Mute all users in the call.
     *
     * @param audio If the audio should be muted
     * @param video If the video should be muted
     * @param screenShare If the screen share should be muted
     * @return The call response from the backend
     */
    public suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse>

    /**
     * Unmute all users in the call.
     *
     * @param audio If the audio should be unmuted
     * @param video If the video should be unmuted
     * @param screenShare If the screen share should be unmuted
     * @return The call response from the backend
     */
    public suspend fun unmuteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse>

    /**
     * Mute users in the call.
     *
     * @param userIds The users to mute
     * @param audio If the audio should be muted
     * @param video If the video should be muted
     * @param screenShare If the screen share should be muted
     * @return The call response from the backend
     */
    public suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse>

    /**
     * Unmute users in the call.
     *
     * @param userIds The users to unmute
     * @param audio If the audio should be unmuted
     * @param video If the video should be unmuted
     * @param screenShare If the screen share should be unmuted
     * @return The call response from the backend
     */
    public suspend fun unmuteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
    ): Result<MuteUsersResponse>

    /**
     * Subscribe to the call events.
     *
     * @param listener The listener to use
     * @return The subscription
     */
    public fun subscribe(listener: StreamVideoEventListener): Result<StreamSubscription>
}