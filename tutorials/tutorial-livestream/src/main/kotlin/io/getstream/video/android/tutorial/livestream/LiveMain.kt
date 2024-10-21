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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.base.styling.StreamTextFieldStyles
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize

@Composable
fun LiveMain(
    navController: NavHostController,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var callId by remember { mutableStateOf(TextFieldValue("dE8AsD5Qxqrt")) }
            StreamTextField(
                modifier = Modifier.width(300.dp),
                value = callId,
                placeholder = "Call Id (required)",
                onValueChange = {
                    callId = it
                },
            )

            Spacer(modifier = Modifier.height(44.dp))

            Button(
                modifier = Modifier
                    .width(300.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.brandPrimary,
                    backgroundColor = VideoTheme.colors.brandPrimary,
                ),
                onClick = {
                    navController.navigate(LiveScreens.Host.destination(callId.text))
                },
            ) {
                Text(text = "host", color = VideoTheme.colors.basePrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                modifier = Modifier
                    .width(300.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = VideoTheme.colors.brandPrimary,
                    backgroundColor = VideoTheme.colors.brandPrimary,
                ),
                onClick = {
                    navController.navigate(LiveScreens.Guest.destination(callId.text))
                },
            ) {
                Text(text = "guest", color = VideoTheme.colors.basePrimary)
            }
        }
    }
}