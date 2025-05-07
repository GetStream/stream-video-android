package io.getstream.video.android.ui.menu

import androidx.compose.ui.graphics.vector.ImageVector
import io.getstream.video.android.core.audio.StreamAudioDevice

data class AudioDeviceUiState(
    val streamAudioDevice: StreamAudioDevice,
    val text: String,
    val icon: ImageVector, // Assuming it's a drawable resource ID
    val highlight: Boolean,
)