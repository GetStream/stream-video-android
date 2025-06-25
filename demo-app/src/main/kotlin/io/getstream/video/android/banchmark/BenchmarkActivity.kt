package io.getstream.video.android.banchmark

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.data.services.stream.StreamService
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.util.UserHelper
import io.getstream.video.android.util.config.AppConfig
import kotlinx.coroutines.flow.mapLatest

public class BenchmarkActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoTheme {
                StartBenchmark()
            }
        }
    }
}


@Composable
fun StartBenchmark() {
    Column(
        Modifier
            .background(Color.White)
            .fillMaxSize()
    ) {
        var started by remember { mutableStateOf(false) }
        var benchmark by remember { mutableStateOf<Benchmark?>(null) }

        if (benchmark == null) {
            val context = LocalContext.current
            BenchmarkScreen(onStarted = { callId, config ->
                benchmark = Benchmark(StreamCallId.fromCallCid(callId), config)
                benchmark!!.run(context)
                started = true
            })
        } else {
            val users by benchmark!!.users.collectAsState()
            Button(onClick = {
                benchmark?.stop()
            }) {
                Text("Stop Benchmark")
            }
            Spacer(Modifier.height(48.dp))
            Divider()
            ParticipantList(users.values.toList())
        }
    }
}

@Composable
fun BenchmarkConfigForm(
    config: BenchmarkConfig, onConfigChange: (BenchmarkConfig) -> Unit
) {
    var joinIntervalMin by remember { mutableStateOf(config.joinIntervalMin.toString()) }
    var joinInterval by remember { mutableStateOf(config.joinInterval.toString()) }
    var leaveIntervalMin by remember { mutableStateOf(config.leaveIntervalMin.toString()) }
    var leaveInterval by remember { mutableStateOf(config.leaveInterval.toString()) }
    var minUsers by remember { mutableStateOf(config.minUsers.toString()) }
    var maxUsers by remember { mutableStateOf(config.maxUsers.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Benchmark Config", style = MaterialTheme.typography.subtitle1)

        OutlinedTextField(value = joinIntervalMin, onValueChange = {
            joinIntervalMin = it
            onConfigChange(
                config.copy(joinIntervalMin = it.toLongOrNull() ?: config.joinIntervalMin)
            )
        }, label = { Text("Join Interval Min (ms)") })

        OutlinedTextField(value = joinInterval, onValueChange = {
            joinInterval = it
            onConfigChange(
                config.copy(joinInterval = it.toLongOrNull() ?: config.joinInterval)
            )
        }, label = { Text("Join Interval (ms)") })

        OutlinedTextField(value = leaveIntervalMin, onValueChange = {
            leaveIntervalMin = it
            onConfigChange(
                config.copy(leaveIntervalMin = it.toLongOrNull() ?: config.leaveIntervalMin)
            )
        }, label = { Text("Leave Interval Min (ms)") })

        OutlinedTextField(value = leaveInterval, onValueChange = {
            leaveInterval = it
            onConfigChange(
                config.copy(leaveInterval = it.toLongOrNull() ?: config.leaveInterval)
            )
        }, label = { Text("Leave Interval (ms)") })

        OutlinedTextField(value = minUsers, onValueChange = {
            minUsers = it
            onConfigChange(
                config.copy(minUsers = it.toIntOrNull() ?: config.minUsers)
            )
        }, label = { Text("Min Users") })

        OutlinedTextField(value = maxUsers, onValueChange = {
            maxUsers = it
            onConfigChange(
                config.copy(maxUsers = it.toIntOrNull() ?: config.maxUsers)
            )
        }, label = { Text("Max Users") })
    }
}

@Composable
fun BenchmarkScreen(onStarted: (String, BenchmarkConfig) -> Unit) {
    var config by remember { mutableStateOf(BenchmarkConfig()) }
    var callId by remember { mutableStateOf("livestream:123lexgvg") }
    OutlinedTextField(value = callId, onValueChange = {
        callId = it
    }, label = { Text("Max Users") })
    BenchmarkConfigForm(config = config, onConfigChange = { config = it })
    Button(onClick = {
        onStarted.invoke(callId, config)
    }) {
        Text("Start Benchmark")
    }
}

@Composable
fun ParticipantList(
    participants: List<ParticipantState>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = participants, key = { it }) { participant ->
            ParticipantListItem(participant = participant)
            Divider()
        }
    }
}


@Composable
fun ParticipantListItem(
    participant: ParticipantState,
) {
    val name by participant.userNameOrId.collectAsState()
    val id by participant.userId.collectAsState()
    val connection by participant.call.state.connection.collectAsState()
    val receivesAudio by remember(participant.call) {
        derivedStateOf {
            participant.call.state.participants.mapLatest {
                it.any { if (it.audioEnabled.value) it.audio.value != null else false }
            }
        }
    }
    val audio by receivesAudio.collectAsState(false)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.fillMaxWidth(1f)) {
            Text(name, style = VideoTheme.typography.bodyL)
            Text(
                text = id,
                style = VideoTheme.typography.bodyS,
            )
        }

        Icon(
            imageVector = if (connection == RealtimeConnection.Connected) Icons.Default.CloudDone else Icons.Default.CloudOff,
            contentDescription = "Mic",
            tint = if (connection == RealtimeConnection.Connected) VideoTheme.colors.brandGreen else VideoTheme.colors.brandRed,
        )
        Icon(
            imageVector = if (audio) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "Mic",
            tint = if (audio) VideoTheme.colors.brandGreen else VideoTheme.colors.brandRed,
        )
    }
}


@Preview
@Composable
fun BenchmarkScreenPreview() {
    VideoTheme {
        BenchmarkScreen { _, _ -> }
    }
}

