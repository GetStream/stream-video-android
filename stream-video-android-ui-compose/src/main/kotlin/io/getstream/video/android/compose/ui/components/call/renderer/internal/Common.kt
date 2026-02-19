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

package io.getstream.video.android.compose.ui.components.call.renderer.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import kotlinx.coroutines.flow.Flow

/**
 * Provides a [LazyGridState] or [LazyListState] depending on the original parameter supplied.
 * The lazy state will update the [Call.state] with visibility information about the items.
 *
 * Creates a [snapshotFlow] of the visible items state and supplies it to the [Call.state] via
 * [DisposableEffect]
 *
 * @param call the call.
 * @param original the original lazy state. Either [LazyGridState] or [LazyListState]
 * @return the original supplied state.
 */
@Composable
internal fun <T : ScrollableState> lazyStateWithVisibilityNotification(call: Call, original: T): T {
    val snapshotFlow: Flow<List<String>> = when (original) {
        // This duplicate code must be here because while the names are the same, the types are different.
        is LazyGridState -> {
            snapshotFlow {
                original.layoutInfo.visibleItemsInfo.map {
                    it.key as String
                }
            }
        }
        is LazyListState ->
            snapshotFlow {
                original.layoutInfo.visibleItemsInfo.map {
                    it.key as String
                }
            }
        else -> throw UnsupportedOperationException("Wrong initial state.") // Currently
    }
    DisposableEffect(key1 = call, effect = {
        call.state.updateParticipantVisibilityFlow(snapshotFlow)

        onDispose {
            call.state.updateParticipantVisibilityFlow(null)
        }
    })
    return original
}

/**
 * Wraps a content that needs to be spotlighted at top of the screen.
 * Used in [PortraitScreenSharingVideoRenderer] and [SpotlightVideoRenderer].
 *
 * @param modifier the modifier
 * @param background the background color if there is no content or content is loading
 * @param content the content to be displayed.
 */
@Composable
internal fun SpotlightContentPortrait(
    modifier: Modifier,
    background: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

/**
 * Wraps a content that needs to be spotlighted at top of the screen.
 * Used in [PortraitScreenSharingVideoRenderer] and [SpotlightVideoRenderer].
 *
 * @param modifier the modifier
 * @param background the background color if there is no content or content is loading
 * @param content the content to be displayed.
 */
@Composable
internal fun SpotlightContentLandscape(
    modifier: Modifier,
    background: Color,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(end = VideoTheme.dimens.spacingXs),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .fillMaxSize(),
        ) {
            content()
        }
    }
}
