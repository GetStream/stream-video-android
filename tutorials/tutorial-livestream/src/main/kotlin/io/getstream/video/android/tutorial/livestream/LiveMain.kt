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

package io.getstream.video.android.tutorial.livestream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun LiveMain(
    navController: NavHostController,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.appBackground),
    ) {
        Column {
            Button(
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.primaryAccent,
                    backgroundColor = VideoTheme.colors.primaryAccent,
                ),
                onClick = {
                    navController.navigate(LiveScreens.Host.destination)
                },
            ) {
                Text(text = "host", color = VideoTheme.colors.textHighEmphasis)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.primaryAccent,
                    backgroundColor = VideoTheme.colors.primaryAccent,
                ),
                onClick = {
                    navController.navigate(LiveScreens.Guest.destination)
                },
            ) {
                Text(text = "guest", color = VideoTheme.colors.textHighEmphasis)
            }
        }
    }
}
