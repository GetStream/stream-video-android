package io.getstream.video.android.app.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.getstream.video.android.app.ui.call.CallActivity
import io.getstream.video.android.app.ui.incoming.IncomingCallActivity
import io.getstream.video.android.app.ui.login.LoginActivity
import io.getstream.video.android.app.ui.outgoing.OutgoingCallActivity
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.model.IncomingCallData
import io.getstream.video.android.model.OutgoingCallData
import io.getstream.video.android.router.StreamRouter

class StreamRouterImpl(
    private val context: Context
) : StreamRouter {

    override fun navigateToCall(callInput: CallInput) {
        context.startActivity(CallActivity.getIntent(context, callInput))
    }

    override fun onIncomingCall(callData: IncomingCallData) {
        context.startActivity(
            IncomingCallActivity.getLaunchIntent(context, callData)
        )
    }

    override fun onOutgoingCall(callData: OutgoingCallData) {
        context.startActivity(
            OutgoingCallActivity.getIntent(
                context,
                callData
            )
        )
    }

    override fun onCallAccepted() {
        TODO("Not yet implemented")
    }

    override fun onCallRejected() {
        TODO("Not yet implemented")
    }

    override fun onCallFailed(reason: String?) {
        reason?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        finish()
    }

    override fun finish() {
        (context as? Activity)?.finish()
    }

    override fun onUserLoggedOut() {
        context.startActivity(Intent(context, LoginActivity::class.java))
        finish()
    }
}