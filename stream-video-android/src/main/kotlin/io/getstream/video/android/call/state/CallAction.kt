package io.getstream.video.android.call.state

/**
 * Represents various actions users can take while in a call.
 */
public sealed class CallAction

/**
 * Action to toggle if the speakerphone is on or off.
 */
public data class ToggleSpeakerphone(
    val isEnabled: Boolean
) : CallAction()

/**
 * Action to toggle if the camera is on or off.
 */
public data class ToggleCamera(
    val isEnabled: Boolean
) : CallAction()

/**
 * Action to toggle if the microphone is on or off.
 */
public data class ToggleMicrophone(
    val isEnabled: Boolean
) : CallAction()

/**
 * Action to flip the active camera.
 */
public object FlipCamera : CallAction()

/**
 * Action to leave the call.
 */
public object LeaveCall : CallAction()

/**
 * Custom action used to handle any custom behavior with the given [data], such as opening chat,
 * inviting people, sharing the screen and more.
 */
public data class CustomAction(
    val data: Map<Any, Any> = emptyMap()
) : CallAction()
