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

import io.getstream.android.core.api.authentication.StreamTokenProvider
import io.getstream.android.core.api.model.value.StreamToken
import io.getstream.android.core.api.model.value.StreamUserId

/**
 * [StreamTokenProvider] for anonymous users, who never open a coordinator socket (D-07).
 *
 * Anonymous users are REST-only: the video client constructs normally, but any explicit
 * connect attempt fails before touching the network. Mirrors the iOS SDK's behavior where
 * `connectUser` throws `ClientError.MissingPermissions` for anonymous users while
 * initialization itself succeeds.
 */
internal class AnonymousStreamTokenProvider : StreamTokenProvider {

    override suspend fun loadToken(userId: StreamUserId): StreamToken =
        throw IllegalStateException(
            "Anonymous users do not open a coordinator socket (D-07). " +
                "REST-only operations remain available on the client.",
        )
}
