package io.getstream.video.android.ui.common.util

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER
)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate Stream Video SDK Api, overriding this API may interfere on how the activity handles the call state."
)
internal annotation class StreamCallActivityDelicateApi()
