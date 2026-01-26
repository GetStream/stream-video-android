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

package io.getstream.video.android.ui.common.util

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY,
)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate Stream Video SDK Api, overriding this API may interfere on how the activity handles the call state.",
)
public annotation class StreamCallActivityDelicateApi()

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY,
)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate Stream Video SDK Api, overriding this API may interfere on how the video is rendered and may introduce unwanted behavior.",
)
public annotation class StreamVideoUiDelicateApi()

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY,
)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an experimental Stream Video SDK Api, it may have breaking changes without notice.",
)
public annotation class StreamVideoExperimentalApi(val message: String)
