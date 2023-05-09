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

import android.content.Context
import io.getstream.log.StreamLog
import io.getstream.result.Result
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.model.DEFAULT_QUERY_CALLS_LIMIT
import io.getstream.video.android.core.model.DEFAULT_QUERY_CALLS_SORT
import io.getstream.video.android.core.model.Device
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.User
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.VideoEvent

/**
 * The main interface to control the Video calls. [StreamVideoImpl] implements this interface.
 */
public interface StreamVideo {

    /**
     * Represents the default call config when starting a call.
     */
    public val context: Context
    public val user: User
    public val userId: String

    val state: ClientState

    /**
     * Create a call with the given type and id
     *
     *
     */
    public fun call(type: String, id: String = ""): Call

    /**
     * Queries calls with a given filter predicate and pagination.
     *
     * @param queryCallsData Request with the data describing the calls. Contains the filters
     * as well as pagination logic to be used when querying.
     * @return [Result] containing the [QueriedCalls].
     */
    public suspend fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField> = listOf(SortField.Asc(DEFAULT_QUERY_CALLS_SORT)),
        limit: Int = DEFAULT_QUERY_CALLS_LIMIT,
        watch: Boolean = false,
    ): Result<QueryCallsResponse>

    /** Subscribe for a specific list of events */
    public fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription

    /** Subscribe to all events */
    public fun subscribe(
        listener: VideoEventListener<VideoEvent>
    ): EventSubscription

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
     * Returns a list of all the edges available on the network.
     */
    public suspend fun getEdges(): Result<List<EdgeData>>

    /**
     * Clears the internal user state, removes push notification devices and clears the call state.
     */
    public fun logOut()

    public suspend fun registerPushDevice()

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

        /**
         * Returns an installed [StreamVideo] instance or throw an exception if its not installed.
         */
        public fun instance(): StreamVideo {
            return internalStreamVideo
                ?: throw IllegalStateException(
                    "StreamVideoBuilder.build() must be called before obtaining StreamVideo instance."
                )
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
        public fun install(streamVideo: StreamVideo) {
            synchronized(this) {
                if (isInstalled) {
                    StreamLog.e("StreamVideo") {
                        "The $internalStreamVideo is already installed but you've tried to " +
                            "install a new exception handler: $streamVideo"
                    }
                }
                internalStreamVideo = streamVideo
            }
        }

        /**
         * Uninstall a previous [StreamVideo] instance.
         */
        public fun unInstall() {
            internalStreamVideo = null
        }
    }

    public fun cleanup()
}
