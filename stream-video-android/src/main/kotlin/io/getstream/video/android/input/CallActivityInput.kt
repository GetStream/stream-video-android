package io.getstream.video.android.input

import android.app.Activity
import io.getstream.video.android.activity.StreamCallActivity
import kotlin.reflect.KClass

public class CallActivityInput private constructor(
    override val className: String
) : CallAndroidInput() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallActivityInput) return false
        if (className != other.className) return false
        return true
    }

    override fun hashCode(): Int = className.hashCode()
    override fun toString(): String = "CallActivityInput(className='$className')"


    public companion object {
        public fun <T> from(clazz: Class<T>): CallActivityInput where T : Activity, T : StreamCallActivity {
            return CallActivityInput(clazz.name)
        }

        public fun <T> from(kClass: KClass<T>): CallActivityInput where T : Activity, T : StreamCallActivity {
            return CallActivityInput(
                kClass.qualifiedName
                    ?: error("qualifiedName cannot be obtained")
            )
        }
    }
}