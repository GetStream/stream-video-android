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

package io.getstream.video.android.tooling.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.tooling.extensions.toast

@Composable
internal fun ExceptionTraceScreen(packageName: String, message: String) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
        Modifier
            .verticalScroll(scrollState)
            .background(VideoTheme.colors.baseSheetPrimary)
            .padding(16.dp),
    ) {
        val context: Context = LocalContext.current
        StreamButton(
            style = VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
            text = stringResource(id = R.string.stream_video_tooling_restart_app),
            onClick = {
                val mainActivity = Class.forName(packageName)
                context.startActivity(Intent(context, mainActivity))
            },
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.align(Alignment.CenterStart),
                text = stringResource(id = R.string.stream_video_tooling_exception_log),
                color = VideoTheme.colors.basePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )

            val clipboardManager: ClipboardManager = LocalClipboardManager.current
            Icon(
                modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(message))
                        context.toast(R.string.stream_video_tooling_copy_into_clipboard)
                    },
                imageVector = Icons.Filled.ContentCopy,
                tint = VideoTheme.colors.basePrimary,
                contentDescription = null,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            modifier =
            Modifier
                .border(
                    border = BorderStroke(2.dp, VideoTheme.colors.brandPrimary),
                    shape = RoundedCornerShape(6.dp),
                )
                .padding(12.dp),
            text = message,
            color = VideoTheme.colors.basePrimary,
            fontSize = 14.sp,
        )
    }
}
