package io.getstream.webrtc.noise.cancellation

import android.content.Context
import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.AudioProcessingFactory
import org.webrtc.ExternalAudioProcessingFactory

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
            dest: File
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