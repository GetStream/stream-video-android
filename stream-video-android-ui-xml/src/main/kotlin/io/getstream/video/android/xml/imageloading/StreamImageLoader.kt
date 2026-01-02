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

package io.getstream.video.android.xml.imageloading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import io.getstream.video.android.xml.utils.Disposable

internal sealed interface StreamImageLoader {
    public companion object {
        public fun instance(): StreamImageLoader =
            CoilStreamImageLoader
    }

    public var imageHeadersProvider: ImageHeadersProvider

    @Suppress("LongParameterList")
    public fun load(
        target: ImageView,
        data: Any?,
        @DrawableRes placeholderResId: Int? = null,
        transformation: ImageTransformation = ImageTransformation.None,
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Disposable

    @Suppress("LongParameterList")
    public fun load(
        target: ImageView,
        data: Any?,
        placeholderDrawable: Drawable? = null,
        transformation: ImageTransformation = ImageTransformation.None,
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Disposable

    @Suppress("LongParameterList")
    public suspend fun loadAndResize(
        target: ImageView,
        data: Any?,
        placeholderDrawable: Drawable? = null,
        transformation: ImageTransformation = ImageTransformation.None,
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
    )

    @Suppress("LongParameterList")
    public fun loadVideoThumbnail(
        target: ImageView,
        uri: Uri?,
        @DrawableRes placeholderResId: Int? = null,
        transformation: ImageTransformation = ImageTransformation.None,
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
    ): Disposable

    public suspend fun loadAsBitmap(
        context: Context,
        url: String,
        transformation: ImageTransformation = ImageTransformation.None,
    ): Bitmap?

    public sealed class ImageTransformation {
        public data object None : ImageTransformation()
        public data object Circle : ImageTransformation()
        public class RoundedCorners(@Px public val radius: Float) : ImageTransformation()
    }
}
