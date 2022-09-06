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

package io.getstream.video.android.audio

import android.os.Handler
import androidx.annotation.VisibleForTesting

internal const val TIMEOUT = 5000L

internal abstract class BluetoothScoJob(
    private val bluetoothScoHandler: Handler,
) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var bluetoothScoRunnable: BluetoothScoRunnable? = null

    protected abstract fun scoAction()

    open fun scoTimeOutAction() {}

    fun executeBluetoothScoJob() {
        // cancel existing runnable
        bluetoothScoRunnable?.let { bluetoothScoHandler.removeCallbacks(it) }

        BluetoothScoRunnable().apply {
            bluetoothScoRunnable = this
            bluetoothScoHandler.post(this)
        }
    }

    fun cancelBluetoothScoJob() {
        bluetoothScoRunnable?.let {
            bluetoothScoHandler.removeCallbacks(it)
            bluetoothScoRunnable = null
        }
    }

    inner class BluetoothScoRunnable : Runnable {

        private val startTime = System.currentTimeMillis()
        private var elapsedTime = 0L

        override fun run() {
            if (elapsedTime < TIMEOUT) {
                scoAction()
                elapsedTime = System.currentTimeMillis() - startTime
                bluetoothScoHandler.postDelayed(this, 500)
            } else {
                scoTimeOutAction()
                cancelBluetoothScoJob()
            }
        }
    }
}
