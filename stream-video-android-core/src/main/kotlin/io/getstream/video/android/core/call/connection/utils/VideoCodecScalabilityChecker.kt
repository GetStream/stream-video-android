package io.getstream.video.android.core.call.connection.utils

import android.util.Log
import io.getstream.webrtc.DefaultVideoEncoderFactory
import io.getstream.webrtc.EglBase
import io.getstream.webrtc.HardwareVideoEncoderFactory
import io.getstream.webrtc.ResolutionAdjustment
import io.getstream.webrtc.SimulcastAlignedVideoEncoderFactory
import io.getstream.webrtc.SoftwareVideoEncoderFactory
import io.getstream.webrtc.VideoCodecInfo
import io.getstream.webrtc.VideoEncoderFactory


/**
 * Utility class to check video codec scalability modes on Android devices
 * This helps debug device-specific codec capabilities
 */
object VideoCodecScalabilityChecker {
    private const val TAG = "VideoCodecScalabilityChecker"

    /**
     * Check and log all supported video codecs and their scalability modes
     */
    fun checkVideoCodecScalabilityModes() {
        Log.i(TAG, "=== Starting Video Codec Scalability Mode Check ===")


        // Create different encoder factories to test
        val factories = arrayOf<VideoEncoderFactory?>(
            SoftwareVideoEncoderFactory(),
//            HardwareVideoEncoderFactory(null, false, true),
            SimulcastAlignedVideoEncoderFactory(
                EglBase.create().eglBaseContext,
                true,
                true,
                ResolutionAdjustment.MULTIPLE_OF_16,
            ),
            DefaultVideoEncoderFactory(null, false, true)
        )

        val factoryNames = arrayOf<String?>(
            "SoftwareVideoEncoderFactory",
            "HardwareVideoEncoderFactory",
            "DefaultVideoEncoderFactory"
        )

        for (i in factories.indices) {
            Log.i(TAG, "\n--- " + factoryNames[i] + " ---")
            checkCodecFactory(factories[i]!!, factoryNames[i])
        }

        Log.i(TAG, "=== Video Codec Scalability Mode Check Complete ===")
    }

    /**
     * Check a specific video encoder factory
     */
    private fun checkCodecFactory(factory: VideoEncoderFactory, factoryName: String?) {
        try {
            val supportedCodecs = factory.supportedCodecs
            Log.i(TAG, factoryName + " supports " + supportedCodecs.size + " codecs:")

            for (codecInfo in supportedCodecs) {
                logCodecInfo(codecInfo, factoryName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking " + factoryName + ": " + e.message)
        }
    }

    /**
     * Log detailed information about a single codec
     */
    private fun logCodecInfo(codecInfo: VideoCodecInfo, factoryName: String?) {
        val log = StringBuilder()
        log.append("Codec: ").append(codecInfo.name)
        log.append(" | Params: ").append(codecInfo.params)


        // Check scalability modes
        val scalabilityModes = codecInfo.scalabilityModes
        if (scalabilityModes == null) {
            log.append(" | ScalabilityModes: NULL")
        } else if (scalabilityModes.isEmpty()) {
            log.append(" | ScalabilityModes: EMPTY LIST")
        } else {
            log.append(" | ScalabilityModes: [")
            for (i in scalabilityModes.indices) {
                if (i > 0) log.append(", ")
                log.append(scalabilityModes.get(i))
            }
            log.append("]")
        }

        Log.i(TAG, log.toString())
    }

    /**
     * Check if a specific scalability mode is supported for a codec
     */
    fun isScalabilityModeSupported(codecName: String?, scalabilityMode: String?): Boolean {
        val factory: VideoEncoderFactory = DefaultVideoEncoderFactory(null, false, true)
        val supportedCodecs = factory.getSupportedCodecs()

        for (codecInfo in supportedCodecs) {
            if (codecInfo.name.equals(codecName, ignoreCase = true)) {
                val scalabilityModes = codecInfo.scalabilityModes
                if (scalabilityModes != null) {
                    return scalabilityModes.contains(scalabilityMode)
                }
            }
        }
        return false
    }

    /**
     * Get all supported scalability modes for a specific codec
     */
    fun getSupportedScalabilityModes(codecName: String?): MutableList<String?> {
        val factory: VideoEncoderFactory = DefaultVideoEncoderFactory(null, false, true)
        val supportedCodecs = factory.getSupportedCodecs()

        for (codecInfo in supportedCodecs) {
            if (codecInfo.name.equals(codecName, ignoreCase = true)) {
                val scalabilityModes = codecInfo.scalabilityModes
                return if (scalabilityModes != null) ArrayList<String?>(scalabilityModes) else ArrayList<String?>()
            }
        }
        return ArrayList<String?>()
    }

    /**
     * Compare codec capabilities between different factories
     */
    fun compareCodecCapabilities() {
        Log.i(TAG, "=== Comparing Codec Capabilities ===")

        val softwareFactory: VideoEncoderFactory = SoftwareVideoEncoderFactory()
        val hardwareFactory: VideoEncoderFactory = HardwareVideoEncoderFactory(null, false, true)

        val softwareCodecs = softwareFactory.getSupportedCodecs()
        val hardwareCodecs = hardwareFactory.getSupportedCodecs()

        Log.i(TAG, "Software codecs: " + softwareCodecs.size)
        Log.i(TAG, "Hardware codecs: " + hardwareCodecs.size)


        // Find common codecs and compare their scalability modes
        for (swCodec in softwareCodecs) {
            for (hwCodec in hardwareCodecs) {
                if (swCodec.name.equals(hwCodec.name, ignoreCase = true)) {
                    Log.i(TAG, "\nComparing codec: " + swCodec.name)
                    Log.i(
                        TAG, "  Software scalability modes: " +
                                (if (swCodec.scalabilityModes != null) swCodec.scalabilityModes else "NULL")
                    )
                    Log.i(
                        TAG, "  Hardware scalability modes: " +
                                (if (hwCodec.scalabilityModes != null) hwCodec.scalabilityModes else "NULL")
                    )


                    // Check if they're different
                    val different = swCodec.scalabilityModes != hwCodec.scalabilityModes
                    if (different) {
                        Log.w(TAG, "  *** DIFFERENCE DETECTED for " + swCodec.name + " ***")
                    }
                    break
                }
            }
        }
    }

    /**
     * Test specific scalability modes that are commonly used
     */
    fun testCommonScalabilityModes() {
        Log.i(TAG, "=== Testing Common Scalability Modes ===")

        val commonModes = arrayOf<String?>(
            "L1T1", "L1T2", "L1T3", "L2T1", "L2T2", "L2T3",
            "L3T1", "L3T2", "L3T3", "S2T1", "S2T2", "S2T3"
        )

        val commonCodecs = arrayOf<String?>("VP8", "VP9", "H264", "AV1", "H265")

        val factory: VideoEncoderFactory = DefaultVideoEncoderFactory(null, false, true)
        val supportedCodecs = factory.getSupportedCodecs()

        for (codecName in commonCodecs) {
            Log.i(TAG, "\nTesting codec: " + codecName)
            val supportedModes = getSupportedScalabilityModes(codecName)

            if (supportedModes.isEmpty()) {
                Log.w(TAG, "  No scalability modes supported for " + codecName)
            } else {
                Log.i(TAG, "  Supported modes: " + supportedModes)

                for (mode in commonModes) {
                    val supported = supportedModes.contains(mode)
                    Log.i(
                        TAG,
                        "    " + mode + ": " + (if (supported) "SUPPORTED" else "NOT SUPPORTED")
                    )
                }
            }
        }
    }
}