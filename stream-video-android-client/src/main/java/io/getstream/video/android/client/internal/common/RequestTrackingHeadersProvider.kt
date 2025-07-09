package io.getstream.video.android.client.internal.common

import android.os.Build
import io.getstream.video.android.client.internal.generated.IntegrationAppConfig

internal class RequestTrackingHeadersProvider {
    /**
     * Header used to track which SDK is being used.
     */
    companion object {
        @JvmStatic
        internal var VERSION_PREFIX_HEADER: VersionPrefixHeader = VersionPrefixHeader.Default
    }

    /**
     * Builds the client information header (X-Stream-Client) that will be added to requests.
     *
     * @return Header value as a string.
     */

    internal fun provideTrackingHeaders(
        config: IntegrationAppConfig? = null
    ): String {
        return buildString {
            append("${VERSION_PREFIX_HEADER.prefix}-${"1.9.1"}")
            append("|os=Android ${Build.VERSION.RELEASE}")
            append("|api_version=${Build.VERSION.SDK_INT}")
            append("|device_model=${Build.MANUFACTURER} ${Build.MODEL}")
            append(buildAppVersionForHeader(config))
            append(buildAppName(config))
        }
    }

    private fun buildAppVersionForHeader(config: IntegrationAppConfig?) =
        "|app_version=" + (config?.appVersion ?: "unknown")

    private fun buildAppName(config: IntegrationAppConfig?): String =
        "|app_name=" + (config?.appName ?: config?.appPackageName ?: "unknown")
}

internal sealed class VersionPrefixHeader {
    abstract val prefix: String

    /**
     * Low-level client.
     */
    data object Default : VersionPrefixHeader() {
        override val prefix: String = "stream-video-android"
    }

    /**
     * XML based UI components.
     */
    data object UiComponents : VersionPrefixHeader() {
        override val prefix: String = "stream-video-android-ui-components"
    }

    /**
     * Compose UI components.
     */
    data object Compose : VersionPrefixHeader() {
        override val prefix: String = "stream-video-android-compose"
    }
}