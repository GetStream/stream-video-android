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

package io.getstream.video.android.core.e2ee

import io.getstream.video.android.core.utils.safeCallWithDefault
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Reflective binding to `org.webrtc.EncryptionManager`.
 *
 * TODO(e2ee): collapse this into direct calls once the WebRTC AAR carrying GetStream/webrtc#110 is
 *  published. The newest artifact on Maven Central (146.7.0) and the newest snapshot
 *  (148.0.1-SNAPSHOT) both predate that PR, so compiling against `org.webrtc.EncryptionManager`
 *  would break every module. Reflection keeps the rest of the E2EE integration buildable and
 *  testable in the meantime, and it costs nothing at runtime: these methods are called once per
 *  track attach and once per key change, never per frame.
 *
 * Methods are resolved by name and arity rather than by exact signature so that the binding still
 * links if the final API differs in nullability or parameter types. Anything that cannot be
 * resolved is left null and the corresponding operation becomes a no-op.
 */
internal class NativeE2EEBinding private constructor(private val managerClass: Class<*>) {

    companion object {
        private const val CLASS_NAME = "org.webrtc.EncryptionManager"

        /**
         * Resolved once. Null when the runtime WebRTC build has no E2EE support, which is what
         * [StreamEncryptionManager.isSupported] reports.
         */
        val instance: NativeE2EEBinding? by lazy {
            safeCallWithDefault(null) {
                val managerClass = Class.forName(CLASS_NAME)
                NativeE2EEBinding(managerClass).takeIf { it.isComplete }
            }
        }

        private fun Class<*>.findMethod(name: String, argCount: Int): Method? =
            methods.firstOrNull { it.name == name && it.parameterTypes.size == argCount }
                ?.apply { isAccessible = true }
    }

    private val createMethod = managerClass.findMethod("create", 1)
    private val encryptMethod = managerClass.findMethod("encrypt", 3)
    private val decryptMethod = managerClass.findMethod("decrypt", 3)
    private val setSharedKeyMethod = managerClass.findMethod("setSharedKey", 2)
    private val setKeyMethod = managerClass.findMethod("setKey", 3)
    private val removeSharedKeyMethod = managerClass.findMethod("removeSharedKey", 1)
    private val removeKeyMethod = managerClass.findMethod("removeKey", 2)
    private val removeAllKeysMethod = managerClass.findMethod("removeAllKeys", 0)
    private val setObserverMethod = managerClass.findMethod("setObserver", 1)
    private val disposeMethod = managerClass.findMethod("dispose", 0)

    /** The native `EncryptionManager.TrackType` enum, taken from the encrypt signature. */
    private val trackTypeClass: Class<*>? =
        encryptMethod?.parameterTypes?.getOrNull(2)?.takeIf { it.isEnum }

    /** A binding is only usable if it can create a manager and attach it to both directions. */
    private val isComplete: Boolean
        get() = createMethod != null && encryptMethod != null && decryptMethod != null

    fun create(userId: String): Any? = createMethod?.invoke(null, userId)

    fun encrypt(manager: Any, sender: Any, codec: String?, trackType: E2EETrackType?) {
        encryptMethod?.invoke(manager, sender, codec, nativeTrackType(trackType))
    }

    fun decrypt(manager: Any, receiver: Any, userId: String, trackType: E2EETrackType?) {
        decryptMethod?.invoke(manager, receiver, userId, nativeTrackType(trackType))
    }

    fun setSharedKey(manager: Any, keyIndex: Int, key: ByteArray) {
        setSharedKeyMethod?.invoke(manager, keyIndex, key)
    }

    fun setKey(manager: Any, userId: String, keyIndex: Int, key: ByteArray) {
        setKeyMethod?.invoke(manager, userId, keyIndex, key)
    }

    fun removeSharedKey(manager: Any, keyIndex: Int) {
        removeSharedKeyMethod?.invoke(manager, keyIndex)
    }

    fun removeKey(manager: Any, userId: String, keyIndex: Int) {
        removeKeyMethod?.invoke(manager, userId, keyIndex)
    }

    fun removeAllKeys(manager: Any) {
        removeAllKeysMethod?.invoke(manager)
    }

    fun dispose(manager: Any) {
        disposeMethod?.invoke(manager)
    }

    /**
     * Bridges [listener] onto whatever single-method observer interface the native `setObserver`
     * expects, discovered from its parameter type. Returns false when the runtime has no observer
     * support, so callers can decide whether that is worth reporting.
     */
    fun setObserver(manager: Any, listener: E2EEEventListener?): Boolean {
        val method = setObserverMethod ?: return false
        val observerType = method.parameterTypes.getOrNull(0)?.takeIf { it.isInterface } ?: return false
        if (listener == null) {
            method.invoke(manager, null)
            return true
        }
        val handler = InvocationHandler { _, invoked, args ->
            when (invoked.name) {
                "equals" -> listener === args?.getOrNull(0)
                "hashCode" -> System.identityHashCode(listener)
                "toString" -> "E2EEObserver($listener)"
                else -> {
                    args?.getOrNull(0)?.let { listener.onEvent(it.toE2EEEvent()) }
                    null
                }
            }
        }
        method.invoke(
            manager,
            Proxy.newProxyInstance(observerType.classLoader, arrayOf(observerType), handler),
        )
        return true
    }

    private fun nativeTrackType(trackType: E2EETrackType?): Any? {
        val enumClass = trackTypeClass ?: return null
        val target = trackType ?: return null
        // Returns null rather than throwing when the native enum has no matching constant, which
        // is the documented "no hint available" case for E2EEManager.
        return enumClass.enumConstants?.firstOrNull { (it as Enum<*>).name == target.name }
    }

    /** Reads `name` / `userId` / `reason` off the native event without knowing its type. */
    private fun Any.toE2EEEvent(): E2EEEvent = E2EEEvent(
        name = readString("name") ?: toString(),
        userId = readString("userId"),
        reason = readString("reason"),
    )

    private fun Any.readString(property: String): String? = safeCallWithDefault(null) {
        val getter = javaClass.findMethod("get${property.replaceFirstChar { it.uppercase() }}", 0)
            ?: javaClass.findMethod(property, 0)
        val value = getter?.invoke(this) ?: javaClass.fields
            .firstOrNull { it.name == property }
            ?.get(this)
        value?.toString()
    }
}
