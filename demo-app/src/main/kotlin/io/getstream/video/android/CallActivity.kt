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

package io.getstream.video.android

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.querysort.QuerySortByField
import io.getstream.result.onSuccessSuspend
import io.getstream.video.android.compose.ui.ComposeStreamCallActivity
import io.getstream.video.android.compose.ui.StreamCallActivityComposeDelegate
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.e2ee.E2EEEventType
import io.getstream.video.android.core.e2ee.StreamEncryptionManager
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.ui.call.CallScreen
import io.getstream.video.android.ui.call.DemoComponentFactory
import io.getstream.video.android.ui.common.StreamActivityUiDelegate
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import io.getstream.video.android.ui.common.util.StreamCallActivityDelicateApi
import io.getstream.video.android.ui.lobby.deriveE2EEKey
import io.getstream.video.android.util.DemoE2eeKeys
import io.getstream.video.android.util.FullScreenCircleProgressBar
import io.getstream.video.android.util.StreamVideoInitHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

@OptIn(StreamCallActivityDelicateApi::class)
class CallActivity : ComposeStreamCallActivity() {

    companion object {
        var USE_CALL_JOIN_INTERCEPTOR = false

        /** Shared E2EE passphrase, forwarded from a deeplink or scanned QR code. */
        const val EXTRA_E2EE_PASSPHRASE = "e2ee_passphrase"

        /**
         * The key index every participant agrees on. A frame carries the index it was encrypted
         * with, so a receiver looking elsewhere fails every decrypt. Kept in step with the lobby
         * and with the web demo, which both use slot 0.
         */
        private const val E2EE_KEY_INDEX = 0

        private const val E2EE_TAG = "CallActivityE2EE"
    }

    override val uiDelegate: StreamActivityUiDelegate<StreamCallActivity> = StreamDemoUiDelegate()
    var observeCallReadyToJoinJob: Job? = null
    var observeRingingJob: Job? = null
    private val previousRingingStates = ConcurrentHashMap.newKeySet<RingingState>()
    override val callJoinInterceptor = DemoCallJoinInterceptor(previousRingingStates)
    private var e2eeManager: StreamEncryptionManager? = null
    private var e2eeCid: String? = null

    /**
     * This code is required to pass the UI-tests (as it hardcodes the configuration)
     * Later, improve the UI-tests
     */
    override fun loadConfigFromIntent(intent: Intent?): StreamCallActivityConfiguration {
        return super.loadConfigFromIntent(intent)
            .copy(closeScreenOnCallEnded = false, canSkipPermissionRationale = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeRingingState()
    }

    private fun observeRingingState() {
        previousRingingStates.clear()
        observeRingingJob?.cancel()
        observeRingingJob = CoroutineScope(Dispatchers.Default).launch {
            StreamVideo.instanceState
                .flatMapLatest { instance ->
                    instance?.state?.ringingCall ?: flowOf(null)
                }.filterNotNull()
                .collectLatest { call ->
                    call.state.ringingState.collectLatest {
                        previousRingingStates.add(it)
                    }
                }
        }
    }

    @StreamCallActivityDelicateApi
    override fun onPreCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        runBlocking {
            if (!StreamVideo.isInstalled) {
                runBlocking { StreamVideoInitHelper.reloadSdk(StreamUserDataStore.instance()) }
            }
        }
        super.onPreCreate(savedInstanceState, persistentState)
    }

    /**
     * Applies the shared key carried by the invite link before the call is joined.
     * [Call.setE2EEManager] is rejected once a session exists, so this is the last point at which a
     * scanned link can still become an encrypted call.
     */
    @StreamCallActivityDelicateApi
    override fun join(
        call: Call,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
    ) {
        intent.getStringExtra(EXTRA_E2EE_PASSPHRASE)
            ?.takeIf { it.isNotBlank() }
            ?.let { enableE2EE(call, it) }
        // Lobby already attached a manager and then died (CLEAR_TASK). Adopt it so leave/finish
        // can dispose it — Call.leave() only detaches, and the 1 Hz PERF_REPORT timer keeps
        // firing until dispose().
        if (e2eeManager == null) {
            DemoE2eeKeys.manager(call.cid)?.let { existing ->
                e2eeManager = existing
                e2eeCid = call.cid
            }
        }
        super.join(call, onSuccess, onError)
    }

    @StreamCallActivityDelicateApi
    override fun leave(
        call: Call,
        callLeaveReason: CallLeaveReason,
        onSuccess: (suspend (Call) -> Unit)?,
        onError: (suspend (Exception) -> Unit)?,
    ) {
        super.leave(
            call,
            callLeaveReason,
            onSuccess = { left ->
                releaseE2EE()
                onSuccess?.invoke(left)
            },
            onError,
        )
    }

    private fun releaseE2EE() {
        e2eeManager?.dispose()
        e2eeManager = null
        e2eeCid?.let { DemoE2eeKeys.forget(it) }
        e2eeCid = null
    }

    private fun enableE2EE(call: Call, passphrase: String) {
        // join() cannot suspend, but the derivation is 100k PBKDF2 iterations - keep it off main.
        val key = runCatching {
            runBlocking(Dispatchers.Default) { deriveE2EEKey(passphrase) }
        }.getOrElse {
            Log.e(E2EE_TAG, "Could not derive the E2EE key from the link", it)
            return
        }
        // EncryptionManager's JNI is registered by libjingle_peerconnection_so's JNI_OnLoad. The
        // lobby gets that for free from its camera preview, but a scanned link joins without ever
        // building a factory first, so without this create() fails with an UnsatisfiedLinkError.
        runCatching { System.loadLibrary("jingle_peerconnection_so") }.onFailure {
            Log.e(E2EE_TAG, "Could not load the WebRTC native library", it)
            return
        }
        val manager = StreamEncryptionManager.create(call.user.id).getOrElse {
            Log.e(E2EE_TAG, "Could not create the E2EE manager", it)
            return
        }
        manager.setEventListener { event ->
            val message = "Native E2EE event: $event"
            when (event.type) {
                E2EEEventType.DECRYPTION_RESUMED -> Log.i(E2EE_TAG, message)
                E2EEEventType.KEY_STATE,
                E2EEEventType.PERF_REPORT,
                -> Log.d(E2EE_TAG, message)
                E2EEEventType.DECRYPTION_FAILED,
                E2EEEventType.DECRYPTION_STALLED,
                E2EEEventType.ENCRYPTION_FAILED,
                E2EEEventType.MISSING_KEY,
                E2EEEventType.UNENCRYPTED_FRAME,
                E2EEEventType.UNSUPPORTED_VERSION,
                E2EEEventType.UNKNOWN,
                -> Log.w(E2EE_TAG, message)
            }
        }
        manager.enablePerformanceReporting(true)
        manager.setSharedKey(E2EE_KEY_INDEX, key)
        val attached = call.setE2EEManager(manager)
        if (attached.isFailure) {
            Log.e(E2EE_TAG, "Could not attach the E2EE manager", attached.exceptionOrNull())
            manager.dispose()
            return
        }
        e2eeManager?.dispose()
        e2eeManager = manager
        // Kept so the in-call share sheet can put the passphrase back on the invite link.
        e2eeCid = call.cid
        DemoE2eeKeys.remember(call.cid, passphrase, manager)
    }

    private class StreamDemoUiDelegate : StreamCallActivityComposeDelegate() {

        override val componentFactory = DemoComponentFactory

        @Composable
        override fun StreamCallActivity.LoadingContent(call: Call) {
            // Use as loading screen.. so the layout is shown.
            FullScreenCircleProgressBar(text = "Connecting...")
        }

        @Composable
        override fun StreamCallActivity.CallDisconnectedContent(call: Call) {
            goBackToMainScreen()
        }

        @Composable
        override fun StreamCallActivity.VideoCallContent(call: Call) {
            CallScreen(
                call = call,
                showDebugOptions = BuildConfig.DEBUG,
                onCallDisconnected = {
                    leave(call)
                    goBackToMainScreen()
                },
                onUserLeaveCall = {
                    leave(call)
                    goBackToMainScreen()
                },
            )

            // step 4 (optional) - chat integration
            val user by ChatClient.instance().clientState.user.collectAsState(initial = null)
            LaunchedEffect(key1 = user) {
                if (user != null) {
                    val channel = ChatClient.instance().channel("videocall", call.id)
                    channel.queryMembers(
                        offset = 0,
                        limit = 10,
                        filter = Filters.neutral(),
                        sort = QuerySortByField(),
                    ).await().onSuccessSuspend { members ->
                        if (members.isNotEmpty()) {
                            channel.addMembers(listOf(user!!.id)).await()
                        } else {
                            channel.create(listOf(user!!.id), emptyMap()).await()
                        }
                    }
                }
            }
        }

        @Composable
        override fun StreamCallActivity.OutgoingCallContent(
            modifier: Modifier,
            call: Call,
            isVideoType: Boolean,
            isShowingHeader: Boolean,
            headerContent: @Composable (ColumnScope.() -> Unit)?,
            detailsContent: @Composable (ColumnScope.(List<MemberState>, Dp) -> Unit)?,
            controlsContent: @Composable (BoxScope.() -> Unit)?,
            onBackPressed: () -> Unit,
            onCallAction: (CallAction) -> Unit,
        ) {
            DemoOutgoingCallContent(
                call = call,
                isVideoType = isVideoType,
                modifier = modifier,
                isShowingHeader = isShowingHeader,
                headerContent = headerContent,
                detailsContent = detailsContent,
                onBackPressed = onBackPressed,
                onCallAction = onCallAction,
            )
        }

        @Composable
        override fun StreamCallActivity.AudioCallContent(call: Call) {
            DemoAudioCallContent(
                call,
                { onCallAction(call, it) },
                { onBackPressed(call) },
            )
        }

        private fun StreamCallActivity.goBackToMainScreen() {
            safeFinish()
        }
    }

    override fun finish() {
        if (isTaskRoot && !isFinishing) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
        super.finish()
        observeCallReadyToJoinJob?.cancel()
        observeRingingJob?.cancel()
        previousRingingStates.clear()
        releaseE2EE()
    }
}
