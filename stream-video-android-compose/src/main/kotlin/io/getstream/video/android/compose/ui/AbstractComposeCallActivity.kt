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

package io.getstream.video.android.compose.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.CallContainer
import io.getstream.video.android.compose.ui.components.call.activecall.DefaultPictureInPictureContent
import io.getstream.video.android.core.Call


public abstract class AbstractComposeCallActivity : AbstractCallActivity() {

    override fun setupUi() {
        setContent(content = buildContent())
    }

    protected open fun buildContent(): (@Composable () -> Unit) = {
        VideoTheme {
            CallContainer(
                modifier = Modifier.background(color = VideoTheme.colors.appBackground),
                viewModel = callViewModel,
                //onCallAction = ::handleCallAction,
                onBackPressed = ::handleBackPressed,
                pictureInPictureContent = { PictureInPictureContent(call = it) }
            )
        }
    }

    @Composable
    protected open fun PictureInPictureContent(call: Call) {
        DefaultPictureInPictureContent(call = call)
    }
}
