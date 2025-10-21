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

package io.getstream.video.android.tutorial.livestream

import android.app.PictureInPictureParams
import android.app.PictureInPictureUiState
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import io.getstream.video.android.compose.theme.VideoTheme

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VideoTheme { LiveNavHost() }
        }
    }

    override fun onPictureInPictureRequested(): Boolean {
        val v = super.onPictureInPictureRequested()
        Log.d(TAG, "[onPictureInPictureRequested] $v")
        return v
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onPictureInPictureUiStateChanged(pipState: PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)
        Log.d(
            TAG,
            "[onPictureInPictureUiStateChanged] isStashed: ${pipState.isStashed}, isTransitioningToPip: ${pipState.isTransitioningToPip}",
        )
    }

    override fun enterPictureInPictureMode(params: PictureInPictureParams): Boolean {
        val v = super.enterPictureInPictureMode(params)
        params.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isAutoEnterEnabled
            }
        }
        Log.d(TAG, "[enterPictureInPictureMode] params:$params, v:$v")
        return v
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d(
            TAG,
            "[onPictureInPictureModeChanged] isInPictureInPictureMode:$isInPictureInPictureMode, newConfig:$newConfig",
        )
    }

    override fun enterPictureInPictureMode() {
        super.enterPictureInPictureMode()
        Log.d(TAG, "[enterPictureInPictureMode]")
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, "[onUserLeaveHint]")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "[onPause]")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "[onPause]")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[onResume]")
    }
}
