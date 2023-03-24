/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.tooling.exception

import android.annotation.SuppressLint
import io.getstream.log.StreamLog
import io.getstream.video.android.tooling.handler.StreamGlobalExceptionHandler

/**
 * Installer for the [StreamGlobalExceptionHandler].
 */
public object StreamGlobalException {

    /**
     * Represents if [StreamGlobalExceptionHandler] is already installed or not.
     * Lets you know if the internal [StreamGlobalExceptionHandler] instance is being used as the
     * uncaught exception handler when true or if it is using the default one if false.
     */
    @JvmStatic
    public var isInstalled: Boolean = false
        private set

    /**
     * [StreamGlobalExceptionHandler] instance to be used.
     */
    @SuppressLint("StaticFieldLeak")
    @Volatile
    @PublishedApi
    internal var internalExceptionHandler: StreamGlobalExceptionHandler? = null
        private set(value) {
            isInstalled = value != null
            field = value
        }

    /**
     * Installs a new [StreamGlobalExceptionHandler] instance to be used.
     */
    @JvmStatic
    public fun install(exceptionHandler: StreamGlobalExceptionHandler) {
        synchronized(this) {
            if (isInstalled) {
                StreamLog.e("StreamLog") {
                    "The $internalExceptionHandler is already installed but you've tried to " +
                        "install a new exception handler: $exceptionHandler"
                }
            }
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
            internalExceptionHandler = exceptionHandler
        }
    }
}
