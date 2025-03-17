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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.collectLatest
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
            IconButton(modifier = Modifier.align(Alignment.TopEnd), onClick = onDismiss) {
                Icon(
                    tint = Color.White,
                    imageVector = Icons.Default.Close,
                    contentDescription = Icons.Default.Close.name,
                )
            }
        }
    }
}

@Composable
fun CallStats(call: Call) {
    val latencyHistory by call.statLatencyHistory.collectAsStateWithLifecycle()
    val statsReport by call.statsReport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    Column(
        modifier = Modifier
            .background(Color(0xFF101213))
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            text = "Stats",
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight(600),
                color = Color.White,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.size(32.dp))
        UserAndCallId(call = call, clipboardManager)
        HeaderWithIconAndBody(
            icon = Icons.Default.AvTimer,
            "Call latency",
            "Very high latency values may reduce call quality, cause lag, and make the call less enjoyable.",
        )
        Spacer(modifier = Modifier.size(6.dp))
        if (LocalInspectionMode.current) {
            LineChartPreview()
        } else {
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                data = latencyHistory.map {
                    it.toFloat()
                },
                dates = emptyList(),
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        Spacer(modifier = Modifier.size(6.dp))
        HeaderWithIconAndBody(
            icon = Icons.Default.BarChart,
            "Call performance",
            "Very high latency values may reduce call quality, cause lag, and make the call less enjoyable.",
        )
        Spacer(modifier = Modifier.size(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            val latency by call.state.stats.publisher.latency.collectAsStateWithLifecycle()
            LaunchedEffect(call) {
                call.statsReport.collectLatest {
                    Log.d("SKALI", "Resolution: ${it?.stateStats?.publisher?.resolution?.value}")
                }
            }
            val publisherResolution by call.state.stats.publisher.resolution.collectAsStateWithLifecycle()
            val publisherDropReason by call.state.stats.publisher.qualityDropReason.collectAsStateWithLifecycle()
            val subscriberResolution by call.state.stats.subscriber.resolution.collectAsStateWithLifecycle()
            val publisherJitter by call.state.stats.publisher.jitterInMs.collectAsStateWithLifecycle()
            val subscriberJitter by call.state.stats.subscriber.jitterInMs.collectAsStateWithLifecycle()
            val publisherBitrate by call.state.stats.publisher.bitrateKbps.collectAsStateWithLifecycle()
            val subscriberBitrate by call.state.stats.subscriber.bitrateKbps.collectAsStateWithLifecycle()
            val publisherVideoCodec by call.state.stats.publisher.videoCodec.collectAsStateWithLifecycle()
            val publisherCodecLabel = if (publisherVideoCodec.isNotEmpty()) "($publisherVideoCodec)" else ""
            val subscriberVideoCodec by call.state.stats.subscriber.videoCodec.collectAsStateWithLifecycle()
            val subscriberCodecLabel = if (subscriberVideoCodec.isNotEmpty()) "($subscriberVideoCodec)" else ""

            LatencyOrJitter(title = "Latency", value = latency)
            Spacer(modifier = Modifier.size(16.dp))
            LatencyOrJitter(title = "Receive jitter", value = subscriberJitter)
            Spacer(modifier = Modifier.size(16.dp))
            LatencyOrJitter(title = "Publish jitter", value = publisherJitter)
            Spacer(modifier = Modifier.size(16.dp))
            StatItem(title = "Region", value = statsReport?.local?.sfu)
            Spacer(modifier = Modifier.size(16.dp))
            StatItem(title = "Publish resolution $publisherCodecLabel", value = publisherResolution)
            Spacer(modifier = Modifier.size(16.dp))
            StatItem(title = "Publish quality drop reason", value = publisherDropReason)
            Spacer(modifier = Modifier.size(16.dp))
            StatItem(
                title = "Receiving resolution $subscriberCodecLabel",
                value = subscriberResolution,
            )
            Spacer(modifier = Modifier.size(16.dp))
            StatItem(title = "Publish bitrate", value = "$publisherBitrate Kbps")
            Spacer(modifier = Modifier.size(16.dp))
            StatItem(title = "Receiving bitrate", value = "$subscriberBitrate Kbps")
        }
    }
}

@Composable
fun HeaderWithIconAndBody(icon: ImageVector, header: String, body: String) {
    val color = remember { Color(0xFFFFFFFF) }
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier
                    .padding(1.dp)
                    .width(28.dp)
                    .height(28.dp),
                imageVector = icon,
                contentDescription = icon.name,
                tint = color,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = header,
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight(500),
                    color = color,
                ),
            )
        }
        Text(
            text = body,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight(400),
                color = VideoTheme.colors.basePrimary,
            ),
        )
    }
}

@Composable
fun LatencyOrJitter(title: String, value: Int?, okRange: IntRange = IntRange(75, 400)) {
    Log.d("SKALI", "Recomposing (Latency or Jitter): $value")
    val dataText = "${value ?: "--"} ms"
    val indicatorData = if (value == null) {
        null
    } else if (okRange.contains(value)) {
        Pair(Color(0xFFFFD646), "Ok")
    } else if (okRange.first > value) {
        Pair(Color(0xFF00E2A1), "Good")
    } else {
        Pair(Color(0xFFDC433B), "Bad")
    }

    StatItem(title = title, value = dataText) {
        indicatorData?.let {
            StatIndicator(indicatorColor = it.first, indicatorText = it.second)
        }
    }
}

@Composable
fun StatItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String?,
    indicator: @Composable () -> Unit = {},
) {
    val text = value
    Log.d("SKALI", "Recomposing (StatItem): $text")
    Column(
        modifier = modifier
            .background(color = Color.Black, shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 10.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.W600,
                color = Color(0xFF979CA0),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                text = value ?: "--",
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.W600,
                    color = Color.White,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            indicator()
        }
    }
}

@Composable
fun StatIndicator(modifier: Modifier = Modifier, indicatorColor: Color, indicatorText: String) {
    Box(
        modifier = modifier
            .width(80.dp)
            .background(
                color = indicatorColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = indicatorText,
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.W600,
                color = indicatorColor,
            ),
        )
    }
}

@Composable
fun UserAndCallId(call: Call, clipboardManager: ClipboardManager?) {
    Box(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .background(color = Color.Black, shape = RoundedCornerShape(16.dp))
                .padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(
                    modifier = Modifier.size(44.dp),
                    userImage = call.user.image,
                    userName = call.user.userNameOrId,
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Call ID:",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight(500),
                            color = Color.White,
                        ),
                    )
                    Text(
                        text = call.cid,
                        softWrap = true,
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.W400,
                            color = VideoTheme.colors.baseSecondary,
                        ),
                    )
                }
            }
            clipboardManager?.let {
                Spacer(modifier = Modifier.size(8.dp))
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = {
                        val clipData = ClipData.newPlainText("Call ID", call.cid)
                        clipboardManager.setPrimaryClip(clipData)
                    },
                ) {
                    Icon(
                        tint = Color.White,
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                    )
                }
            }
        }
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
    Canvas(modifier = modifier.padding(end = 44.dp, top = 16.dp, bottom = 24.dp)) {
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
            val x = (index * ((size.width.minus(10)) / (data.size - 1)))
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
            modifier = Modifier.size(400.dp, 200.dp),
            color = Color(0xFAFAFA),
        ) {
            LineChart(
                data = listOf(100f, 200f, 150f, 300f, 0f),
                dates = emptyList(),
            )
        }
    }
}

@Composable
@Preview
fun StatsItemPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Column {
            StatItem(title = "Latency", value = "12 ms") {
                StatIndicator(indicatorColor = Color(0xFF00E2A1), indicatorText = "Good")
            }
            Spacer(Modifier.size(16.dp))
            StatItem(title = "Latency", value = "122 ms") {
                StatIndicator(indicatorColor = Color(0xFFFFD646), indicatorText = "Ok")
            }
            Spacer(Modifier.size(16.dp))
            StatItem(title = "Latency", value = "432 ms") {
                StatIndicator(indicatorColor = Color(0xFFDC433B), indicatorText = "Bad")
            }
            Spacer(Modifier.size(16.dp))
            StatItem(
                title = "Region",
                value = "sfu-7d887ab5-9c00-4f1f-b6b0-d8f097164727-7d887ab5-9c00-4f1f-b6b0-d8f097164727",
            )
        }
    }
}

@Composable
@Preview
fun StatsIndicatorPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatIndicator(indicatorColor = Color(0xFF00E2A1), indicatorText = "Good")
            StatIndicator(indicatorColor = Color(0xFFDC433B), indicatorText = "Bad")
            StatIndicator(indicatorColor = Color(0xFFFFD646), indicatorText = "Ok")
        }
    }
}
