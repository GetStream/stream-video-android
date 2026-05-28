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
import io.getstream.video.android.core.faultinjector.FailureInjector
import io.getstream.video.android.core.faultinjector.FailureKey
import retrofit2.HttpException
import retrofit2.Response

internal class FailureInjectorImpl : FailureInjector {
    private val faultCounts = mutableMapOf<FailureKey, Int>()

    override fun enable(key: FailureKey) {
        if ((faultCounts[key] ?: 0) == 0) faultCounts[key] = 1
    }

    override fun disable(key: FailureKey) {
        faultCounts[key] = 0
    }

    override fun setEnabled(key: FailureKey, enabled: Boolean) {
        if (enabled) enable(key) else disable(key)
    }

    override fun isEnabled(key: FailureKey): Boolean {
        return (faultCounts[key] ?: 0) > 0
    }

    override fun setCount(key: FailureKey, count: Int) {
        faultCounts[key] = count
    }

    override fun getCount(key: FailureKey): Int {
        return faultCounts[key] ?: 0
    }

    override fun clear() {
        faultCounts.clear()
    }

    override fun throwDebugFault(key: FailureKey) {
        val count = faultCounts[key] ?: 0
        if (count > 0) {
            faultCounts[key] = count - 1
            throw when (key) {
                FailureKey.FAIL_LOCATION -> HttpException(
                    Response.error<String>(
                        100,
                        okhttp3.ResponseBody.create(null, ""),
                    ),
                )
                else -> RuntimeException("Failure injected: $key")
            }
        }
    }

    override fun sendFailResult(key: FailureKey): io.getstream.result.Result.Failure {
        val count = faultCounts[key] ?: 0
        if (count > 0) {
            faultCounts[key] = count - 1
        }
        val message = when (key) {
            FailureKey.FAIL_JOIN_CALL -> "Unable to resolve host"
            else -> "Failure injected: $key"
        }
        return io.getstream.result.Result.Failure(
            Error.ThrowableError(
                message,
                RuntimeException(message),
            ),
        )
    }
}
