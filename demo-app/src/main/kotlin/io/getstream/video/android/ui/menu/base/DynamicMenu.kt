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

package io.getstream.video.android.ui.menu.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.base.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamToggleButton
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.ui.menu.debugSubmenu
import io.getstream.video.android.ui.menu.defaultStreamMenu

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicMenu(header: (@Composable LazyItemScope.() -> Unit)? = null, items: List<MenuItem>) {
    val history = remember { mutableStateListOf<Pair<String, List<MenuItem>>>() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = VideoTheme.colors.baseSheetPrimary,
                shape = VideoTheme.shapes.dialog,
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = VideoTheme.shapes.sheet,
                    color = VideoTheme.colors.baseSheetPrimary,
                )
                .padding(12.dp),
        ) {
            if (history.isEmpty()) {
                header?.let {
                    item(content = header)
                }
                menuItems(items) {
                    history.add(Pair(it.title, it.items))
                }
            } else {
                val lastContent = history.last()
                stickyHeader {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(VideoTheme.colors.baseSheetPrimary)
                            .fillMaxWidth(),
                    ) {
                        IconButton(onClick = { history.removeLastOrNull() }) {
                            Icon(
                                tint = VideoTheme.colors.basePrimary,
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                        Text(
                            text = lastContent.first,
                            style = VideoTheme.typography.subtitleS,
                            color = VideoTheme.colors.basePrimary,
                        )
                    }
                }

                if (lastContent.second.isEmpty()) {
                    item {
                        Text(
                            textAlign = TextAlign.Center,
                            text = "No items",
                            style = VideoTheme.typography.subtitleS,
                            color = VideoTheme.colors.basePrimary,
                        )
                    }
                } else {
                    menuItems(lastContent.second) {
                        history.add(Pair(it.title, it.items))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.menuItems(items: List<MenuItem>, onNewSubmenu: (SubMenuItem) -> Unit) {
    items(items.size) { index ->
        val item = items[index]
        val highlight = item.highlight
        StreamToggleButton(
            onText = item.title,
            offText = item.title,
            onIcon = item.icon,
            onStyle = VideoTheme.styles.buttonStyles.toggleButtonStyleOn(StyleSize.XS).copy(
                iconStyle = VideoTheme.styles.iconStyles.customColorIconStyle(
                    color = if (highlight) VideoTheme.colors.brandPrimary else VideoTheme.colors.basePrimary,
                ),
            ),
            onClick = {
                val actionItem = item as? ActionMenuItem
                actionItem?.action?.invoke()
                val menuItem = item as? SubMenuItem
                menuItem?.let {
                    onNewSubmenu(it)
                }
            },
        )
    }
}

@Preview
@Composable
private fun DynamicMenuPreview() {
    VideoTheme {
        DynamicMenu(
            items = defaultStreamMenu(
                codecList = emptyList(),
                onCodecSelected = {
                },
                isScreenShareEnabled = false,
                isBackgroundBlurEnabled = true,
                onSwitchMicrophoneClick = { },
                onToggleScreenShare = { },
                onShowCallStats = { },
                onToggleBackgroundBlurClick = { },
                onToggleAudioFilterClick = { },
                onRestartSubscriberIceClick = { },
                onRestartPublisherIceClick = { },
                onKillSfuWsClick = { },
                onSwitchSfuClick = { },
                availableDevices = emptyList(),
                onDeviceSelected = {
                },
            ),
        )
    }
}

@Preview
@Composable
private fun DynamicMenuDebugPreview() {
    VideoTheme {
        DynamicMenu(
            items = debugSubmenu(
                codecList = emptyList(),
                onCodecSelected = {
                },
                onKillSfuWsClick = { },
                onRestartPublisherIceClick = { },
                onRestartSubscriberIceClick = { },
                onToggleAudioFilterClick = { },
                onSwitchSfuClick = { },
            ),
        )
    }
}
