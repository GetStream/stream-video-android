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
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.core.Call
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.util.config.types.StreamEnvironment

@Composable
public fun ShareCallWithOthers(
    modifier: Modifier = Modifier,
    call: Call,
    clipboardManager: ClipboardManager?,
    env: State<StreamEnvironment?>,
    context: Context,
) {
    val shareUrl = "${env.value?.sharelink}${call.id}"
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    if (isPortrait) {
        ShareSettingsBox(modifier, call, clipboardManager, shareUrl) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareUrl)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    } else {
        ShareSettingsBoxLandscape(modifier, call, clipboardManager, shareUrl) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareUrl)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    }
}

@Composable
public fun ShareSettingsBox(
    modifier: Modifier = Modifier,
    call: Call,
    clipboardManager: ClipboardManager?,
    shareUrl: String,
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
                textAlign = TextAlign.Center,
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
                textOverflow = TextOverflow.Ellipsis,
                icon = Icons.Default.CopyAll,
                style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle().copy(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = VideoTheme.colors.baseSheetTertiary,
                        contentColor = VideoTheme.colors.basePrimary,
                        disabledBackgroundColor = VideoTheme.colors.baseSheetTertiary,
                    ),
                ),
                onClick = {
                    val clipData = ClipData.newPlainText("Call ID", call.id)
                    clipboardManager?.setPrimaryClip(clipData)
                },
            )
            Spacer(modifier = Modifier.size(16.dp))
            JoinCallQRCode(shareUrl = shareUrl)
        }
    }
}

@Composable
public fun ShareSettingsBoxLandscape(
    modifier: Modifier = Modifier,
    call: Call,
    clipboardManager: ClipboardManager?,
    shareUrl: String,
    onShare: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = VideoTheme.colors.baseSheetTertiary,
                shape = VideoTheme.shapes.dialog,
            ),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(VideoTheme.dimens.spacingXl),
            ) {
                JoinCallQRCode(shareUrl = shareUrl)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(VideoTheme.dimens.spacingL)
                    .align(Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Your meeting is live!",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
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
                    textOverflow = TextOverflow.Ellipsis,
                    icon = Icons.Default.CopyAll,
                    style = VideoTheme.styles.buttonStyles.tertiaryButtonStyle().copy(
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = VideoTheme.colors.baseSheetTertiary,
                            contentColor = VideoTheme.colors.basePrimary,
                            disabledBackgroundColor = VideoTheme.colors.baseSheetTertiary,
                        ),
                    ),
                    onClick = {
                        val clipData = ClipData.newPlainText("Call ID", call.id)
                        clipboardManager?.setPrimaryClip(clipData)
                    },
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun JoinCallQRCode(shareUrl: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Black, shape = VideoTheme.shapes.sheet)
            .padding(VideoTheme.dimens.spacingM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QRCode(content = shareUrl, size = 150.dp)
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "Scan the QR code to join from another device",
            style = VideoTheme.typography.labelXS.copy(fontWeight = FontWeight.W400),
        )
    }
}

@Preview
@Composable
private fun ShareSettingsBoxPortraitPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)

    VideoTheme {
        ShareSettingsBox(
            call = previewCall,
            clipboardManager = null,
            shareUrl = "http://test/join/123",
            onShare = {},
        )
    }
}

@Preview(
    name = "Landscape Preview",
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:width=891dp,height=411dp,dpi=420",
)
@Composable
private fun ShareSettingsBoxLandscapePreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)

    VideoTheme {
        ShareSettingsBoxLandscape(
            call = previewCall,
            clipboardManager = null,
            shareUrl = "http://test/join/123",
            onShare = {},
        )
    }
}
