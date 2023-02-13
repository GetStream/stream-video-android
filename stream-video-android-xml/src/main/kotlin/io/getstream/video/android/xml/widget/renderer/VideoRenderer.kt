package io.getstream.video.android.xml.widget.renderer

import io.getstream.video.android.xml.widget.participant.RendererInitializer

internal interface VideoRenderer {

    public fun setRendererInitializer(rendererInitializer: RendererInitializer)

}