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
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient

private const val DEFAULT_MEMORY_PERCENTAGE = 0.25
private const val DEFAULT_DISK_CACHE_PERCENTAGE = 0.02
private const val DISK_CACHE_DIRECTORY = "image_cache"

public class StreamImageLoaderFactory(
    private val context: Context,
    private val builder: ImageLoader.Builder.() -> Unit = {},
) : ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context).maxSizePercent(DEFAULT_MEMORY_PERCENTAGE).build()
            }
            .allowHardware(false)
            .crossfade(true)
            .okHttpClient {
                val cacheControlInterceptor = Interceptor { chain ->
                    chain.proceed(chain.request())
                        .newBuilder()
                        .header("Cache-Control", "max-age=3600,public")
                        .build()
                }
                // Don't limit concurrent network requests by host.
                val dispatcher = Dispatcher().apply { maxRequestsPerHost = maxRequests }

                OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .addNetworkInterceptor(cacheControlInterceptor)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_DIRECTORY))
                    .maxSizePercent(DEFAULT_DISK_CACHE_PERCENTAGE)
                    .build()
            }
            .apply(builder)
            .build()
    }
}
