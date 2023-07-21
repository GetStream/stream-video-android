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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.ui.AppNavHost
import io.getstream.video.android.util.InstallReferrer

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Try to read the Google Play install referrer value. We use it to deliver
        // the Call ID from the QR code link.
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.FLAVOR == "production") {
            InstallReferrer(this).extractInstallReferrer { callId: String ->
                startActivity(DeeplinkingActivity.createIntent(this, callId))
            }
        }

        setContent {
            VideoTheme { AppNavHost() }
        }
    }
}
