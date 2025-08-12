package com.twilio.audioswitch

import com.twilio.audioswitch.android.Logger

class UnitTestLogger(override var loggingEnabled: Boolean = true) : Logger {

    override fun d(tag: String, message: String) {
        printMessage(message)
    }

    override fun w(tag: String, message: String) {
        printMessage(message)
    }

    override fun e(tag: String, message: String) {
        printMessage(message)
    }

    override fun e(tag: String, message: String, throwable: Throwable) {
        printMessage(message)
        throwable.printStackTrace()
    }

    private fun printMessage(message: String) {
        if (loggingEnabled) {
            println(message)
        }
    }
}
