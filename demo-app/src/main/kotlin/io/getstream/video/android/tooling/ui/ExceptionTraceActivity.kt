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

package io.getstream.video.android.tooling.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.os.bundleOf
import io.getstream.video.android.compose.theme.VideoTheme

internal class ExceptionTraceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val throwable = intent.getStringExtra(EXTRA_EXCEPTION)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (throwable == null || packageName == null) {
            finish()
        }

        setContent {
            VideoTheme { ExceptionTraceScreen(packageName = packageName!!, message = throwable!!) }
        }
    }

    internal companion object {
        private const val EXTRA_EXCEPTION = "EXTRA_EXCEPTION"
        private const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        private const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"

        fun getIntent(context: Context, exception: String, message: String, packageName: String) =
            Intent(context, ExceptionTraceActivity::class.java).apply {
                putExtras(
                    bundleOf(
                        EXTRA_EXCEPTION to exception,
                        EXTRA_MESSAGE to message,
                        EXTRA_PACKAGE_NAME to packageName,
                    ),
                )
            }
    }
}
