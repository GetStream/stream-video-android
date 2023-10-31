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

package io.getstream.video.android.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
fun StreamImageButton(
    modifier: Modifier,
    enabled: Boolean = true,
    @DrawableRes imageRes: Int,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = VideoTheme.colors.primaryAccent,
            contentColor = VideoTheme.colors.primaryAccent,
            disabledBackgroundColor = Colors.description,
            disabledContentColor = Colors.description,
        ),
        onClick = onClick,
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
        )
    }
}
