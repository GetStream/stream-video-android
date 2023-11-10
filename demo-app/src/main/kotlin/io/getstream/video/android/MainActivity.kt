/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.video.android.analytics.FirebaseEvents
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.ui.AppNavHost
import io.getstream.video.android.ui.AppScreens
import io.getstream.video.android.util.InstallReferrer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var dataStore: StreamUserDataStore
    private val viewModel: MainActivityViewModel by viewModels()
    private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Try to read the Google Play install referrer value. We use it to deliver
        // the Call ID from the QR code link.
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.FLAVOR == "production") {
            InstallReferrer(this).extractInstallReferrer { callId: String ->
                firebaseAnalytics.logEvent(FirebaseEvents.INSTALL_FROM_QR_CODE, null)
                startActivity(DeeplinkingActivity.createIntent(this, callId, true))
            }
        }

        lifecycleScope.launch {
            val isLoggedIn = dataStore.user.firstOrNull() != null

            setContent {
                VideoTheme {
                    AppNavHost(
                        startDestination = if (!isLoggedIn) {
                            AppScreens.Login.routeWithArg(true) // Pass true for autoLogIn
                        } else {
                            AppScreens.CallJoin.route
                        },
                    )
                }
            }

            checkForAppUpdates(viewModel.appUpdateResultFlow)
        }
    }

    private suspend fun checkForAppUpdates(appUpdateResultFlow: Flow<AppUpdateResult>) {
        appUpdateResultFlow.collectLatest { result ->
            when (result) {
                is AppUpdateResult.Available -> {
                    Log.d(IN_APP_UPDATE_LOG_TAG, "Update available")
                    result.startImmediateUpdate(appUpdateActivityResultLauncher)
                }
                is AppUpdateResult.InProgress -> Log.d(IN_APP_UPDATE_LOG_TAG, "Update in progress")
                is AppUpdateResult.Downloaded -> Log.d(IN_APP_UPDATE_LOG_TAG, "Update downloaded")
                is AppUpdateResult.NotAvailable -> Log.i(IN_APP_UPDATE_LOG_TAG, "None available")
            }
        }
    }

    private val appUpdateActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        handleAppUpdateActivityResult(activityResult.resultCode)
    }

    private fun handleAppUpdateActivityResult(resultCode: Int) {
        when (resultCode) {
            // For immediate updates, you might not receive RESULT_OK because
            // the update should already be finished by the time control is given back to your app.
            RESULT_OK -> {
                Log.d(IN_APP_UPDATE_LOG_TAG, "Update successful")
                showToast(getString(R.string.in_app_update_successful))
            }
            RESULT_CANCELED -> {
                Log.d(IN_APP_UPDATE_LOG_TAG, "Update canceled")
                showToast(getString(R.string.in_app_update_canceled))
            }
            RESULT_IN_APP_UPDATE_FAILED -> {
                Log.d(IN_APP_UPDATE_LOG_TAG, "Update failed")
                showToast(getString(R.string.in_app_update_failed))
            }
        }
    }

    private fun showToast(userMessage: String) =
        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show()
}

private const val IN_APP_UPDATE_LOG_TAG = "In-app update"
