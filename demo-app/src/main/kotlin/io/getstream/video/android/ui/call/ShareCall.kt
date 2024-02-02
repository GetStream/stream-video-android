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

package io.getstream.video.android.ui.call

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.core.Call
import io.getstream.video.android.util.config.types.StreamEnvironment

@Composable
public fun ShareCallWithOthers(
    modifier: Modifier = Modifier,
    call: Call,
    clipboardManager: ClipboardManager?,
    env: State<StreamEnvironment?>,
    context: Context,
) {
    ShareSettingsBox(modifier, call, clipboardManager) {
        val link = if (env.value?.env == "demo") {
            "https://getstream.io/video/demos/join/${call.id}"
        } else {
            "https://${env.value?.env}.getstream.io/video/demos/join/${call.id}"
        }
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, link)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}

@Composable
public fun ShareSettingsBox(
    modifier: Modifier = Modifier,
    call: Call,
    clipboardManager: ClipboardManager?,
    onShare: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = VideoTheme.colors.baseSheetTertiary,
                shape = VideoTheme.shapes.dialog,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VideoTheme.dimens.spacingL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StreamButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Share link with others",
                icon = Icons.Default.PersonAddAlt1,
                style = VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
            ) {
                onShare(call.id)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Or share this call ID with the \u2028others you want in the meeting",
                style = VideoTheme.typography.bodyM,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row {
                    Text(
                        text = "Call ID:",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight(500),
                            color = Color.White,
                        ),
                    )
                    Text(
                        modifier = Modifier.width(200.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = call.id,
                        softWrap = false,
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.W400,
                            color = VideoTheme.colors.brandCyan,
                        ),
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        val clipData = ClipData.newPlainText("Call ID", call.id)
                        clipboardManager?.setPrimaryClip(clipData)
                    },
                ) {
                    Icon(
                        tint = Color.White,
                        imageVector = Icons.Default.CopyAll,
                        contentDescription = "Copy",
                    )
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}
