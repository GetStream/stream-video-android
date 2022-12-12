package io.getstream.video.android.common.util

import io.getstream.video.android.model.CallUser

// TODO add internal annotation
public fun buildSmallCallText(participants: List<CallUser>): String {
    val names = participants.map { it.name }

    return if (names.isEmpty()) {
        "none"
    } else if (names.size == 1) {
        names.first()
    } else {
        "${names[0]} and ${names[1]}"
    }
}

public fun buildLargeCallText(participants: List<CallUser>): String {
    if (participants.isEmpty()) return "No participants"
    val initial = buildSmallCallText(participants)
    if (participants.size == 1) return initial

    return "$initial and +${participants.size - 2} more"
}