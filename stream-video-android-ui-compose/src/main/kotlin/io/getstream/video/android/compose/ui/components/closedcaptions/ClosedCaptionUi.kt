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

package io.getstream.video.android.compose.ui.components.closedcaptions

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import org.openapitools.client.models.CallClosedCaption

private val Y_OFFSET = -100.dp
private val CLOSED_CAPTIONS_BOX_HORIZONTAL_MARGIN = 16.dp
private const val CLOSED_CAPTIONS_BOX_ALPHA = 0.5f
private val CLOSED_CAPTIONS_BOX_PADDING = 12.dp
private val SPEAKER_COLOR = Color.Yellow
private val TEXT_COLOR = Color.White


@Preview
@Composable
public fun ClosedCaptionListDemo() {
    ClosedCaptionList(
        arrayListOf(
            ClosedCaptionUiModel("Rahul", "This is closed captions text in Call Content"),
            ClosedCaptionUiModel("Princy", "Hi I am Princy"),
            ClosedCaptionUiModel("Meenu", "Hi I am Meenu, I am from Noida. I am a physiotherapist")))
}

@Composable
public fun ClosedCaptionsContainer(call: Call) {
    val closedCaptions by call.state.captionManager.closedCaptions
        .collectAsStateWithLifecycle()
    if (closedCaptions.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = Y_OFFSET)
                .padding(horizontal = CLOSED_CAPTIONS_BOX_HORIZONTAL_MARGIN),

            contentAlignment = Alignment.BottomCenter,
        ) {
            ClosedCaptionList(closedCaptions.map { it.toClosedCaptionUiModel(call) })
        }
    }
}

@Composable
public fun ClosedCaptionList(captions: List<ClosedCaptionUiModel>, max: Int = 3) {
    LazyColumn(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = CLOSED_CAPTIONS_BOX_ALPHA),
                shape = RoundedCornerShape(16.dp)
            )
            .fillMaxWidth()
            .padding(CLOSED_CAPTIONS_BOX_PADDING),
        userScrollEnabled = false,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(captions.takeLast(max)) { index, item ->
            ClosedCaptionUi(item, index != captions.size-1)
        }
    }
}

@Composable
public fun ClosedCaptionUi(closedCaptionUiModel: ClosedCaptionUiModel, semiFade: Boolean) {
    var alpha = 1f
    if (semiFade) {
        alpha = 0.6f
    }
    val formattedSpeakerText = closedCaptionUiModel.speaker + ":"

    Row(
        modifier = Modifier.alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(formattedSpeakerText, color = SPEAKER_COLOR)
        Text(
            closedCaptionUiModel.text,
            color = TEXT_COLOR,
            modifier = Modifier.wrapContentWidth()
        )
    }
}

public data class ClosedCaptionUiModel(val speaker:String, val text: String)

public fun CallClosedCaption.toClosedCaptionUiModel(call: Call): ClosedCaptionUiModel {
    val participants = call.state.participants.value
    val user = participants.firstOrNull { it.userId.value == this.speakerId }
    if (user != null) {
        return ClosedCaptionUiModel(user.userNameOrId.value, this.text)
    }
    return ClosedCaptionUiModel("N/A", this.text)
}