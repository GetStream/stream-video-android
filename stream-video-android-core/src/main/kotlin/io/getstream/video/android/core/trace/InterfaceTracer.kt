package io.getstream.video.android.core.trace

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

inline fun <reified T> tracedWith(
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
class InterfaceMethodInvocationCounter<T>(
    private val tracer: Tracer,
    private val target: T,
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        tracer.trace(method.name, args)
        return method.invoke(target, *(args ?: emptyArray()))
    }
}