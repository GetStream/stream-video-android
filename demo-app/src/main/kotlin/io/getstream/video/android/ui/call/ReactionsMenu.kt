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

@file:OptIn(ExperimentalLayoutApi::class)

package io.getstream.video.android.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.styling.StyleSize
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.mapper.ReactionMapper
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Default reaction item data
 *
 * @param displayText the text visible on the screen.
 * @param emojiCode the code of the emoji e.g. ":like:"
 * */
private data class ReactionItemData(val displayText: String, val emojiCode: String)

/**
 * Default defined reactions.
 *
 * There is one main reaction, and a list of other reactions. The main reaction is shown on top of the rest.
 */
private object DefaultReactionsMenuData {
    val mainReaction = ReactionItemData("Raise hand", ":raise-hand:")
    val defaultReactions = listOf(
        ReactionItemData("Fireworks", ":fireworks:"),
        ReactionItemData("Like", ":like:"),
        ReactionItemData("Dislike", ":dislike:"),
        ReactionItemData("Smile", ":smile:"),
        ReactionItemData("Heart", ":heart:"),
    )
}

/**
 * Reactions menu. The reaction menu is a dialog displaying the list of reactions found in
 * [DefaultReactionsMenuData].
 *
 * @param call the call object.
 * @param reactionMapper the mapper of reactions to map from emoji code into UTF see: [ReactionMapper]
 * @param onDismiss on dismiss listener.
 */
@Composable
internal fun ReactionsMenu(
    call: Call,
    reactionMapper: ReactionMapper,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val onEmojiSelected: (emoji: String) -> Unit = {
        sendReaction(scope, call, it, onDismiss)
    }
    Column(Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            maxItemsInEachRow = 5,
            verticalArrangement = Arrangement.Center,
        ) {
            DefaultReactionsMenuData.defaultReactions.forEach {
                ReactionItem(
                    reactionMapper = reactionMapper,
                    onEmojiSelected = onEmojiSelected,
                    reaction = it,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.Center) {
            ReactionItem(
                showText = true,
                reactionMapper = reactionMapper,
                reaction = DefaultReactionsMenuData.mainReaction,
                onEmojiSelected = onEmojiSelected,
            )
        }
    }
}

@Composable
private fun ReactionItem(
    reactionMapper: ReactionMapper,
    reaction: ReactionItemData,
    showText: Boolean = false,
    onEmojiSelected: (emoji: String) -> Unit,
) {
    val mappedEmoji = reactionMapper.map(reaction.emojiCode)
    val text = if (showText) {
        "$mappedEmoji ${reaction.displayText}"
    } else {
        mappedEmoji
    }
    val modifier = if (showText) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .requiredWidth(VideoTheme.dimens.componentHeightL)
            .requiredHeight(VideoTheme.dimens.componentHeightL)
    }
    StreamButton(
        modifier = modifier,
        style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle(StyleSize.S),
        text = text,
        onClick = { onEmojiSelected(reaction.emojiCode) },
    )
}

private fun sendReaction(scope: CoroutineScope, call: Call, emoji: String, onDismiss: () -> Unit) {
    scope.launch {
        call.sendReaction("default", emoji)
        onDismiss()
    }
}

@Preview
@Composable
private fun ReactionMenuPreview() {
    VideoTheme {
        StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
        ReactionsMenu(
            call = previewCall,
            reactionMapper = ReactionMapper.defaultReactionMapper(),
            onDismiss = { },
        )
    }
}
