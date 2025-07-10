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

package io.getstream.video.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.video.android.analytics.FirebaseEvents
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.tooling.util.StreamBuildFlavorUtil
import io.getstream.video.android.ui.AppNavHost
import io.getstream.video.android.ui.AppScreens
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.util.InAppUpdateHelper
import io.getstream.video.android.util.InstallReferrer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var dataStore: StreamUserDataStore
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract callId from intent if available
        val launchIntentCallId = intent.getStringExtra(EXTRA_CALL_ID)

        // Try to read the Google Play install referrer value. We use it to deliver
        // the Call ID from the QR code link.
        @Suppress("KotlinConstantConditions")
        if (StreamBuildFlavorUtil.isProduction) {
            InstallReferrer(this).extractInstallReferrer { callId: String ->
                Log.d("MainActivity", "Call ID: $callId")
                firebaseAnalytics.logEvent(FirebaseEvents.INSTALL_FROM_QR_CODE, null)
                startActivity(DeeplinkingActivity.createIntent(this, callId, true))
                finish()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                InAppUpdateHelper(this@MainActivity).checkForAppUpdates()
            }
        }

        lifecycleScope.launch {
            val isLoggedIn = dataStore.user.firstOrNull() != null

            setContent {
                VideoTheme {
                    AppNavHost(
                        modifier = Modifier
                            .background(VideoTheme.colors.baseSheetPrimary)
                            .systemBarsPadding(),
                        startDestination = if (!isLoggedIn) {
                            AppScreens.Login.routeWithArg(true) // Pass true for autoLogIn
                        } else {
                            AppScreens.CallJoin.route
                        },
                        prefilledCallId = launchIntentCallId,
                    )
                }
            }
        }

        observeIncomingCall()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeIncomingCall() {
        lifecycleScope.launch {
            StreamVideo.instanceState.flatMapLatest { instance ->
                instance?.state?.ringingCall ?: flowOf(null)
            }.collectLatest { call ->
                if (call != null) {
                    lifecycleScope.launch {
                        // Monitor the ringingState on a non-null call
                        call.state.ringingState.collectLatest {
                            if (it is RingingState.Incoming) {
                                startIncomingCallActivity(call)
                            }
                        }
                    }
                }
            }
        }
    }

    fun startIncomingCallActivity(call: Call) {
        val intent = StreamCallActivity.callIntent(
            context = this,
            cid = StreamCallId.fromCallCid(call.cid),
            members = emptyList(),
            leaveWhenLastInCall = true,
            action = NotificationHandler.ACTION_INCOMING_CALL,
            clazz = CallActivity::class.java,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
    }
}

internal const val EXTRA_CALL_ID = "io.getstream.video.android.demoapp.CALL_ID"
