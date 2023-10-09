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

@file:OptIn(ExperimentalLayoutApi::class)

package io.getstream.video.android.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.mapper.ReactionMapper
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object ReactionsMenuData {
    const val raiseHand = ":raise-hand:"
    val reactions = listOf(":fireworks:", ":hello:", ":like:", ":hate:", ":smile:", ":heart:")
}

@Composable
internal fun ReactionsMenu(
    call: Call,
    reactionMapper: ReactionMapper,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val modifier = Modifier.background(
        color = Color.White,
        shape = RoundedCornerShape(2.dp),
    ).wrapContentWidth()
    val onEmojiSelected: (emoji: String) -> Unit = {
        sendReaction(scope, call, it, onDismiss)
    }

    Dialog(onDismiss) {
        Card(
            Modifier.wrapContentWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.Center) {
                    ReactionItem(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFF1F4F0),
                                shape = RoundedCornerShape(2.dp),
                            )
                            .fillMaxWidth(),
                        textModifier = Modifier.fillMaxWidth(),
                        reactionMapper = reactionMapper,
                        emojiCode = ReactionsMenuData.raiseHand,
                        reactionText = "Raise hand",
                        onEmojiSelected = onEmojiSelected,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 3,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ReactionItem(
                        modifier = modifier,
                        reactionMapper = reactionMapper,
                        emojiCode = ReactionsMenuData.reactions[0],
                        reactionText = "Fireworks",
                        onEmojiSelected = onEmojiSelected,
                    )
                    ReactionItem(
                        modifier = modifier,
                        reactionMapper = reactionMapper,
                        emojiCode = ReactionsMenuData.reactions[1],
                        reactionText = "Hello",
                        onEmojiSelected = onEmojiSelected,
                    )
                    ReactionItem(
                        modifier = modifier,
                        reactionMapper = reactionMapper,
                        emojiCode = ReactionsMenuData.reactions[2],
                        reactionText = "Like",
                        onEmojiSelected = onEmojiSelected,
                    )
                    ReactionItem(
                        modifier = modifier,
                        reactionMapper = reactionMapper,
                        emojiCode = ReactionsMenuData.reactions[3],
                        reactionText = "Dislike",
                        onEmojiSelected = onEmojiSelected,
                    )
                    ReactionItem(
                        modifier = modifier,
                        reactionMapper = reactionMapper,
                        emojiCode = ReactionsMenuData.reactions[4],
                        reactionText = "Smile",
                        onEmojiSelected = onEmojiSelected,
                    )
                    ReactionItem(
                        modifier = modifier,
                        reactionMapper = reactionMapper,
                        emojiCode = ReactionsMenuData.reactions[5],
                        reactionText = "Heart",
                        onEmojiSelected = onEmojiSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionItem(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    reactionMapper: ReactionMapper,
    emojiCode: String,
    reactionText: String,
    onEmojiSelected: (emoji: String) -> Unit,
) {
    val mappedEmoji = reactionMapper.map(emojiCode)
    Box(
        modifier = modifier
            .clickable {
                onEmojiSelected(emojiCode)
            }
            .padding(2.dp),
    ) {
        Text(
            textAlign = TextAlign.Center,
            modifier = textModifier.padding(12.dp),
            text = "$mappedEmoji $reactionText",
        )
    }
}

private fun sendReaction(scope: CoroutineScope, call: Call, emoji: String, onDismiss: () -> Unit) {
    scope.launch {
        call.sendReaction("default", emoji)
        onDismiss()
    }
}

@Preview
@Composable
private fun ReactionItemPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    ReactionItem(
        emojiCode = ":raise-hand",
        reactionMapper = ReactionMapper.defaultReactionMapper(),
        reactionText = "Raise hand",
    ) {
        // Ignore
    }
}

@Preview
@Composable
private fun ReactionMenuPreview() {
    VideoTheme {
        StreamMockUtils.initializeStreamVideo(LocalContext.current)
        ReactionsMenu(
            call = mockCall,
            reactionMapper = ReactionMapper.defaultReactionMapper(),
            onDismiss = { /* Do nothing */ },
        )
    }
}
