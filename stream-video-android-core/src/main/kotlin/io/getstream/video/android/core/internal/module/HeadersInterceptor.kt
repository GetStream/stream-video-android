package io.getstream.video.android.core.internal.module

import io.getstream.video.android.core.StreamVideo
import okhttp3.Interceptor
import okhttp3.Response

internal class HeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("X-Stream-Client", StreamVideo.buildSdkTrackingHeaders())
            .build()
        return chain.proceed(request)
    }
}