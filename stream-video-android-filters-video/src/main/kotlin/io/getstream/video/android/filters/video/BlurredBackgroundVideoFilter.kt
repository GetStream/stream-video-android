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

package io.getstream.video.android.filters.video

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.Keep
import com.google.android.gms.tasks.Tasks
import com.google.android.renderscript.Toolkit
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.call.video.BitmapVideoFilter
import kotlin.getValue

/**
 * Applies a blur effect to the background of a video call.
 *
 * @param blurIntensity The intensity of the blur effect. See [BlurIntensity] for options. Defaults to [BlurIntensity.MEDIUM].
 * @param foregroundThreshold The confidence threshold for the foreground. Pixels with a confidence value greater than or equal to this threshold are considered to be in the foreground. Value is coerced between 0 and 1, inclusive.
 */
@Keep
public class BlurredBackgroundVideoFilter(
    private val blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    foregroundThreshold: Double = DEFAULT_FOREGROUND_THRESHOLD,
) : BitmapVideoFilter() {
    private val options =
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .enableRawSizeMask()
            .build()
    private val segmenter = Segmentation.getClient(options)
    private lateinit var segmentationMask: SegmentationMask
    private var foregroundThreshold: Double = foregroundThreshold.coerceIn(0.0, 1.0)
    private val backgroundBitmap by lazy {
        Bitmap.createBitmap(
            segmentationMask.width,
            segmentationMask.height,
            Bitmap.Config.ARGB_8888,
        )
    }

    private val logger by taggedLogger("BlurredBackgroundVideoFilter")

    override fun applyFilter(videoFrameBitmap: Bitmap) {
        // Apply segmentation
        val mlImage = InputImage.fromBitmap(videoFrameBitmap, 0)
        val task = segmenter.process(mlImage)
        segmentationMask = Tasks.await(task)

        // Copy the background segment to a new bitmap - backgroundBitmap
        copySegment(
            segment = Segment.BACKGROUND,
            source = videoFrameBitmap,
            destination = backgroundBitmap,
            segmentationMask = segmentationMask,
            confidenceThreshold = foregroundThreshold,
        )

        // Blur the background bitmap
        val timeBeforeBlur = System.currentTimeMillis()
        val blurredBackgroundBitmap = Toolkit.blur(backgroundBitmap, blurIntensity.radius)
        logger.d { "Time taken to blur image : ${System.currentTimeMillis() - timeBeforeBlur}" }

        // Draw the blurred background bitmap on the original bitmap
        val canvas = Canvas(videoFrameBitmap)
        val matrix = newSegmentationMaskMatrix(videoFrameBitmap, segmentationMask)
        canvas.drawBitmap(blurredBackgroundBitmap, matrix, null)
    }
}

/**
 * The intensity of the background blur effect. Used in [BlurredBackgroundVideoFilter].
 */
@Keep
public enum class BlurIntensity(public val radius: Int) {
    LIGHT(7),
    MEDIUM(11),
    HEAVY(16),
    ULTRA(25),
}

private const val DEFAULT_FOREGROUND_THRESHOLD: Double = 0.999 // 1 is max confidence that pixel is in the foreground
