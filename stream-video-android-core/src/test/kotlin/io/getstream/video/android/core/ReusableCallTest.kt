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

package io.getstream.video.android.core

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jakewharton.threetenabp.AndroidThreeTen
import io.getstream.android.core.api.StreamClient
import io.getstream.android.core.api.model.connection.StreamConnectionState
import io.getstream.android.core.api.subscribe.StreamSubscription
import io.getstream.android.video.api.generated.models.Credentials
import io.getstream.android.video.api.generated.models.JoinCallResponse
import io.getstream.android.video.api.generated.models.SFUResponse
import io.getstream.android.video.api.generated.models.StatsOptions
import io.getstream.android.video.api.generated.models.UserResponse
import io.getstream.android.video.api.generated.models.VideoEvent
import io.getstream.result.Result.Success
import io.getstream.video.android.core.analytics.AnalyticsModule
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.base.toResponse
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.sounds.RingingCallVibrationConfig
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.model.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.OffsetDateTime
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType

@RunWith(RobolectricTestRunner::class)
class ReusableCallTest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

    @Before
    fun setUp() {
        AndroidThreeTen.init(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        Call.testInstanceProvider.mediaManagerCreator = null
        unmockkAll()
    }

    @Test
    fun `same Call joins leaves and joins again with fresh session state`() = runTest {
        val mediaManager = reusableMediaManager()
        Call.testInstanceProvider.mediaManagerCreator = { mediaManager }
        val client = reusableClient(backgroundScope)
        val call = client.call("default", "reusable-call")
        val events = call.events

        assertThat(client.call("default", "reusable-call")).isSameInstanceAs(call)

        val firstSession = reusableSession()
        val secondSession = reusableSession()
        val sessions = ArrayDeque(listOf(firstSession, secondSession))
        var secondGenerationSnapshot: ResetSnapshot? = null
        val firstSessionId = call.sessionId
        call.unitTestRtcSessionFactory = {
            if (sessions.size == 1) {
                secondGenerationSnapshot = ResetSnapshot(
                    sessionWasCleared = call.session.value == null,
                    participantsWereCleared = call.state.participants.value.isEmpty(),
                    statsReportWasCleared = call.statsReport.value == null,
                    statsHistory = call.statLatencyHistory.value,
                    publisherLatency = call.state.stats.publisher.latency.value,
                    reconnectAttempts = call.nonFastReconnectAttempts(),
                    reconnectDeadlineMillis = call.reconnectDeadlineMillis,
                    ringingState = call.state.ringingState.value,
                    sessionId = call.sessionId,
                )
            }
            sessions.removeFirst()
        }
        var joinNumber = 0
        coEvery { client.getCachedLocation() } returns Success("test-location")
        coEvery {
            client.joinCall(
                any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(),
            )
        } coAnswers {
            joinNumber++
            Success(joinResponse(call, "session-$joinNumber"))
        }
        coEvery { firstSession.connectInternal(any(), any()) } returns SfuConnectionResult.Success

        val secondVideoTrack = VideoTrack(
            streamId = "second-video",
            video = mockk(relaxed = true),
        )
        val secondAudioTrack = AudioTrack(
            streamId = "second-audio",
            audio = mockk(relaxed = true),
        )
        coEvery { secondSession.connectInternal(any(), any()) } coAnswers {
            val participant = call.state.getOrCreateParticipant(
                Participant(
                    user_id = "second-user",
                    session_id = "second-participant",
                    published_tracks = listOf(
                        TrackType.TRACK_TYPE_VIDEO,
                        TrackType.TRACK_TYPE_AUDIO,
                    ),
                    track_lookup_prefix = "second",
                    name = "Second participant",
                    image = "",
                ),
            )
            participant.setVideoTrack(secondVideoTrack)
            participant._audioTrack.value = secondAudioTrack
            call.state.replaceParticipants(listOf(participant))
            SfuConnectionResult.Success
        }

        val participantEmissions = mutableListOf<List<String>>()
        val participantCollector = backgroundScope.launch {
            call.state.participants.collect { participants ->
                participantEmissions += participants.map(ParticipantState::sessionId)
            }
        }
        val receivedEvents = mutableListOf<VideoEvent>()
        val eventCollector = backgroundScope.launch { events.collect(receivedEvents::add) }
        runCurrent()

        assertThat(call.join()).isEqualTo(Success(firstSession))

        val oldVideoTrack = VideoTrack(
            streamId = "old-video",
            video = mockk(relaxed = true),
        )
        val oldAudioTrack = AudioTrack(
            streamId = "old-audio",
            audio = mockk(relaxed = true),
        )
        val oldParticipant = call.state.getOrCreateParticipant(
            Participant(
                user_id = "old-user",
                session_id = "old-participant",
                published_tracks = listOf(
                    TrackType.TRACK_TYPE_VIDEO,
                    TrackType.TRACK_TYPE_AUDIO,
                ),
                track_lookup_prefix = "old",
                name = "Old participant",
                image = "",
            ),
        )
        oldParticipant.setVideoTrack(oldVideoTrack)
        oldParticipant._audioTrack.value = oldAudioTrack
        call.state.replaceParticipants(listOf(oldParticipant))
        call.statsReport.value = mockk(relaxed = true)
        call.statLatencyHistory.value = listOf(17, 23)
        call.state.stats.publisher._latency.value = 91
        call.injectReconnectState(attempts = 4, deadlineMillis = 42_000)
        call.state.injectRingingState(RingingState.Active)

        call.leave()
        assertThat(call.join()).isEqualTo(Success(secondSession))
        runCurrent()

        val snapshot = checkNotNull(secondGenerationSnapshot)
        assertThat(snapshot.sessionWasCleared).isTrue()
        assertThat(snapshot.participantsWereCleared).isTrue()
        assertThat(snapshot.statsReportWasCleared).isTrue()
        assertThat(snapshot.statsHistory).containsExactly(0, 0, 0).inOrder()
        assertThat(snapshot.publisherLatency).isEqualTo(0)
        assertThat(snapshot.reconnectAttempts).isEqualTo(0)
        assertThat(snapshot.reconnectDeadlineMillis).isEqualTo(10_000)
        assertThat(snapshot.ringingState).isEqualTo(RingingState.Idle)
        assertThat(snapshot.sessionId).isNotEqualTo(firstSessionId)

        assertThat(call.session.value).isSameInstanceAs(secondSession)
        assertThat(call.state.getParticipantBySessionId("old-participant")).isNull()
        val secondParticipant = call.state.getParticipantBySessionId("second-participant")
        assertThat(secondParticipant).isNotNull()
        assertThat(secondParticipant!!.videoTrack.value).isSameInstanceAs(secondVideoTrack)
        assertThat(secondParticipant.audioTrack.value).isSameInstanceAs(secondAudioTrack)
        assertThat(secondParticipant.videoEnabled.value).isTrue()
        assertThat(secondParticipant.audioEnabled.value).isTrue()
        assertThat(participantCollector.isActive).isTrue()
        assertThat(participantEmissions).contains(listOf("second-participant"))

        assertThat(call.events).isSameInstanceAs(events)
        val event = mockk<VideoEvent>(relaxed = true)
        call.fireEvent(event)
        runCurrent()
        assertThat(receivedEvents).contains(event)
        assertThat(eventCollector.isActive).isTrue()
        assertThat(client.call("default", "reusable-call")).isSameInstanceAs(call)

        verify(atLeast = 1) { mediaManager.cleanup() }
        verify(atLeast = 1) { mediaManager.prepareForJoin() }

        call.leave()
        runCurrent()
    }

    private data class ResetSnapshot(
        val sessionWasCleared: Boolean,
        val participantsWereCleared: Boolean,
        val statsReportWasCleared: Boolean,
        val statsHistory: List<Int>,
        val publisherLatency: Int,
        val reconnectAttempts: Int,
        val reconnectDeadlineMillis: Int,
        val ringingState: RingingState,
        val sessionId: String,
    )

    private fun reusableClient(scope: CoroutineScope): StreamVideoClient {
        val streamClient = mockk<StreamClient>(relaxed = true)
        every { streamClient.subscribe(any()) } returns Result.success(
            mockk<StreamSubscription>(relaxed = true),
        )
        every { streamClient.connectionState } returns
            MutableStateFlow(StreamConnectionState.Idle)

        val network = mockk<NetworkStateProvider>(relaxed = true) {
            every { isConnected() } returns true
        }
        val coordinator = mockk<CoordinatorConnectionModule>(relaxed = true) {
            every { networkStateProvider } returns network
        }
        val analytics = mockk<AnalyticsModule>(relaxed = true) {
            every { clientEventReporter } returns mockk<ClientEventReporter>(relaxed = true)
        }
        val permissions = mockk<StreamPermissionCheck> {
            every { checkAndroidPermissionsGroup(any(), any()) } returns (true to emptySet())
        }
        val callConfigs = CallServiceConfigRegistry().apply {
            register("default") { setRunCallServiceInForeground(false) }
        }

        return spyk(
            StreamVideoClient(
                context = ApplicationProvider.getApplicationContext<Context>(),
                scope = scope,
                initialUser = User(id = "test-user", role = "user"),
                apiKey = "test-api-key",
                token = "test-token",
                lifecycle = mockk<Lifecycle>(relaxed = true),
                coordinatorConnectionModule = coordinator,
                streamClient = streamClient,
                tokenRepository = mockk<TokenRepository>(relaxed = true),
                streamNotificationManager = mockk<StreamNotificationManager>(relaxed = true),
                enableCallNotificationUpdates = false,
                callServiceConfigRegistry = callConfigs,
                sounds = mockk<Sounds>(relaxed = true),
                vibrationConfig = mockk<RingingCallVibrationConfig>(relaxed = true),
                permissionCheck = permissions,
                analytics = analytics,
            ),
        )
    }

    private fun reusableMediaManager(): MediaManagerImpl {
        val camera = mockk<CameraManager>(relaxed = true) {
            every { status } returns MutableStateFlow(DeviceStatus.NotSelected)
        }
        val microphone = mockk<MicrophoneManager>(relaxed = true) {
            every { status } returns MutableStateFlow(DeviceStatus.NotSelected)
            every { isEnabled } returns MutableStateFlow(false)
            every { devices } returns MutableStateFlow(emptyList())
        }
        val speaker = mockk<SpeakerManager>(relaxed = true) {
            every { status } returns MutableStateFlow(DeviceStatus.NotSelected)
        }
        return mockk(relaxed = true) {
            every { this@mockk.camera } returns camera
            every { this@mockk.microphone } returns microphone
            every { this@mockk.speaker } returns speaker
            every { screenShare } returns mockk(relaxed = true)
        }
    }

    private fun reusableSession(): RtcSession = mockk(relaxed = true) {
        every { publisher } returns MutableStateFlow<Publisher?>(null)
        every { subscriber } returns MutableStateFlow<Subscriber?>(null)
        every { socket.events() } returns MutableSharedFlow()
    }

    private fun joinResponse(call: Call, sessionId: String): JoinCallResponse {
        val now = OffsetDateTime.parse("2026-01-01T00:00:00Z")
        val createdBy = UserResponse(
            createdAt = now,
            id = "test-user",
            language = "en",
            role = "user",
            updatedAt = now,
        )
        return JoinCallResponse(
            created = false,
            duration = "0",
            call = call.toResponse(createdBy).copy(currentSessionId = sessionId),
            credentials = Credentials(
                token = "sfu-token",
                server = SFUResponse(
                    edgeName = "test-edge",
                    url = "https://test-sfu",
                    wsEndpoint = "wss://test-sfu",
                ),
            ),
            statsOptions = StatsOptions(
                enableRtcStats = false,
                reportingIntervalMs = 60_000,
            ),
        )
    }
}
