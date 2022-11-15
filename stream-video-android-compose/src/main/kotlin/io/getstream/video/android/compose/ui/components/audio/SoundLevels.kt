/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Used to indicate the sound levels of a given participant.
 *
 * @param audioLevel The active level of audio of the participant.
 */
@Composable
public fun SoundLevels(audioLevel: Float) {
    val levels = remember { mutableStateOf(listOf<Float>()) }
    val activeColor = VideoTheme.colors.primaryAccent
    val inactiveColor = Color.White

    calculateLevels(levels, audioLevel) // TODO - check the state and result of this

    Canvas(
        modifier = Modifier.size(
            height = 16.dp,
            width = 24.dp
        ),
        onDraw = {
            val values = levels.value

            val first = values.getOrNull(0) ?: -1f
            drawAudioLevel(first, activeColor, inactiveColor, 0f)

            val second = values.getOrNull(1) ?: -1f
            drawAudioLevel(second, activeColor, inactiveColor, 15f)

            val third = values.getOrNull(2) ?: -1f
            drawAudioLevel(third, activeColor, inactiveColor, 30f)
        }
    )
}

private fun DrawScope.drawAudioLevel(
    audioLevel: Float,
    activeColor: Color,
    inactiveColor: Color,
    offsetX: Float
) {
    val offsetY = when {
        audioLevel < 0f -> 25f
        audioLevel in 0f..0.5f -> 25f
        audioLevel > 0.5f -> 0f
        else -> 25f
    }

    drawLine(
        color = if (audioLevel <= 0f) inactiveColor else activeColor,
        end = Offset(offsetX, offsetY),
        start = Offset(offsetX, 50f),
        strokeWidth = 6f
    )
}

private fun calculateLevels(levels: MutableState<List<Float>>, currentLevel: Float) {
    val list = levels.value.toMutableList()

    if (list.size < 3) {
        list.add(currentLevel)

        levels.value = list
    } else {
        val lastTwo = list.takeLast(2)

        levels.value = lastTwo + currentLevel
    }
}
