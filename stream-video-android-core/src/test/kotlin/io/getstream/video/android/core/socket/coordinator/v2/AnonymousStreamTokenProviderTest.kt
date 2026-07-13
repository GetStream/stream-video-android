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

package io.getstream.video.android.core.socket.coordinator.v2

import io.getstream.android.core.api.model.value.StreamUserId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

internal class AnonymousStreamTokenProviderTest {

    @Test
    fun `loadToken always throws because anonymous users never open a coordinator socket`() =
        runTest {
            assertFailsWith<IllegalStateException> {
                AnonymousStreamTokenProvider().loadToken(StreamUserId.fromString("anon-1"))
            }
        }
}
