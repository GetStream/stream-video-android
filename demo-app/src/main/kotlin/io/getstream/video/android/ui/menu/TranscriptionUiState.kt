package io.getstream.video.android.ui.menu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Transcribe
import androidx.compose.ui.graphics.vector.ImageVector
import io.getstream.video.android.core.TranscriptionState

data class TranscriptionUiState(val text: String,
                                val icon: ImageVector, // Assuming it's a drawable resource ID
                                val isButtonEnabled: Boolean)

fun TranscriptionState.mapTouUiState(): TranscriptionUiState {
    return when(this){
        is TranscriptionState.CallTranscriptionInitialState -> TranscriptionUiState(
            text = "Transcription Not Ready",
            icon = Icons.Default.Transcribe,
            isButtonEnabled = false
        )
        is TranscriptionState.CallTranscriptionReadyState -> TranscriptionUiState(
            text = "Transcription is ready",
            icon = Icons.Default.Transcribe,
            isButtonEnabled = true
        )
        is TranscriptionState.CallTranscriptionStartedState -> TranscriptionUiState(
            text = "Transcription in progress",
            icon = Icons.Default.Transcribe,
            isButtonEnabled = true
        )
        is TranscriptionState.CallTranscriptionStoppedState -> TranscriptionUiState(
            text = "Transcription stopped",
            icon = Icons.Default.Transcribe,
            isButtonEnabled = true
        )
        is TranscriptionState.CallTranscriptionFailedState -> TranscriptionUiState(
            text = "Transcription failed",
            icon = Icons.Default.Transcribe,
            isButtonEnabled = false
        )
    }
}
