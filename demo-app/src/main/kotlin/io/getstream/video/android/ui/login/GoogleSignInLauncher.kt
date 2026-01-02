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

package io.getstream.video.android.ui.login

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

@Composable
fun rememberLauncherForGoogleSignInActivityResult(
    onSignInSuccess: (email: String) -> Unit,
    onSignInFailed: () -> Unit,
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    return rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        Log.d("Google Sign In", "Checking activity result")

        if (result.resultCode != Activity.RESULT_OK) {
            Log.d("Google Sign In", "Failed with result not OK: ${result.resultCode}")
            onSignInFailed()
        } else {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(
                result.data,
            )
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account.email

                if (email != null) {
                    Log.d("Google Sign In", "Successful: $email")
                    onSignInSuccess(email)
                } else {
                    Log.d("Google Sign In", "Failed with null email")
                    onSignInFailed()
                }
            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.d("Google Sign In", "Failed with ApiException: ${e.statusCode}")
                onSignInFailed()
            }
        }
    }
}
