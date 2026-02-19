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

package io.getstream.video.android.core.header

import io.getstream.video.android.core.internal.InternalStreamVideoApi

@InternalStreamVideoApi
public sealed class VersionPrefixHeader {
    public abstract val prefix: String

    /**
     * Low-level client.
     */
    public data object Default : VersionPrefixHeader() {
        override val prefix: String = "stream-video-android"
    }

    /**
     * XML based UI components.
     */
    public data object UiComponents : VersionPrefixHeader() {
        override val prefix: String = "stream-video-android-ui-components"
    }

    /**
     * Compose UI components.
     */
    public data object Compose : VersionPrefixHeader() {
        override val prefix: String = "stream-video-android-compose"
    }
}
