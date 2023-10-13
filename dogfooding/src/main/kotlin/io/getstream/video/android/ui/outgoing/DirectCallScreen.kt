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

package io.getstream.video.android.ui.outgoing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.R
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.ui.theme.StreamImageButton

@Composable
fun DirectCallScreen(
    viewModel: DirectCallViewModel = hiltViewModel(),
    navigateToRingCall: (callId: String, membersList: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) { viewModel.getAllUsers() }

    VideoTheme {
        Column {
//            AppBar()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center),
                        color = VideoTheme.colors.primaryAccent
                    )
                } else {
                    UserList(
                        entries = uiState.users,
                        onUserClick = { clickedIndex -> viewModel.toggleUserSelection(clickedIndex) }
                    )
                    StreamImageButton(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        enabled = uiState.users.any { it.isSelected },
                        imageRes = R.drawable.stream_video_ic_call,
                        onClick = {
                            navigateToRingCall(
                                "123",
                                uiState.users.filter { it.isSelected }
                                    .joinToString(separator = ",") { it.user.id }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            color = Color.White,
            text = "Direct Call",
            maxLines = 1,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun UserList(entries: List<UserUiState>, onUserClick: (Int) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        entries.forEachIndexed { index, entry ->
            UserRow(
                index = index,
                name = entry.user.name,
                avatarUrl = entry.user.avatarUrl,
                isSelected = entry.isSelected,
                onClick = { onUserClick(index) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun UserRow(index: Int, name: String, avatarUrl: String?, isSelected: Boolean, onClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(index) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(avatarUrl)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 16.sp,
            )
        }
        RadioButton(
            selected = isSelected,
            modifier = Modifier.size(20.dp),
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = VideoTheme.colors.primaryAccent,
                unselectedColor = Color.LightGray,
            )
        )
    }

}

@Composable
private fun UserAvatar(url: String?) {
    NetworkImage(
        url = url ?: "",
        modifier = Modifier
            .size(50.dp)
            .clip(shape = CircleShape),
        crossfadeMillis = 200,
        alpha = 0.8f,
        error = ColorPainter(color = Color.DarkGray),
        fallback = ColorPainter(color = Color.DarkGray),
        placeholder = ColorPainter(color = Color.DarkGray)
    )
}

@Composable
private fun NetworkImage(
    url: String,
    modifier: Modifier = Modifier,
    crossfadeMillis: Int = 0,
    alpha: Float = 1f,
    error: Painter? = null,
    fallback: Painter? = null,
    placeholder: Painter? = null,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .size(Size.ORIGINAL)
            .crossfade(durationMillis = crossfadeMillis)
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alpha = alpha,
        error = error,
        fallback = fallback,
        placeholder = placeholder
    )
}

@Preview
@Composable
private fun DebugCallScreenPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        DirectCallScreen(
            navigateToRingCall = { _, _ -> },
        )
    }
}
