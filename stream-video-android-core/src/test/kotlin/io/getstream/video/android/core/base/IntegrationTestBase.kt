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

package io.getstream.video.android.core.base

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import io.getstream.log.Priority
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.logging.HttpLoggingLevel
import io.getstream.video.android.core.logging.LoggingLevel
import io.mockk.MockKAnnotations
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.openapitools.client.models.AudioSettingsResponse
import org.openapitools.client.models.BackstageSettingsResponse
import org.openapitools.client.models.BroadcastSettingsResponse
import org.openapitools.client.models.CallIngressResponse
import org.openapitools.client.models.CallResponse
import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.EgressResponse
import org.openapitools.client.models.GeofenceSettingsResponse
import org.openapitools.client.models.HLSSettingsResponse
import org.openapitools.client.models.LimitsSettingsResponse
import org.openapitools.client.models.RTMPIngress
import org.openapitools.client.models.RecordSettingsRequest
import org.openapitools.client.models.RecordSettingsResponse
import org.openapitools.client.models.RingSettingsResponse
import org.openapitools.client.models.ScreensharingSettingsResponse
import org.openapitools.client.models.TargetResolution
import org.openapitools.client.models.ThumbnailsSettingsResponse
import org.openapitools.client.models.TranscriptionSettingsResponse
import org.openapitools.client.models.UserResponse
import org.openapitools.client.models.VideoEvent
import org.openapitools.client.models.VideoSettingsResponse
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object IntegrationTestState {
    var client: StreamVideo? = null
    var call: Call? = null
}

open class IntegrationTestBase(val connectCoordinatorWS: Boolean = true) : TestBase() {
    /** Client */
    lateinit var client: StreamVideo

    /** Implementation of the client for more access to interals */
    internal lateinit var clientImpl: StreamVideoClient

    /** Tracks all events received by the client during a test */
    lateinit var events: MutableList<VideoEvent>

    /** The builder used for creating the client */
    lateinit var builder: StreamVideoBuilder

    var nextEventContinuation: Continuation<VideoEvent>? = null
    var nextEventCompleted: Boolean = false

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Before
    fun setupVideo() {
        builder = StreamVideoBuilder(
            context = ApplicationProvider.getApplicationContext(),
            apiKey = authData?.apiKey!!,
            geo = GEO.GlobalEdgeNetwork,
            user = testData.users["thierry"]!!,
            token = authData?.token!!,
            loggingLevel = LoggingLevel(Priority.DEBUG, HttpLoggingLevel.BASIC),
        )
//        if (BuildConfig.CORE_TEST_LOCAL == "1") {
//            builder.videoDomain = "localhost"
//        }

        if (IntegrationTestState.client == null) {
            client = builder.build()
            clientImpl = client as StreamVideoClient
            clientImpl.testSessionId = UUID.randomUUID().toString()
            // always mock the peer connection factory, it can't work in unit tests
            clientImpl.peerConnectionFactory = mockedPCFactory
            Call.testInstanceProvider.mediaManagerCreator = { mockk(relaxed = true) }
            Call.testInstanceProvider.rtcSessionCreator = { mockk(relaxed = true) }
            // Connect to the WS if needed
            if (connectCoordinatorWS) {
                // wait for the connection/ avoids race conditions in tests
                runBlocking(CoroutineScope(dispatcherRule.testDispatcher).coroutineContext) {
                    withTimeout(10000) {
                        val connectResultDeferred = clientImpl.connectAsync()
                        val connectResult = connectResultDeferred.await()
                    }
                }
            }
            IntegrationTestState.client = client
        } else {
            client = IntegrationTestState.client!!
            clientImpl = client as StreamVideoClient
        }

        // monitor for events
        events = mutableListOf()
        client.subscribe {
            events.add(it)

            nextEventContinuation?.let { continuation ->
                if (!nextEventCompleted) {
                    nextEventCompleted = true
                    continuation.resume(value = it)
                }
            }
        }
    }

    val call: Call by lazy {
        if (IntegrationTestState.call != null) {
            IntegrationTestState.call!!
        } else {
            val call = client.call("default", randomUUID())
            IntegrationTestState.call = call
            runBlocking {
                val result = call.create()
                assertSuccess(result)
            }
            call
        }
    }

    /**
     * Assert that we received the event of this type, get the event as well for
     * further checks
     */
    fun assertEventReceived(eventClass: Class<*>?): VideoEvent {
        val eventTypes = events.map { it::class.java }
        Truth.assertThat(eventTypes).contains(eventClass)
        return events.filter { it::class.java == eventClass }[0]
    }

    /**
     * If we have already received an event of this type return immediately
     * Otherwise wait for the next event of this type
     * TODO: add a timeout
     */
    suspend inline fun <reified T : VideoEvent> waitForNextEvent(): T =
        // This is needed because some of our unit tests are doing real API calls and we need
        // to wait the real time, not just the virtual dispatcher time.
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(10000) {
                suspendCoroutine { continuation ->
                    var finished = false

                    // check historical events
                    val matchingEvents = events.filterIsInstance<T>()
                    if (matchingEvents.isNotEmpty()) {
                        continuation.resume(matchingEvents[0])
                        finished = true
                    }

                    client.subscribe {
                        if (!finished) {
                            // listen to the latest events
                            if (it is T) {
                                continuation.resume(it)
                                finished = true
                            }
                        }
                    }
                }
            }
        }

    @Before
    fun resetTestVars() {
        events = mutableListOf()
    }
}

// convert a Call object to a CallResponse object
internal fun Call.toResponse(createdBy: UserResponse): CallResponse {
    val now = OffsetDateTime.now(Clock.systemUTC())
    val ingress = CallIngressResponse(rtmp = RTMPIngress(address = ""))
    val audioSettings = AudioSettingsResponse(
        accessRequestEnabled = true,
        micDefaultOn = true,
        opusDtxEnabled = true,
        redundantCodingEnabled = true,
        speakerDefaultOn = true,
        defaultDevice = AudioSettingsResponse.DefaultDevice.Speaker,
    )
    val settings = CallSettingsResponse(
        audio = audioSettings,
        backstage = BackstageSettingsResponse(enabled = false),
        broadcasting = BroadcastSettingsResponse(
            enabled = false,
            hls = HLSSettingsResponse(autoOn = false, enabled = false, qualityTracks = listOf("f")),
        ),
        geofencing = GeofenceSettingsResponse(names = emptyList()),
        recording = RecordSettingsResponse(
            audioOnly = false,
            mode = RecordSettingsRequest.Mode.Available.value,
            quality = RecordSettingsRequest.Quality.`720p`.value,
        ),
        ring = RingSettingsResponse(
            autoCancelTimeoutMs = 10000,
            incomingCallTimeoutMs = 10000,
            missedCallTimeoutMs = 10000,
        ),
        screensharing = ScreensharingSettingsResponse(false, false),
        transcription = TranscriptionSettingsResponse(
            TranscriptionSettingsResponse.ClosedCaptionMode.Available,
            emptyList(),
            TranscriptionSettingsResponse.Mode.Available,
        ),
        video = VideoSettingsResponse(
            false,
            false,
            VideoSettingsResponse.CameraFacing.Front,
            false,
            TargetResolution(3000000, 1024, 1280),
        ),
        limits = LimitsSettingsResponse(
            maxParticipants = 10,
            maxDurationSeconds = 10,
        ),
        thumbnails = ThumbnailsSettingsResponse(
            enabled = false,
        ),
    )
    val response = CallResponse(
        id = id,
        type = type,
        cid = "$type:$id",
        backstage = false,
        createdAt = now,
        custom = emptyMap(),
        recording = false,
        transcribing = false,
        currentSessionId = "",
        createdBy = createdBy,
        blockedUserIds = emptyList(),
        ingress = ingress,
        settings = settings,
        egress = EgressResponse(false, emptyList(), null),
        updatedAt = now,
        captioning = false,
    )
    return response
}
