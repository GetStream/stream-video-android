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

package io.getstream.video.android.ui.call

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.compose.theme.ParticipantVideoActionsContentParams
import io.getstream.video.android.compose.theme.ParticipantVideoReactionContentParams
import io.getstream.video.android.compose.theme.VideoComponentFactory
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantAction
import io.getstream.video.android.compose.ui.components.call.pinning.ParticipantActions
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import kotlinx.coroutines.launch

/**
 * Demonstrates how to customize the components used throughout the SDK with a
 * [VideoComponentFactory]. The factory is passed to `VideoTheme(componentFactory = ...)` and every
 * built-in component that renders the overridden slots picks it up, so there is no need to pass
 * slot lambdas to each composable.
 */
object DemoComponentFactory : VideoComponentFactory {

    @Composable
    override fun BoxScope.ParticipantVideoReactionContent(
        params: ParticipantVideoReactionContentParams,
    ) {
        CustomReactionContent(
            participant = params.participant,
            style = params.style.copy(
                reactionPosition = Alignment.TopCenter,
                reactionDuration = 5000,
            ),
        )
    }

    @Composable
    override fun BoxScope.ParticipantVideoActionsContent(
        params: ParticipantVideoActionsContentParams,
    ) {
        ParticipantActions(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .testTag("Stream_ParticipantActionsIcon"),
            actions = params.actions + listOf(
                ParticipantAction(
                    icon = io.getstream.video.android.compose.R.drawable.stream_design_ic_user_remove_fill,
                    label = "Kick",
                    condition = { call, participantState ->
                        call.hasCapability(OwnCapability.KickUser) && !participantState.isLocal
                    },
                    action = { call, participantState ->
                        launch {
                            call.kickUser(
                                participantState.userId.value,
                                false,
                            )
                        }
                    },
                ),
            ),
            call = params.call,
            participant = params.participant,
        )
    }
}
