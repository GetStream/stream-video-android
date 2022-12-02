/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.xml.utils.extensions

import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.getstream.log.StreamLog

private const val TAG = "Call:ViewGroupExt"

internal inline val ViewGroup.inflater: LayoutInflater
    get() = LayoutInflater.from(context)

internal val ViewGroup.streamThemeInflater: LayoutInflater
    get() = LayoutInflater.from(context.createStreamThemeWrapper())

internal fun ViewGroup.initToolbar(toolbar: Toolbar) {
    val activity: AppCompatActivity = context as? AppCompatActivity
        ?: (context as? ContextWrapper)?.baseContext as? AppCompatActivity
        ?: kotlin.run {
            StreamLog.w(TAG) { "[initToolbar] rejected (no AppCompatActivity found)" }
            return
        }
    activity.apply {
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayShowTitleEnabled(false)
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)

            /*ContextCompat.getDrawable(context(), R.drawable.ic_icon_left)?.apply {
                setTint(ContextCompat.getColor(context, R.color.stream_ui_black))
            }?.let(toolbar::setNavigationIcon)*/

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }
}
