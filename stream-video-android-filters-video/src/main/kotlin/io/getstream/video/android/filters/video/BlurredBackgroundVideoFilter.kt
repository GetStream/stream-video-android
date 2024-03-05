/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.filters.video

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.tasks.Tasks
import com.google.android.renderscript.Toolkit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import io.getstream.video.android.core.call.video.BitmapVideoFilter

/**
 * Applies a blur effect to the background of a video call.
 *
 * @param blurIntensity The intensity of the blur effect. See [BlurIntensity] for options. Defaults to [BlurIntensity.MEDIUM].
 */
public class BlurredBackgroundVideoFilter(
    private val blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
) : BitmapVideoFilter() {
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
    private val segmenter = Segmentation.getClient(options)

    private lateinit var segmentationMask: SegmentationMask
    private val backgroundBitmap by lazy {
        Bitmap.createBitmap(
            segmentationMask.width,
            segmentationMask.height,
            Bitmap.Config.ARGB_8888,
        )
    }

    override fun filter(videoFrameBitmap: Bitmap) {
        // 1. Apply segmentation
        val mlImage = InputImage.fromBitmap(videoFrameBitmap, 0)
        val task = segmenter.process(mlImage)
        segmentationMask = Tasks.await(task)

        // 2. Copy the background segment to a new bitmap
        copySegment(
            segment = Segment.BACKGROUND,
            source = videoFrameBitmap,
            destination = backgroundBitmap,
            segmentationMask = segmentationMask,
            confidenceThreshold = FOREGROUND_THRESHOLD,
        )

        // 3. Blur the background bitmap
        val blurredBackgroundBitmap = Toolkit.blur(backgroundBitmap, blurIntensity.radius)

        // 4. Draw the blurred background bitmap on the original bitmap
        val canvas = Canvas(videoFrameBitmap)
        val matrix = newSegmentationMaskMatrix(videoFrameBitmap, segmentationMask)
        canvas.drawBitmap(blurredBackgroundBitmap, matrix, null)
    }
}

/**
 * The intensity of the blur effect. Used in [BlurredBackgroundVideoFilter].
 */
public enum class BlurIntensity(public val radius: Int) {
    LIGHT(7),
    MEDIUM(11),
    HEAVY(16),
}

private const val FOREGROUND_THRESHOLD: Double = 0.99999 // 1 is max confidence that pixel is in the foreground
