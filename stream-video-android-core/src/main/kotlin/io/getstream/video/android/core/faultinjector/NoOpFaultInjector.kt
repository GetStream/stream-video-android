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

package io.getstream.video.android.core.faultinjector

import io.getstream.result.Error

internal class NoOpFaultInjector : FaultInjector {

    override fun enable(key: FaultKey) {}
    override fun disable(key: FaultKey) {}
    override fun setEnabled(key: FaultKey, enabled: Boolean) {}
    override fun isEnabled(key: FaultKey): Boolean = false
    override fun clear() {}
    override fun throwDebugFault(key: FaultKey) {}
    override fun sendFailResult(key: FaultKey): io.getstream.result.Result.Failure {
        return io.getstream.result.Result.Failure(
            Error.GenericError("Fault injected: $key"),
        )
    }
}
