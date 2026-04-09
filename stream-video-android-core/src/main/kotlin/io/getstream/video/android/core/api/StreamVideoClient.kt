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

import android.content.Context
import androidx.compose.runtime.Stable
import io.getstream.android.push.PushDevice
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.result.Result
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.EventSubscription
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

/**
 * The main interface to control Stream Video calls.
 *
 * Replaces the v1 [io.getstream.video.android.core.StreamVideo] interface with a cleaner
 * contract. Provides call creation, querying, device management, event subscription,
 * and connection lifecycle.
 *
 * Companion object methods (singleton management) are intentionally excluded --
 * these move to VideoClientRegistry in a future phase.
 *
 * Inherits [NotificationHandler] for push notification support. This coupling
 * is documented for future refactoring.
 */
@Stable
public interface StreamVideoClient : NotificationHandler {

    /** The application context. */
    public val context: Context

    /** The current authenticated user. */
    public val user: User

    /** The current user's ID. */
    public val userId: String

    /** The client-level state (active call, ringing call, connection status). */
    public val state: ClientState

    /**
     * Creates or retrieves a call with the given type and ID.
     *
     * @param type The call type (e.g., "default", "livestream").
     * @param id The call ID. If empty, a new ID will be generated.
     * @return A [Call] instance for the given type and ID.
     */
    public fun call(type: String, id: String = ""): Call

    /**
     * Queries calls with filtering, sorting, and pagination.
     *
     * @param filters Filter predicates for the query.
     * @param sort Sort fields and direction.
     * @param limit Maximum number of results.
     * @param prev Pagination cursor for previous page.
     * @param next Pagination cursor for next page.
     * @param watch Whether to watch for real-time updates.
     * @return Result containing the queried calls.
     */
    public suspend fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField> = listOf(SortField.Asc("cid")),
        limit: Int = 25,
        prev: String? = null,
        next: String? = null,
        watch: Boolean = false,
    ): Result<QueriedCalls>

    /**
     * Queries members of a call with filtering, sorting, and pagination.
     *
     * @param type The call type.
     * @param id The call ID.
     * @param filter Filter predicates.
     * @param sort Sort fields and direction.
     * @param prev Pagination cursor for previous page.
     * @param next Pagination cursor for next page.
     * @param limit Maximum number of results.
     * @return Result containing the queried members.
     */
    public suspend fun queryMembers(
        type: String,
        id: String,
        filter: Map<String, Any>? = null,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        prev: String? = null,
        next: String? = null,
        limit: Int = 25,
    ): Result<QueriedMembers>

    /**
     * Subscribes to specific event types.
     *
     * @param eventTypes The event types to subscribe to.
     * @param listener The listener to invoke when matching events occur.
     * @return An [EventSubscription] that can be disposed to stop receiving events.
     */
    public fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription

    /**
     * Subscribes to all events.
     *
     * @param listener The listener to invoke for every event.
     * @return An [EventSubscription] that can be disposed to stop receiving events.
     */
    public fun subscribe(
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription

    /**
     * Gets the cached push notification device.
     *
     * @return A flow emitting the current device, or null.
     */
    public fun getDevice(): Flow<Device?>

    /**
     * Registers a device for push notifications.
     *
     * @param pushDevice The push device from the selected push provider.
     * @return Result containing the created device.
     */
    public suspend fun createDevice(pushDevice: PushDevice): Result<Device>

    /**
     * Updates a push notification device.
     *
     * @param device The device to update.
     */
    public suspend fun updateDevice(device: Device?)

    /**
     * Removes a push notification device.
     *
     * @param device The device to remove.
     * @return Result indicating success or failure.
     */
    public suspend fun deleteDevice(device: Device): Result<Unit>

    /**
     * Returns the list of available network edges.
     */
    public suspend fun getEdges(): Result<List<EdgeData>>

    /**
     * Connects the user asynchronously.
     *
     * @return A deferred result with the connection timestamp.
     */
    public suspend fun connectAsync(): Deferred<Result<Long>>

    /**
     * Connects the user and suspends until connected.
     *
     * @return Result with the connection timestamp.
     */
    public suspend fun connect(): Result<Long>

    /**
     * Connects the user if not already connected.
     */
    public suspend fun connectIfNotAlreadyConnected()

    /**
     * Logs out the user, clearing internal state and push devices.
     */
    public fun logOut()

    /**
     * Cleans up all resources held by this client.
     */
    public fun cleanup()
}
