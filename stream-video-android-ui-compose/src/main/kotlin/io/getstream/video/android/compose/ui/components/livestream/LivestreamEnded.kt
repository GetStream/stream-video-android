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

package io.getstream.video.android.compose.ui.components.livestream

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.android.video.generated.models.CallRecording
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.ui.common.R

@Composable
internal fun BoxScope.LivestreamEndedUi(call: Call) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(
                id = R.string.stream_video_livestreaming_ended,
            ),
            fontSize = 18.sp,
            color = VideoTheme.colors.basePrimary,
        )
        Spacer(modifier = Modifier.height(18.dp))
        LivestreamRecordingsUi(call)
    }
}

@Composable
internal fun LivestreamRecordingsUi(call: Call) {
    var recordings by remember { mutableStateOf(emptyList<CallRecording>()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        call.listRecordings()
            .onSuccess { recordings = it.recordings }
            .onError { recordings = emptyList<CallRecording>() }
    }

    val recordingListItems = recordings.map { RecordingListItem(it.url, it.filename) }

    if (recordingListItems.isNotEmpty()) {
        // Do nothing
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                stringResource(
                    id = R.string.stream_video_livestreaming_watch_recording,
                ),
                fontSize = 16.sp,
                color = VideoTheme.colors.basePrimary,
            )
            recordingListItems.forEach {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier
                        .clickable {
                            try {
                                context.downloadFile(it.url, it.filename)
                            } catch (e: Exception) { }
                        }
                        .align(Alignment.CenterHorizontally),
                    text = it.url,
                    fontSize = 14.sp,
                    color = VideoTheme.colors.baseSecondary,
                )
            }
        }
    }
}

private fun Context.downloadFile(url: String, title: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(title) // Title of the Download Notification
        .setDescription("Downloading") // Description of the Download Notification
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
        ) // Visibility of the download Notification
        .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
        .setAllowedOverRoaming(true) // Set if download is allowed on Roaming network
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)

    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request) // enqueue puts the download request in the queue.
}

internal data class RecordingListItem(val url: String, val filename: String)
