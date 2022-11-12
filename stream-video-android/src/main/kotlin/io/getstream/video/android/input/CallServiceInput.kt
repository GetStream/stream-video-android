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

package io.getstream.video.android.input

import android.app.Service
import io.getstream.video.android.StreamVideoProvider
import kotlin.reflect.KClass

public class CallServiceInput private constructor(
    override val className: String
) : CallAndroidInput() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallServiceInput) return false
        if (className != other.className) return false
        return true
    }

    override fun hashCode(): Int = className.hashCode()
    override fun toString(): String = "CallServiceInput(className='$className')"

    public companion object {
        public fun <T> from(clazz: Class<T>): CallServiceInput where T : Service, T : StreamVideoProvider {
            return CallServiceInput(clazz.name)
        }

        public fun <T> from(kClass: KClass<T>): CallServiceInput where T : Service, T : StreamVideoProvider {
            return CallServiceInput(
                kClass.qualifiedName
                    ?: error("qualifiedName cannot be obtained")
            )
        }
    }
}
