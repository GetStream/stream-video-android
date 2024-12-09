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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.openapitools.client.models.CallClosedCaption

@Composable
public fun ClosedCaptionUi(speaker: String, text: String) {
    Row {
        Text(speaker)
        Text(text)
    }
}

@Composable
public fun ClosedCaptionList(captions: List<CallClosedCaption>, max: Int = 3) {
    LazyColumn {
        itemsIndexed(captions.takeLast(max)) { index, item ->
            ClosedCaptionUi(item.speakerId, item.text)
        }
    }
}
