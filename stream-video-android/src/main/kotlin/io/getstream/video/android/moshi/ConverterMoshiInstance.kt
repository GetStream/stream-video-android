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

package io.getstream.video.android.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.MultiMapJsonAdapter
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

internal val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .add(MultiMapJsonAdapter.FACTORY)
    .build()

@OptIn(ExperimentalStdlibApi::class)
internal val filterAdapter = moshi.adapter<Map<String, Any>>()
