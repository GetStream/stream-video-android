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

package io.getstream.video.android.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * Provides a [VideoComponentFactory] built on top of the current [VideoComponentFactory] to the
 * given [content], allowing overrides to be layered for a subtree.
 *
 * ```
 * CompoundComponentFactory(
 *     factory = { currentComponentFactory ->
 *         object : VideoComponentFactory by currentComponentFactory {
 *             // Overridden components
 *         }
 *     },
 * ) {
 *     // Content using the compound factory
 * }
 * ```
 *
 * @param keys Optional keys to control the recomposition of the compound factory.
 * @param factory Builds the new [VideoComponentFactory] from the current one.
 * @param content The content that uses the compound factory.
 */
@Composable
public fun CompoundComponentFactory(
    vararg keys: Any?,
    factory: (currentComponentFactory: VideoComponentFactory) -> VideoComponentFactory,
    content: @Composable () -> Unit,
) {
    val currentComponentFactory = LocalComponentFactory.current
    val compoundComponentFactory = remember(currentComponentFactory, factory, *keys) {
        factory(currentComponentFactory)
    }
    CompositionLocalProvider(
        value = LocalComponentFactory provides compoundComponentFactory,
        content = content,
    )
}
