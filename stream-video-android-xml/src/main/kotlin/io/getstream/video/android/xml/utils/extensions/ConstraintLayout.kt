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

package io.getstream.video.android.xml.utils.extensions

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children

internal inline fun ConstraintLayout.setConstraints(actions: ConstraintSet.() -> Unit) {
    val set = ConstraintSet()
    set.actions()
    set.applyTo(this)
}

internal inline fun ConstraintLayout.updateConstraints(
    clearAllConstraints: Boolean = false,
    actions: ConstraintSet.() -> Unit,
) {
    val set = ConstraintSet()
    set.clone(this)
    if (clearAllConstraints) {
        children.forEach { set.clearConstraints(it.id) }
    }
    set.actions()
    set.applyTo(this)
}

internal fun ConstraintSet.clearConstraints(viewId: Int) {
    clear(viewId, ConstraintSet.START)
    clear(viewId, ConstraintSet.END)
    clear(viewId, ConstraintSet.TOP)
    clear(viewId, ConstraintSet.BOTTOM)
    clear(viewId, ConstraintSet.LEFT)
    clear(viewId, ConstraintSet.RIGHT)
}

internal fun ConstraintSet.constrainViewToParentBySide(view: View, side: Int, margin: Int = 0) {
    connect(view.id, side, ConstraintSet.PARENT_ID, side, margin)
}

internal fun ConstraintSet.constrainViewStartToEndOfView(startView: View, endView: View, margin: Int = 0) {
    connect(startView.id, ConstraintSet.START, endView.id, ConstraintSet.END, margin)
}

internal fun ConstraintSet.constrainViewEndToEndOfView(startView: View, endView: View, margin: Int = 0) {
    connect(startView.id, ConstraintSet.END, endView.id, ConstraintSet.END, margin)
}

internal fun ConstraintSet.constrainViewEndToStartOfView(startView: View, endView: View, margin: Int = 0) {
    connect(startView.id, ConstraintSet.END, endView.id, ConstraintSet.START, margin)
}

internal fun ConstraintSet.constrainViewBottomToTopOfView(startView: View, endView: View, margin: Int = 0) {
    connect(startView.id, ConstraintSet.BOTTOM, endView.id, ConstraintSet.TOP, margin)
}

internal fun ConstraintSet.constrainViewTopToBottomOfView(startView: View, endView: View, margin: Int = 0) {
    connect(startView.id, ConstraintSet.TOP, endView.id, ConstraintSet.BOTTOM, margin)
}

internal fun ConstraintSet.constrainViewToParent(view: View) {
    connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
    connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    connect(view.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    connect(view.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
}

internal fun ConstraintSet.horizontalChainInParent(views: List<View>) {
    createHorizontalChain(
        ConstraintSet.PARENT_ID,
        ConstraintSet.LEFT,
        ConstraintSet.PARENT_ID,
        ConstraintSet.RIGHT,
        views.map(View::getId).toIntArray(),
        null,
        ConstraintSet.CHAIN_SPREAD
    )
}

internal fun ConstraintSet.horizontalChainInParent(vararg views: View) {
    horizontalChainInParent(views.toList())
}

internal fun ConstraintSet.verticalChainInParent(vararg views: View) {
    createVerticalChain(
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
        views.map(View::getId).toIntArray(),
        null,
        ConstraintSet.CHAIN_SPREAD
    )
}
