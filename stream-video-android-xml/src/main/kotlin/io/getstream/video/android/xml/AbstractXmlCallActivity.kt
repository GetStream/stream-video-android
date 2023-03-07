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

package io.getstream.video.android.xml

import android.view.Menu
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import io.getstream.video.android.common.AbstractCallActivity
import io.getstream.video.android.xml.binding.bindView
import io.getstream.video.android.xml.widget.callcontainer.CallContainerView
import io.getstream.video.android.ui.common.R as RCommon

public abstract class AbstractXmlCallActivity : AbstractCallActivity() {

    override fun setupUi() {
        val callContent = CallContainerView(this)
        callContent.bindView(
            viewModel = callViewModel,
            lifecycleOwner = this,
            handleCallAction = { handleCallAction(it) },
            handleBackPressed = { handleBackPressed() },
            onIdle = { finish() }
        )
        callContent.setupToolbar(this)
        setContentView(callContent)
        setupBackHandler()
    }

    /**
     * Sets up a back handler to override default back button behavior.
     */
    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPressed()
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.call_menu, menu)
        menu?.findItem(R.id.callParticipants)?.let {
            it.icon?.setTint(ContextCompat.getColor(this, RCommon.color.stream_text_high_emphasis))
        }
        return true
    }
}
