/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui

import io.getstream.result.Error
import io.getstream.video.android.core.faultinjector.FaultInjector
import io.getstream.video.android.core.faultinjector.FaultKey
import retrofit2.HttpException
import retrofit2.Response

internal class FaultInjectorImpl : FaultInjector {
    private val enabledFaults = mutableMapOf<FaultKey, Boolean>()

    override fun enable(key: FaultKey) {
        enabledFaults[key] = true
    }

    override fun disable(key: FaultKey) {
        enabledFaults[key] = false
    }

    override fun setEnabled(key: FaultKey, enabled: Boolean) {
        enabledFaults[key] = enabled
    }

    override fun isEnabled(key: FaultKey): Boolean {
        return enabledFaults[key] == true
    }

    override fun clear() {
        enabledFaults.clear()
    }

    override fun throwDebugFault(key: FaultKey) {
        if (enabledFaults[key] == true) {
            throw when (key) {
                FaultKey.FAIL_LOCATION -> HttpException(
                    Response.error<String>(
                        100,
                        okhttp3.ResponseBody.create(null, ""),
                    ),
                )
                else -> RuntimeException("Fault injected: $key")
            }
        }
    }

    override fun sendFailResult(key: FaultKey): io.getstream.result.Result.Failure {
        return io.getstream.result.Result.Failure(
            Error.GenericError("Fault injected: $key"),
        )
    }
}
