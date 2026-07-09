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
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.CreateGuestRequest
import io.getstream.android.video.generated.models.UserRequest
import io.getstream.video.android.core.utils.toUser
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserType

/**
 * Writable sink for the SDK's currently active [User]. Introduced here as a minimal seam so the
 * guest flow can adopt the server-issued identity (id, role, image, custom) returned by the
 * `createGuest` REST call. A follow-up PR connects this to the concrete SDK-wide user store.
 */
internal interface WritableUserRepository {
    /** Persists [user] as the SDK's active user. */
    fun setUser(user: User)
}

/**
 * [StreamTokenProvider] implementation for guest users.
 *
 * Mirrors the iOS SDK's guest flow (`StreamVideo.loadGuestUserInfo`): on invocation, calls the
 * coordinator's `POST /video/guest` endpoint via [ProductvideoApi], adopts the server-issued
 * user identity through [userRepository], publishes the JWT to the SDK's REST auth path via
 * [onTokenIssued], and returns the token wrapped in a [StreamToken] for core's WS auth path.
 *
 * The video SDK keeps two auth surfaces until HTTP is unified through core: the coordinator
 * WS (core's `StreamTokenManager`, fed by this provider's return value) and the legacy Retrofit
 * `CoordinatorAuthInterceptor` (fed by [onTokenIssued]). Both must observe the guest JWT —
 * iOS expresses the same invariant through its single `tokenSubject`.
 *
 * Token refresh matches iOS: an expired guest token is renewed by calling `createGuest` again
 * (`StreamTokenManager` re-invokes this provider).
 *
 * `StreamTokenManager` wraps this provider in a `StreamSingleFlightProcessor`, so concurrent
 * callers share the same in-flight request. No additional deduplication is needed here (unlike
 * the legacy `guestUserJob: Deferred<Unit>` synchronization layer that this replaces).
 */
internal class GuestStreamTokenProvider(
    private val api: ProductvideoApi,
    private val initialUser: User,
    private val userRepository: WritableUserRepository,
    private val onTokenIssued: (String) -> Unit,
) : StreamTokenProvider {

    override suspend fun loadToken(userId: StreamUserId): StreamToken {
        val response = api.createGuest(
            createGuestRequest = CreateGuestRequest(
                user = UserRequest(
                    id = initialUser.id,
                    image = initialUser.image,
                    name = initialUser.name,
                    custom = initialUser.custom,
                ),
            ),
        )
        // Adopt the server-issued user identity so downstream SDK components observe the
        // canonical id/role instead of the placeholder used at builder time (AND-1202).
        userRepository.setUser(adoptServerUser(response.user.toUser()))
        // Sync the JWT into the legacy REST auth path before core proceeds to WS auth.
        onTokenIssued(response.accessToken)
        return StreamToken.fromString(response.accessToken)
    }

    /**
     * Ports iOS's name-preservation quirk (`StreamVideo.loadGuestUserInfo`): the server decorates
     * guest display names (e.g. `guest-<id>-<name>`); when the decorated name's last `-` component
     * equals the locally supplied name, keep the local name.
     */
    private fun adoptServerUser(serverUser: User): User {
        val localName = initialUser.name
        val lastNameComponent = serverUser.name?.split("-")?.lastOrNull()
        val resolvedName = if (!localName.isNullOrBlank() && lastNameComponent == localName) {
            localName
        } else {
            serverUser.name
        }
        return serverUser.copy(type = UserType.Guest, name = resolvedName)
    }
}
