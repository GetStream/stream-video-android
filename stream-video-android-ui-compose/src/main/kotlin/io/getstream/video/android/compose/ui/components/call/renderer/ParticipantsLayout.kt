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

package io.getstream.video.android.compose.ui.components.call.renderer

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatarBackground
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall

public enum class LayoutType {
    /** Automatically choose between Grid and Spotlight based on pinned participants and dominant speaker. */
    DYNAMIC,

    /** Force a spotlight view, showing the dominant speaker or the first speaker in the list. */
    SPOTLIGHT,

    /** Always show a grid layout, regardless of pinned participants. */
    GRID,
}

/**
 * Renders all the participants, based on the number of people in a call and the call state.
 * Also takes into account if there are any screen sharing sessions active and adjusts the UI
 * accordingly.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param style Defined properties for styling a single video call track.
 * @param layoutType The type of layout. [LayoutType], default - [LayoutType.DYNAMIC]
 * @param videoRenderer A single video renderer renders each individual participant.
 * @param screenSharingFallbackContent Fallback content to show when the screen sharing session is loading or not available.
 */
@Composable
public fun ParticipantsLayout(
    call: Call,
    modifier: Modifier = Modifier,
    style: VideoRendererStyle = RegularVideoRendererStyle(),
    layoutType: LayoutType = LayoutType.DYNAMIC,
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle,
        )
    },
    floatingVideoRenderer: @Composable (BoxScope.(call: Call, IntSize) -> Unit)? = null,
    screenSharingFallbackContent: @Composable (ScreenSharingSession) -> Unit = {
        val userName by it.participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by it.participant.image.collectAsStateWithLifecycle()
        UserAvatarBackground(userImage = userImage, userName = userName)
    },
) {
    val screenSharingSession = call.state.screenSharingSession.collectAsStateWithLifecycle()
    val screenSharing = screenSharingSession.value
    val pinnedParticipants by call.state.pinnedParticipants.collectAsStateWithLifecycle()
    val showSpotlight by remember(key1 = pinnedParticipants, key2 = layoutType) {
        derivedStateOf {
            when (layoutType) {
                LayoutType.GRID -> false
                LayoutType.SPOTLIGHT -> true
                else -> pinnedParticipants.isNotEmpty()
            }
        }
    }

    if (screenSharing == null || screenSharing.participant.isLocal) {
        if (showSpotlight) {
            ParticipantsSpotlight(
                call = call,
                modifier = modifier,
                style = SpotlightVideoRendererStyle().copy(
                    isFocused = style.isFocused,
                    isShowingReactions = style.isShowingReactions,
                    labelPosition = style.labelPosition,
                ),
                videoRenderer = videoRenderer,
            )
        } else {
            ParticipantsRegularGrid(
                call = call,
                modifier = modifier.testTag("Stream_GridView"),
                style = style,
                videoRenderer = videoRenderer,
                floatingVideoRenderer = floatingVideoRenderer,
            )
        }
    } else {
        ParticipantsScreenSharing(
            call = call,
            modifier = modifier.testTag("Stream_ParticipantsScreenSharingView"),
            session = screenSharing,
            style = ScreenSharingVideoRendererStyle().copy(
                isFocused = style.isFocused,
                isShowingReactions = style.isShowingReactions,
                labelPosition = style.labelPosition,
            ),
            videoRenderer = videoRenderer,
            screenSharingFallbackContent = screenSharingFallbackContent,
        )
    }
}

@Preview
@Composable
private fun CallVideoRendererPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantsLayout(
            call = previewCall,
            modifier = Modifier.fillMaxWidth(),
            layoutType = LayoutType.GRID,
        )
    }
}
