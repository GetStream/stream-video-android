/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.router.StreamRouter

class StreamRouterImpl(
    private val context: Context
) : StreamRouter {

    override fun navigateToCall(
        callInput: CallInput,
        finishCurrent: Boolean
    ) {
        context.startActivity(CallActivity.getIntent(context, callInput))
        if (finishCurrent) {
            finish()
        }
    }

    override fun onIncomingCall() {
        context.startActivity(
            IncomingCallActivity.getLaunchIntent(context)
        )
    }

    override fun onOutgoingCall() {
        context.startActivity(
            OutgoingCallActivity.getIntent(context)
        )
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
