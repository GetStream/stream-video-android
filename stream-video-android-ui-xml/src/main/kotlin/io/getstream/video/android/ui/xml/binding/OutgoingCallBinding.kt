package io.getstream.video.android.ui.xml.binding

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.model.CallStatus
import io.getstream.video.android.ui.xml.widget.outgoing.OutgoingCallView
import io.getstream.video.android.viewmodel.CallViewModel
import kotlinx.coroutines.flow.collectLatest

public fun OutgoingCallView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner
) {
    setCallStatus(CallStatus.Outgoing)

    setCallActionListener(viewModel::onCallAction)

    lifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.callMediaState.collectLatest {
            setMicrophoneEnabled(it.isMicrophoneEnabled)
            setCameraEnabled(it.isCameraEnabled)
        }
    }

    lifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.participants.collectLatest {
            setParticipants(it)
        }
    }
}