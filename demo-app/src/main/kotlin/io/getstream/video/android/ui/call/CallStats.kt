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

package io.getstream.video.android.ui.call

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.core.Call
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallStatsDialog(call: Call, onDismiss: () -> Unit) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Black,
                ),
        ) {
            CallStats(call = call)
        }
    }
}

@Composable
fun CallStats(call: Call) {
    val stats by call.statLatencyHistory.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.Black),
    ) {
        UserAndCallId(call = call)
        if (LocalInspectionMode.current) {
            LineChartPreview()
        } else {
            LineChart(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = stats.map {
                    it.toFloat()
                },
                dates = emptyList(),
            )
        }
    }
}

@Composable
fun UserAndCallId(call: Call) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            modifier = Modifier.size(44.dp),
            userName = call.user.userNameOrId,
            userImage = call.user.image,
        )
        Text(
            text = "Call ID:\n${call.sessionId}",
            style = TextStyle(
                fontSize = 10.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.W400,
                color = VideoTheme.colors.textLowEmphasis,
            ),
        )
    }
}

@Composable
fun LineChart(
    modifier: Modifier = Modifier,
    data: List<Float>,
    dates: List<Date>,
    xAxisFormat: String = "HH:mm:ss",
    yAxisSuffix: String = "ms",
    lineColor: Color = Color(0xFF00E2A1),
    axisColor: Color = Color.Transparent,
    gridColor: Color = Color(0xFF4D535F),
    labelTextColor: Color = Color(0xFF75808A),
) {
    Canvas(modifier = modifier.padding(start = 8.dp, end = 44.dp, top = 16.dp, bottom = 24.dp)) {
        val maxDataValue = data.maxOrNull() ?: 100f
        val thirdOfMax = maxDataValue / 3
        val maxValue = if (maxDataValue > 100) {
            maxDataValue + thirdOfMax
        } else {
            100f
        }
        val maxValueHeight = size.height.minus(16) // Leave some space for labels
        val step = maxValueHeight / maxValue

        // Draw y-axis
        drawLine(
            color = axisColor,
            start = Offset(size.width + 10f, -40f),
            end = Offset(size.width + 10f, size.height.plus(10)),
            strokeWidth = 2.dp.toPx(),
        )

        // Draw x-axis
        drawLine(
            color = axisColor,
            start = Offset(40f, size.height.plus(10)),
            end = Offset(size.width.plus(10), size.height.plus(10)),
            strokeWidth = 2.dp.toPx(),
        )

        // Draw y-axis labels and grid lines
        val labelStep = maxValue / 5
        val labelYStep = maxValueHeight / 5
        for (i in 0..5) {
            val label = (i * labelStep).toInt().toString() + " $yAxisSuffix"
            drawLine(
                color = gridColor,
                start = Offset(0f, size.height - (i * labelYStep).minus(10)),
                end = Offset(size.width - 10, size.height - (i * labelYStep).minus(10)),
                strokeWidth = 1.dp.toPx(),
            )
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    label,
                    size.width,
                    size.height - i * labelYStep + 8.dp.toPx(),
                    Paint().apply {
                        color = labelTextColor.toArgb()
                        textSize = 12.sp.toPx()
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT
                    },
                )
            }
        }

        // Draw x-axis labels and grid lines
        for (i in dates.indices) {
            val x = (i * ((size.width.minus(30)) / (data.size - 1))) + 40f
            val label = SimpleDateFormat(xAxisFormat, Locale.getDefault()).format(dates[i])
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height.minus(20)),
                strokeWidth = 1.dp.toPx(),
            )
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    label,
                    x,
                    size.height + 20.dp.toPx(),
                    Paint().apply {
                        color = labelTextColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 12.sp.toPx()
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT
                    },
                )
            }
        }

        // Draw line chart
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = (index * ((size.width.minus(50)) / (data.size - 1))) + 40f
            val y = size.height - (value * step)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = ((index - 1) * ((size.width.minus(50)) / (data.size - 1))) + 40f
                val prevY = size.height - (data[index - 1] * step)
                val cpx1 = prevX + ((x - prevX) / 2)
                val cpy1 = prevY
                val cpx2 = prevX + ((x - prevX) / 2)
                val cpy2 = y
                path.cubicTo(cpx1, cpy1, cpx2, cpy2, x, y)
            }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
@Preview
fun CallStatsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallStats(call = previewCall)
    }
}

@Composable
@Preview
fun LineChartPreview() {
    val dates = listOf(
        Date(System.currentTimeMillis() - 6 * 24 * 60 * 60 * 1000), // Three days ago
        Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000), // Two days ago
        Date(System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000), // One day ago
        Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000), // Three days ago
        Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000), // Two days ago
        Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000), // One day ago
        Date(System.currentTimeMillis()), // Today
    )
    VideoTheme {
        Surface(
            modifier = Modifier
                .size(300.dp, 200.dp),
            color = Color(0xFAFAFA),
        ) {
            LineChart(
                data = listOf(100f, 200f, 150f, 300f, 0f),
                dates = emptyList(),
            )
        }
    }
}
