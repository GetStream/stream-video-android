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

import io.getstream.video.android.service.StreamCallService
import kotlin.reflect.KClass

public class CallServiceInput private constructor(
    public val serviceClassName: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallServiceInput) return false
        if (serviceClassName != other.serviceClassName) return false
        return true
    }

    override fun hashCode(): Int {
        return serviceClassName.hashCode()
    }

    override fun toString(): String {
        return "CallServiceInput(serviceClassName='$serviceClassName')"
    }

    public companion object {
        public fun forClass(clazz: Class<out StreamCallService>): CallServiceInput = CallServiceInput(clazz.name)
        public fun forClass(kClass: KClass<out StreamCallService>): CallServiceInput = CallServiceInput(
            kClass.qualifiedName ?: error("qualifiedName cannot be obtained")
        )
    }
}
