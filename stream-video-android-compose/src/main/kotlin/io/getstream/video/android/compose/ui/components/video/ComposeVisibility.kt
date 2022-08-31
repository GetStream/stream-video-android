/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.compose.ui.components.video

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.CallSuper
import androidx.compose.ui.layout.LayoutCoordinates
import java.util.*

public abstract class VideoSinkVisibility : Observable() {
    public abstract fun isVisible(): Boolean
    public abstract fun size(): Size

    /**
     * This should be called whenever the visibility or size has changed.
     */
    public fun notifyChanged() {
        setChanged()
        notifyObservers()
    }

    /**
     * Called when this object is no longer needed and should clean up any unused resources.
     */
    @CallSuper
    public open fun close() {
        deleteObservers()
    }
}

public class ComposeVisibility : VideoSinkVisibility() {
    private var lastCoordinates: LayoutCoordinates? = null

    override fun isVisible(): Boolean {
        return (lastCoordinates?.isAttached == true && lastCoordinates?.size?.width != 0 && lastCoordinates?.size?.height != 0)
    }

    override fun size(): Size {
        val width = lastCoordinates?.size?.width ?: 0
        val height = lastCoordinates?.size?.height ?: 0
        return Size(width, height)
    }

    public fun onGloballyPositioned(layoutCoordinates: LayoutCoordinates) {
        val lastVisible = isVisible()
        val lastSize = size()
        lastCoordinates = layoutCoordinates

        if (lastVisible != isVisible() || lastSize != size()) {
            notifyChanged()
        }
    }

    public fun onDispose() {
        if (lastCoordinates == null) {
            return
        }
        lastCoordinates = null
        notifyChanged()
    }
}

public class ViewVisibility(private val view: View) : VideoSinkVisibility() {

    private val handler = Handler(Looper.getMainLooper())
    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        val lastVisibility = false
        val lastSize = Size(0, 0)

        override fun onGlobalLayout() {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                var shouldNotify = false
                val newVisibility = isVisible()
                val newSize = size()
                if (newVisibility != lastVisibility) {
                    shouldNotify = true
                }
                if (newSize != lastSize) {
                    shouldNotify = true
                }

                if (shouldNotify) {
                    notifyChanged()
                }
            }, 2000)
        }
    }

    init {
        view.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private val loc = IntArray(2)
    private val viewRect = Rect()
    private val windowRect = Rect()

    private fun isViewAncestorsVisible(view: View): Boolean {
        if (view.visibility != View.VISIBLE) {
            return false
        }
        val parent = view.parent as? View
        if (parent != null) {
            return isViewAncestorsVisible(parent)
        }
        return true
    }

    override fun isVisible(): Boolean {
        if (view.windowVisibility != View.VISIBLE || !isViewAncestorsVisible(view)) {
            return false
        }

        view.getLocationInWindow(loc)
        viewRect.set(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)

        view.getWindowVisibleDisplayFrame(windowRect)
        // Ensure window rect origin is at 0,0
        windowRect.offset(-windowRect.left, -windowRect.top)

        return viewRect.intersect(windowRect)
    }

    override fun size(): Size {
        return Size(view.width, view.height)
    }

    override fun close() {
        super.close()
        handler.removeCallbacksAndMessages(null)
        view.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
    }
}
