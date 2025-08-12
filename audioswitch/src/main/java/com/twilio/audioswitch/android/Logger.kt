package com.twilio.audioswitch.android

internal interface Logger {
    var loggingEnabled: Boolean
    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable)
}
