package io.getstream.video.android.core

import io.getstream.video.android.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class MemberState(user: User) {
    /**
     * If you are a participant or not
     */
    private val _isParticipant: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val videoEnabled: StateFlow<Boolean> = _isParticipant
}