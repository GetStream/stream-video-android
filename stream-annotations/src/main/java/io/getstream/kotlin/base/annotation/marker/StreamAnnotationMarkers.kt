/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-android-base/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getstream.kotlin.base.annotation.marker

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/** Marks an API as DLS for the Stream SDK. */
@DslMarker annotation class StreamDsl

/** Marks a function as a factory. */
@Target(FUNCTION) @Retention(BINARY) @DslMarker annotation class StreamFactory

/**
 * Marks a data class as a configuration class. All fields must have default values.
 *
 * Note: If you delete a field from a `StreamConfiguration` data class you MUST add it to a
 * deprecated list so old config will not break at build time.
 */
@Target(CLASS) @Retention(RUNTIME) annotation class StreamConfiguration
