package com.twilio.audioswitch.android

interface PermissionsCheckStrategy {
    fun hasPermissions(): Boolean
}
