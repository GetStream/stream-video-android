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

package io.getstream.video.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.getstream.video.android.compose.theme.VideoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

private data class LogFileItem(
    val file: File,
    val source: String,
    val lastModifiedAt: Long,
)

@Composable
fun LogFilesScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val internalLogDir = remember(context) {
        File(context.filesDir.path)
    }

    var files by remember { mutableStateOf<List<LogFileItem>>(emptyList()) }

    suspend fun reloadFiles() {
        files = withContext(Dispatchers.IO) {
            buildList {
                addAll(
                    internalLogDir
                        .safeListFiles()
                        .filter { it.name == "internal_0.txt" }
                        .map {
                            LogFileItem(
                                file = it,
                                source = "Internal",
                                lastModifiedAt = it.lastModified(),
                            )
                        },
                )
            }.sortedByDescending { it.lastModifiedAt }
        }
    }

    LaunchedEffect(internalLogDir) {
        reloadFiles()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.baseSheetPrimary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VideoTheme.colors.baseSheetSecondary)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Log Files",
                style = VideoTheme.typography.subtitleM,
                color = VideoTheme.colors.basePrimary,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Clear logs",
                    modifier = Modifier.clickable {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                File(context.filesDir, "internal_0.txt").writeText("")
                            }
                            reloadFiles()
                        }
                    },
                    style = VideoTheme.typography.bodyS,
                    color = VideoTheme.colors.brandPrimary,
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = VideoTheme.colors.baseSheetTertiary,
                            shape = RoundedCornerShape(999.dp),
                        )
                        .clickable(onClick = onClose)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = VideoTheme.colors.basePrimary,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(files, key = { it.file.absolutePath }) { item ->
                LogFileRow(
                    item = item,
                    onClick = { shareFile(context, item.file) },
                )
            }
        }
    }
}

@Composable
private fun LogFileRow(
    item: LogFileItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = item.file.name,
            style = VideoTheme.typography.labelL,
            color = VideoTheme.colors.basePrimary,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogMetaChip(text = item.source)
            LogMetaChip(text = formatTimestamp(item.lastModifiedAt))
            LogMetaChip(text = readableFileSize(item.file.length()))
        }

        Text(
            text = item.file.absolutePath,
            modifier = Modifier.padding(top = 10.dp),
            style = VideoTheme.typography.labelM,
            color = VideoTheme.colors.baseSecondary,
        )
    }
}

@Composable
private fun LogMetaChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                color = VideoTheme.colors.baseSheetTertiary,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = VideoTheme.typography.bodyS,
        color = VideoTheme.colors.basePrimary,
    )
}

private fun File.safeListFiles(): List<File> {
    if (!exists() || !isDirectory) return emptyList()
    return listFiles()
        ?.filter { it.isFile }
        .orEmpty()
}

private fun formatTimestamp(timestamp: Long): String {
    return DateFormat.getDateTimeInstance().format(Date(timestamp))
}

private fun readableFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}

private fun shareFile(context: Context, file: File) {
//    val action = "io.getstream.log.android.SHARE"
//    val serviceIntent = Intent(context, StreamLogFileService::class.java).setAction(action)
//    context.startService(serviceIntent)

    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(intent, "Share log file"),
    )
}
