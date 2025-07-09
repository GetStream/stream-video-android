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

/** Marks an API as internal to the Stream SDK. */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message =
        "This is internal API for the Stream SDKs. Do not depend on this API in your own client code. " +
                "These functions may have breaking changes without notice. Usage by integrators is discouraged. Use at your own risk.",
    level = RequiresOptIn.Level.ERROR,
)
annotation class StreamInternalApi

/** Marks an API as experimental and may be changed or removed in future releases. */
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message =
        "This API is experimental and may be changed or removed in future releases. " +
                "Usage by integrators is discouraged. Use at your own risk.",
    level = RequiresOptIn.Level.ERROR,
)
annotation class StreamExperimentalApi

/** Marks an API as delicate i.e. its usage has to be considered carefully!. */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message =
        "This is a delicate api, you should make sure to understand the implications of using this API." +
                "These functions may have breaking changes without notice. Usage by integrators is discouraged. Use at your own risk.",
    level = RequiresOptIn.Level.ERROR,
)
annotation class StreamDelicateApi(val message: String = "")

/** Marks an API as error i.e. it must not be used. Usually added to inherited APIs from Java. */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message = "Usage of this API is an error. Use alternative APIs instead. The usage of this API breaks the SDK functionality.",
    level = RequiresOptIn.Level.ERROR
)
annotation class StreamErrorUsage

/**
 * Marks an API as deprecated and "needs to be removed" after a certain date.
 *
 * Note: This is a marker annotation and does not have any effect on the code. If you mark an API
 * with this also mark it with @Deprecated.
 *
 * @property deprecatedOn The date when the annotation was added "dd.MM.yyyy"
 * @property deprecatedInVersion The version in which the API is deprecated i.e. current version.
 * @property warnAfterDays The number of days after which a warning should be logged
 * @property errorAfterDays The number of days after which an error should be logged
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.SOURCE)
annotation class StreamDeprecated(
    val deprecatedOn: String = "01.01.2099",
    val deprecatedInVersion: String = "1.0.0",
    val warnAfterDays: Long = 90,
    val errorAfterDays: Long = -1,
)
