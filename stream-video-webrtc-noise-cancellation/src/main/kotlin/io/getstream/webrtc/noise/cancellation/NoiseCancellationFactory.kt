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

package io.getstream.webrtc.noise.cancellation

import android.content.Context
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.AudioProcessingFactory
import org.webrtc.ExternalAudioProcessingFactory
import java.io.File
import java.io.FileOutputStream

public class NoiseCancellationFactory(
    private val context: Context,
    private val model: NoiseCancellationModel = NoiseCancellationModel.FullBand,
) : AudioProcessingFactory {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val krispDir = File(context.filesDir, KRISP)
    private val delegate = ExternalAudioProcessingFactory(LIB_FILENAME)

    init {
        scope.launch {
            context.copyKrispModelFromAssetsTo(model, krispDir)
        }
    }

    override fun createNative(): Long {
        val modelFile = File(krispDir, model.filename)
        logger.d { "[createNative] modelFile: $modelFile" }
        initModel(modelFile.absolutePath)
        return delegate.createNative()
    }

    private external fun initModel(path: String)

    private companion object {
        private const val TAG = "NoiseCancellation"
        private const val KRISP = "krisp"
        private const val LIBNAME = "noise_cancellation"
        private const val LIB_FILENAME = "libnoise_cancellation.so"

        private val logger by taggedLogger(TAG)

        init {
            System.loadLibrary(LIBNAME)
        }

        private fun Context.copyKrispModelFromAssetsTo(
            model: NoiseCancellationModel,
            dest: File,
        ) {
            logger.d { "[copyKrispModelsFromAssets] model: $model, dest: $dest" }
            val outFile = File(dest, model.filename)

            if (outFile.exists()) {
                logger.v { "[copyKrispModelsFromAssets] rejected (model already exists)" }
                return
            }

            try {
                dest.mkdirs() // Ensure the directory exists
                val assetPath = "$KRISP/${model.filename}"

                assets.open(assetPath).use { inputStream ->
                    FileOutputStream(outFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                StreamLog.e(TAG, e) { "[copyKrispModelsFromAssets] failed" }
            } finally {
                StreamLog.v(TAG) { "[copyKrispModelsFromAssets] completed" }
            }
        }
    }
}
