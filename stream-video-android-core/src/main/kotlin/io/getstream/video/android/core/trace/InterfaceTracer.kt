/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.trace

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal inline fun <reified T> tracedWith(
    target: T,
    tracer: Tracer,
): T {
    val clazz = target!!::class.java
    val handler = InterfaceMethodInvocationCounter(tracer, target)
    return Proxy.newProxyInstance(clazz.classLoader, arrayOf(T::class.java), handler) as T
}

/**
 * Count the invocations of the methods of the target object.
 *
 * @param scope The scope in which the counter should run.
 * @param target The target object to count the invocations of.
 * @param config The configuration for the counter.
 */
internal class InterfaceMethodInvocationCounter<T>(
    private val tracer: Tracer,
    private val target: T,
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        tracer.trace(method.name, args)
        return method.invoke(target, *(args ?: emptyArray()))
    }
}
