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

package io.getstream.video.android.tooling.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme

@Composable
internal fun StreamPrimaryButton(
    modifier: Modifier = Modifier,
    @StringRes text: Int = -1,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentColor: Color? = null,
    disabledContentColor: Color? = null,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    Button(
        modifier = modifier.fillMaxWidth().padding(16.dp).heightIn(min = 54.dp),
        shape = RoundedCornerShape(8.dp),
        enabled = enabled,
        colors =
        ButtonDefaults.buttonColors(
            contentColor = contentColor ?: VideoTheme.colors.primaryAccent,
            backgroundColor = contentColor ?: VideoTheme.colors.primaryAccent,
            disabledContentColor = disabledContentColor ?: VideoTheme.colors.disabled,
        ),
        onClick = onClick,
        content = content
            ?: {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = text),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            },
    )
}
