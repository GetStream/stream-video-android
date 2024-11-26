package io.getstream.video.android.core

sealed class TranscriptionState {
    data object CallTranscriptionInitialState : TranscriptionState()
    data object CallTranscriptionStartedState : TranscriptionState()
    data object CallTranscriptionStoppedState : TranscriptionState()
    data object CallTranscriptionReadyState : TranscriptionState()
    data object CallTranscriptionFailedState : TranscriptionState()
}