package io.getstream.video.android.audio

internal interface AudioManagerAdapter {

    fun hasEarpiece(): Boolean

    fun hasSpeakerphone(): Boolean

    fun setAudioFocus()

    fun enableBluetoothSco(enable: Boolean)

    fun enableSpeakerphone(enable: Boolean)

    fun mute(mute: Boolean)

    fun cacheAudioState()

    fun restoreAudioState()

}