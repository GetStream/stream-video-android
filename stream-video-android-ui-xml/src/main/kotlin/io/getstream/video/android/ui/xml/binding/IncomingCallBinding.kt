package io.getstream.video.android.ui.xml.binding

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.model.CallStatus
import io.getstream.video.android.ui.xml.widget.active.ActiveCallView
import io.getstream.video.android.ui.xml.widget.incoming.IncomingCallView
import io.getstream.video.android.viewmodel.CallViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Binds [IncomingCallView] with [CallViewModel], updating the view's state based on data provided by the ViewModel,
 * and propagating view events to the ViewModel as needed.
 *
 * This function sets listeners on the view and ViewModel. Call this method
 * before setting any additional listeners on these objects yourself.
 */
public fun IncomingCallView.bindView(
    viewModel: CallViewModel,
    lifecycleOwner: LifecycleOwner
) {
    setCallStatus(CallStatus.Outgoing)

    callActionListener = viewModel::onCallAction

    lifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.callMediaState.collectLatest {
            setCameraEnabled(it.isCameraEnabled)
        }
    }

    lifecycleOwner.lifecycleScope.launchWhenCreated {
        viewModel.participants.collectLatest {
            setParticipants(it)
        }
    }
}