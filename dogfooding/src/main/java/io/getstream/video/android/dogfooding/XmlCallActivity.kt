package io.getstream.video.android.dogfooding

import android.content.Context
import io.getstream.video.android.xml.AbstractXmlCallActivity

class XmlCallActivity: AbstractXmlCallActivity() {
    override fun getStreamVideo(context: Context) = context.dogfoodingApp.streamVideo
}