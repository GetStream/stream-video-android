package io.getstream.video.android.pages

import com.kaspersky.kaspresso.screens.KScreen
import io.getstream.video.android.MainActivity
import io.getstream.video.android.R

object SamplePage : KScreen<SamplePage>() {

    override val layoutId: Int = R.layout.activity_main
    override val viewClass: Class<*> = MainActivity::class.java

}