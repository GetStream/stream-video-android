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

package io.getstream.video.android.util

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import io.getstream.video.android.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

class InAppUpdateHelper(private val activity: ComponentActivity) {
    private lateinit var appUpdateResultFlow: Flow<AppUpdateResult>
    private lateinit var appUpdateActivityResultLauncher:
        ActivityResultLauncher<IntentSenderRequest>

    init {
        try {
            appUpdateResultFlow = AppUpdateManagerFactory
                .create(activity)
                .requestUpdateFlow()
                .catch {
                    emit(AppUpdateResult.NotAvailable)
                } // Catch exception when app is not downloaded from Play Store

            appUpdateActivityResultLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { activityResult ->
                handleAppUpdateActivityResult(activityResult.resultCode)
            }
        } catch (e: NullPointerException) {
            Log.e(
                IN_APP_UPDATE_LOG_TAG,
                "Cannot initialize InAppUpdateHelper. Make sure it's instantiated in or after Activity.onCreate().",
            )
        }
    }

    suspend fun checkForAppUpdates() {
        if (::appUpdateResultFlow.isInitialized) {
            appUpdateResultFlow.collectLatest { result ->
                when (result) {
                    is AppUpdateResult.Available -> {
                        Log.d(IN_APP_UPDATE_LOG_TAG, "Update available")
                        result.startImmediateUpdate(appUpdateActivityResultLauncher)
                    }
                    is AppUpdateResult.InProgress -> Log.d(
                        IN_APP_UPDATE_LOG_TAG,
                        "Update in progress",
                    )
                    is AppUpdateResult.Downloaded -> Log.d(
                        IN_APP_UPDATE_LOG_TAG,
                        "Update downloaded",
                    )
                    is AppUpdateResult.NotAvailable -> Log.i(
                        IN_APP_UPDATE_LOG_TAG,
                        "None available",
                    )
                }
            }
        }
    }

    private fun handleAppUpdateActivityResult(resultCode: Int) {
        when (resultCode) {
            // For immediate updates, you might not receive RESULT_OK because
            // the update should already be finished by the time control is given back to your app.
            Activity.RESULT_OK -> {
                Log.d(IN_APP_UPDATE_LOG_TAG, "Update successful")
                showToast(activity.getString(R.string.in_app_update_successful))
            }
            Activity.RESULT_CANCELED -> {
                Log.d(IN_APP_UPDATE_LOG_TAG, "Update canceled")
                showToast(activity.getString(R.string.in_app_update_canceled))
            }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                Log.d(IN_APP_UPDATE_LOG_TAG, "Update failed")
                showToast(activity.getString(R.string.in_app_update_failed))
            }
        }
    }

    private fun showToast(userMessage: String) =
        Toast.makeText(activity, userMessage, Toast.LENGTH_LONG).show()
}

private const val IN_APP_UPDATE_LOG_TAG = "In-app update"
