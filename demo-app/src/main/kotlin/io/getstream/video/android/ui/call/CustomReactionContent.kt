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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.Reaction
import io.getstream.video.android.core.model.ReactionState
import kotlinx.coroutines.delay

@Composable
fun BoxScope.CustomReactionContent(
    participant: ParticipantState,
    style: VideoRendererStyle,
) {
    val reactions by participant.reactions.collectAsStateWithLifecycle()
    val reaction = reactions.lastOrNull { it.createdAt + 3000 > System.currentTimeMillis() }
    var currentReaction: Reaction? by remember { mutableStateOf(null) }
    var reactionState: ReactionState by remember { mutableStateOf(ReactionState.Nothing) }

    LaunchedEffect(key1 = reaction) {
        if (reactionState == ReactionState.Nothing) {
            currentReaction?.let { participant.consumeReaction(it) }
            currentReaction = reaction

            // deliberately execute this instead of animation finish listener to remove animation on the screen.
            if (reaction != null) {
                reactionState = ReactionState.Running
                delay(style.reactionDuration * 2 - 50L)
                participant.consumeReaction(reaction)
                currentReaction = null
                reactionState = ReactionState.Nothing
            }
        } else {
            if (currentReaction != null) {
                participant.consumeReaction(currentReaction!!)
                reactionState = ReactionState.Nothing
                currentReaction = null
                delay(style.reactionDuration * 2 - 50L)
            }
        }
    }

    val emojiCode = currentReaction?.response?.emojiCode
    if (currentReaction != null && emojiCode != null) {
        var isEmojiVisible by remember { mutableStateOf(true) }
        val emojiMapper = VideoTheme.reactionMapper
        val emojiText = emojiMapper.map(emojiCode)

        LaunchedEffect(key1 = Unit) {
            delay(style.reactionDuration.toLong())
            isEmojiVisible = false
        }

        if (isEmojiVisible) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = emojiText,
                    modifier = Modifier
                        .padding(top = maxHeight * 0.10f)
                        .align(style.reactionPosition),
                    fontSize = VideoTheme.dimens.componentHeightM.value.sp,
                )
            }
        }
    }
}
