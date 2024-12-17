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

package io.getstream.video.android.ui.closedcaptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import org.openapitools.client.models.CallClosedCaption

/**
 * A set of composables and supporting classes for displaying and customizing closed captions in a call.
 *
 * This collection includes a demo preview, the main container for closed captions,
 * and UI elements for rendering individual captions and caption lists.
 */

/**
 * A preview function for displaying a demo of the closed captions list.
 *
 * Demonstrates how the [ClosedCaptionList] renders multiple captions with default configurations.
 * Useful for testing and visualizing the closed captions UI in isolation.
 */
@Preview
@Composable
public fun ClosedCaptionListDemo() {
    val config = ClosedCaptionsDefaults.config
    ClosedCaptionList(
        arrayListOf(
            ClosedCaptionUiModel("Rahul", "This is closed captions text in Call Content"),
            ClosedCaptionUiModel("Princy", "Hi I am Princy"),
            ClosedCaptionUiModel("Meenu", "Hi I am Meenu, I am from Noida. I am a physiotherapist"),
        ),
        config,
    )
}

/**
 * A composable container for rendering closed captions in a call.
 *
 * This container adapts its behavior based on the environment:
 * - In `LocalInspectionMode`, it displays a static demo of closed captions using [ClosedCaptionListDemo].
 * - During a live call, it listens to the state of the [Call]'s [ClosedCaptionManager] to render
 *   dynamically updated captions.
 *
 * @param call The current [Call] instance, providing state and caption data.
 * @param config A [ClosedCaptionsThemeConfig] defining the styling and positioning of the container.
 */
@Composable
public fun ClosedCaptionsContainer(
    call: Call,
    config: ClosedCaptionsThemeConfig = ClosedCaptionsDefaults.config,
    closedCaptionUiState: ClosedCaptionUiState,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = config.yOffset)
                .padding(horizontal = config.horizontalMargin),

            contentAlignment = Alignment.BottomCenter,
        ) {
            ClosedCaptionListDemo()
        }
    } else {
        val closedCaptions by call.state.closedCaptionManager.closedCaptions
            .collectAsStateWithLifecycle()

        if (closedCaptionUiState == ClosedCaptionUiState.Running && closedCaptions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = config.yOffset)
                    .padding(horizontal = config.horizontalMargin),

                contentAlignment = Alignment.BottomCenter,
            ) {
                ClosedCaptionList(closedCaptions.map { it.toClosedCaptionUiModel(call) }, config)
            }
        }
    }
}

/**
 * A composable function for displaying a list of closed captions.
 *
 * This function uses a [LazyColumn] to display captions with a background, padding,
 * and styling defined in the provided [config]. It limits the number of visible captions
 * to [ClosedCaptionsThemeConfig.maxVisibleCaptions].
 *
 * @param captions The list of [ClosedCaptionUiModel]s to display.
 * @param config A [ClosedCaptionsThemeConfig] defining the layout and styling of the caption list.
 */

@Composable
public fun ClosedCaptionList(
    captions: List<ClosedCaptionUiModel>,
    config: ClosedCaptionsThemeConfig,
) {
    LazyColumn(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = config.boxAlpha),
                shape = RoundedCornerShape(16.dp),
            )
            .fillMaxWidth()
            .padding(config.boxPadding),
        userScrollEnabled = false,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(captions.takeLast(config.maxVisibleCaptions)) { index, item ->
            ClosedCaptionUi(item, index != captions.size - 1, config)
        }
    }
}

/**
 * A composable function for rendering an individual closed caption.
 *
 * Displays the speaker's name and their caption text, with optional semi-transparency for
 * earlier captions (controlled by [semiFade]).
 *
 * @param closedCaptionUiModel The [ClosedCaptionUiModel] containing the speaker and text.
 * @param semiFade Whether to render the caption with reduced opacity.
 * @param config A [ClosedCaptionsThemeConfig] defining the text colors and styling.
 */

@Composable
public fun ClosedCaptionUi(
    closedCaptionUiModel: ClosedCaptionUiModel,
    semiFade: Boolean,
    config: ClosedCaptionsThemeConfig,
) {
    val alpha = if (semiFade) 0.6f else 1f

    val formattedSpeakerText = closedCaptionUiModel.speaker + ":"

    Row(
        modifier = Modifier.alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(formattedSpeakerText, color = config.speakerColor)
        Text(
            closedCaptionUiModel.text,
            color = config.textColor,
            modifier = Modifier.wrapContentWidth(),
        )
    }
}

/**
 * Represents a single closed caption with the speaker's name and their text.
 *
 * @property speaker The name of the speaker for this caption.
 * @property text The text of the caption.
 */
public data class ClosedCaptionUiModel(val speaker: String, val text: String)

/**
 * Converts a [CallClosedCaption] into a [ClosedCaptionUiModel] for UI rendering.
 *
 * Maps the speaker's ID to their name using the participants in the given [Call].
 * If the speaker cannot be identified, the speaker is labeled as "N/A".
 *
 * @param call The [Call] instance containing the list of participants.
 * @return A [ClosedCaptionUiModel] containing the speaker's name and caption text.
 */
public fun CallClosedCaption.toClosedCaptionUiModel(call: Call): ClosedCaptionUiModel {
    val participants = call.state.participants.value
    val user = participants.firstOrNull { it.userId.value == this.speakerId }
    return ClosedCaptionUiModel(
        speaker = user?.userNameOrId?.value ?: "N/A",
        text = this.text,
    )
}
