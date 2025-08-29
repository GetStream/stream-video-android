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

package io.getstream.video.android.core

import android.content.Context
import androidx.compose.runtime.Stable
import io.getstream.android.push.PushDevice
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.StreamLog
import io.getstream.result.Result
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.utils.TokenUtils
import io.getstream.video.android.model.User
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The main interface to control the Video calls. [StreamVideoClient] implements this interface.
 */
@Stable
public interface StreamVideo : NotificationHandler {

    /**
     * Represents the default call config when starting a call.
     */
    public val context: Context
    public val user: User
    public val userId: String

    val state: ClientState

    /**
     * Create a call with the given type and id.
     */
    public fun call(type: String, id: String = ""): Call

    /**
     * Queries calls with a given filter predicate and pagination.
     *
     * @param queryCallsData Request with the data describing the calls. Contains the filters
     * as well as pagination logic to be used when querying.
     * @param limit - maximum number of items in response
     * @param prev - paging support. Set null for first page. Otherwise set the value from last page
     * response.
     * @param next - paging support. Set null for first page. Otherwise set the value from last page
     * response.
     *
     * @return [Result] containing the [QueriedCalls].
     */
    public suspend fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField> = listOf(SortField.Asc(DEFAULT_QUERY_CALLS_SORT)),
        limit: Int = DEFAULT_QUERY_CALLS_LIMIT,
        prev: String? = null,
        next: String? = null,
        watch: Boolean = false,
    ): Result<QueriedCalls>

    /**
     * Queries calls with a given filter predicate and pagination.
     *
     * @param prev - paging support. Set null for first page. Otherwise set the value from last page
     * response.
     * @param next - paging support. Set null for first page. Otherwise set the value from last page
     * response.
     * @param limit - maximum number of items in response
     */
    suspend fun queryMembers(
        type: String,
        id: String,
        filter: Map<String, Any>? = null,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        prev: String? = null,
        next: String? = null,
        limit: Int = 25,
    ): Result<QueriedMembers>

    /** Subscribe for a specific list of events */
    public fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription

    /** Subscribe to all events */
    public fun subscribe(
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription

    /**
     * Get a cached device used to receive push notifications.
     *
     * @return stream of Device.
     */
    public fun getDevice(): Flow<io.getstream.video.android.model.Device?>

    /**
     * Create a device that will be used to receive push notifications.
     *
     * @param pushDevice The PushDevice obtained from the selected push provider.
     */
    public suspend fun createDevice(
        pushDevice: PushDevice,
    ): Result<io.getstream.video.android.model.Device>

    /**
     * Update a device used to receive push notifications.
     *
     * @param device The Device.
     */
    public suspend fun updateDevice(device: io.getstream.video.android.model.Device?)

    /**
     * Remove a device used to receive push notifications.
     *
     * @param device The Device, previously provided by [createDevice].
     * @return Result if the operation was successful or not.
     */
    public suspend fun deleteDevice(device: io.getstream.video.android.model.Device): Result<Unit>

    /**
     * Returns a list of all the edges available on the network.
     */
    public suspend fun getEdges(): Result<List<EdgeData>>

    public suspend fun connectAsync(): Deferred<Result<Long>>

    /**
     * Clears the internal user state, removes push notification devices and clears the call state.
     */
    public fun logOut()

    public companion object {

        /**
         * Represents if [StreamVideo] is already installed or not.
         * Lets you know if the internal [StreamVideo] instance is being used as the
         * uncaught exception handler when true or if it is using the default one if false.
         */
        public var isInstalled: Boolean = false
            get() = internalStreamVideo != null
            private set

        /**
         * [StreamVideo] instance to be used.
         */
        @Volatile
        private var internalStreamVideo: StreamVideo? = null
            private set(value) {
                field = value
                // We know its MutableStateFlow
                (instanceState as MutableStateFlow).value = field
            }

        /**
         * This allows an alternative way to observe when an instance is available.
         */
        public val instanceState: StateFlow<StreamVideo?> = MutableStateFlow(internalStreamVideo)

        /**
         * Returns an installed [StreamVideo] instance or throw an exception if its not installed.
         */
        public fun instance(): StreamVideo {
            return internalStreamVideo
                ?: throw IllegalStateException(
                    "StreamVideoBuilder.build() must be called before obtaining StreamVideo instance.",
                )
        }

        public fun instanceOrNull(): StreamVideo? {
            return internalStreamVideo
        }

        /**
         * Returns an installed [StreamVideo] instance lazy or throw an exception if its not installed.
         */
        public fun lazyInstance(): Lazy<StreamVideo> {
            return lazy(LazyThreadSafetyMode.NONE) { instance() }
        }

        /**
         * Installs a new [StreamVideo] instance to be used.
         */
        internal fun install(streamVideo: StreamVideo) {
            synchronized(this) {
                if (isInstalled) {
                    StreamLog.e("StreamVideo") {
                        "The $internalStreamVideo is already installed but you've tried to " +
                            "install a new exception handler: $streamVideo"
                    }
                }
                isInstalled = true
                internalStreamVideo = streamVideo
            }
        }

        /**
         * Uninstall a previous [StreamVideo] instance.
         */
        public fun removeClient() {
            isInstalled = false
            internalStreamVideo?.cleanup()
            internalStreamVideo = null
        }

        /**
         * Generate a developer token that can be used to connect users while the app is using a development environment.
         *
         * @param userId the desired id of the user to be connected.
         */
        public fun devToken(userId: String): String = TokenUtils.devToken(userId)
    }

    public fun cleanup()
    suspend fun connectIfNotAlreadyConnected()
}

private const val DEFAULT_QUERY_CALLS_SORT = "cid"
private const val DEFAULT_QUERY_CALLS_LIMIT = 25
