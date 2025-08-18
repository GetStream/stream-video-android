package io.getstream.video.android

import android.content.Intent

data class IncomingNotificationIntent(val acceptIntent: Intent, val rejectIntent: Intent)