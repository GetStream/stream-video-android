package io.getstream.video.android.compose.ui.components.call.activecall.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.viewmodel.CallViewModel

@Composable
internal fun AudioDeviceMenu(callViewModel: CallViewModel) {
    val devices = callViewModel.getAudioDevices()

    Box(
        modifier = Modifier
            .background(color = Color.LightGray.copy(alpha = 0.7f))
            .fillMaxSize()
            .clickable { callViewModel.dismissOptions() }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(200.dp)
                .align(Alignment.Center),
            color = Color.White
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(text = "Choose audio device")

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    items(devices) {
                        AudioDeviceItem(it) { device ->
                            callViewModel.selectAudioDevice(device)
                            callViewModel.dismissOptions()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioDeviceItem(
    device: AudioDevice,
    onDeviceSelected: (AudioDevice) -> Unit
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        content = { Text(text = device.name) },
        onClick = { onDeviceSelected(device) }
    )
}