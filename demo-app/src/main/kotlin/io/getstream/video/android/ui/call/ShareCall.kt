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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.core.Call
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList
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
        val link = "${env.value?.sharelink}${call.id}"
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
        modifier = modifier
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
            Text(
                text = "Your meeting is live!",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left,
                style = VideoTheme.typography.titleS.copy(fontSize = 20.sp),
            )
            Spacer(modifier = Modifier.size(16.dp))
            StreamButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                text = "Add others",
                icon = Icons.Default.PersonAddAlt1,
                style = VideoTheme.styles.buttonStyles.secondaryButtonStyle(),
                onClick = { onShare(call.id) },
            )
            Spacer(modifier = Modifier.size(16.dp))
            StreamButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                text = "Call ID: ${call.id}",
                icon = Icons.Default.CopyAll,
                style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle().copy(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = VideoTheme.colors.baseSheetTertiary,
                        contentColor = VideoTheme.colors.basePrimary,
                        disabledBackgroundColor = VideoTheme.colors.baseSheetTertiary,
                    )
                ),
                onClick = {
                    val clipData = ClipData.newPlainText("Call ID", call.id)
                    clipboardManager?.setPrimaryClip(clipData)
                },
            )
            Spacer(modifier = Modifier.size(16.dp))
            JoinCallQRCode()
        }
    }
}

@Composable
private fun JoinCallQRCode() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Black, shape = VideoTheme.shapes.sheet)
            .padding(VideoTheme.dimens.spacingM),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QRCode(content = "Join Call QR Code")
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "Scan the QR code to join from another device.",
            style = VideoTheme.typography.subtitleS.copy(color = VideoTheme.colors.basePrimary),
        )
    }
}

@Preview
@Composable
private fun ShareSettingsBoxPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)

    VideoTheme {
        ShareSettingsBox(
            call = previewCall,
            clipboardManager = null,
            onShare = {},
        )
    }
}
