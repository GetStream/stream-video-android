package io.getstream.video.android.client.internal.common

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.getstream.video.android.client.internal.generated.IntegrationAppConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

public class RequestTrackingHeadersProviderTest {

    @Test
    public fun `provideTrackingHeaders returns header with default app version and name`() {
        val context = mockk<Context>(relaxed = true)
        val packageManager = mockk<PackageManager>(relaxed = true)
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "package.name"

        val packageInfo = mockk<PackageInfo>(relaxed = true)
        every { packageInfo.versionName } returns "1.2.3"
        every { packageManager.getPackageInfo(context.packageName, 0) } returns packageInfo

        val provider = RequestTrackingHeadersProvider()
        val header = provider.provideTrackingHeaders(context)

        assertTrue(header.contains("app_version=1.2.3"))
        assertTrue(header.contains("app_name=TestApp"))
        assertTrue(header.contains("os=Android"))
        assertTrue(header.contains("device_model="))
    }

    @Test
    public fun `provideTrackingHeaders returns header with custom app version and name from config`() {
        val context = mockk<Context>(relaxed = true)
        val config = IntegrationAppConfig(appVersion = "9.9.9", appName = "CustomApp")
        val provider = RequestTrackingHeadersProvider()
        val header = provider.provideTrackingHeaders(context, config)

        assertTrue(header.contains("app_version=9.9.9"))
        assertTrue(header.contains("app_name=CustomApp"))
    }
}