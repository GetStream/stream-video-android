package io.getstream.video.android.input

import android.app.Service
import io.getstream.video.android.service.StreamCallService
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
        public fun <T> from(clazz: Class<T>): CallServiceInput where T : Service, T : StreamCallService {
            return CallServiceInput(clazz.name)
        }

        public fun <T> from(kClass: KClass<T>): CallServiceInput where T : Service, T : StreamCallService {
            return CallServiceInput(
                kClass.qualifiedName
                    ?: error("qualifiedName cannot be obtained")
            )
        }
    }

}