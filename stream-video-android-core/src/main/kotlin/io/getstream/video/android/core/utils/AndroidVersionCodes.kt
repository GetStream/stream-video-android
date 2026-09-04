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

package io.getstream.video.android.core.utils

import android.os.Build

// Polyfills for Build.VERSION_CODES values not yet available at the current compileSdk.
// TODO: delete each once compileSdk covers it, replacing usages with the real constant.
internal const val BUILD_VERSION_CODES_BAKLAVA = 36 // Build.VERSION_CODES.BAKLAVA — needs compileSdk 36
internal const val BUILD_VERSION_CODES_CINNAMON_BUN = 35 // Build.VERSION_CODES.CINNAMON_BUN — needs compileSdk 37 TODO Revert it back to 37

internal fun isAndroid17OrHigher(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= BUILD_VERSION_CODES_CINNAMON_BUN
