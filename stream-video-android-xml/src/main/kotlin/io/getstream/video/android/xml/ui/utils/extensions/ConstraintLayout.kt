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

package io.getstream.video.android.xml.ui.utils.extensions

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

internal inline fun ConstraintLayout.updateConstraints(actions: ConstraintSet.() -> Unit) {
    val set = ConstraintSet()
    set.clone(this)
    set.actions()
    set.applyTo(this)
}

internal fun ConstraintSet.constrainViewToParentBySide(view: View, side: Int) {
    connect(view.id, side, ConstraintSet.PARENT_ID, side)
}

internal fun ConstraintSet.constrainViewStartToEndOfView(startView: View, endView: View) {
    connect(startView.id, ConstraintSet.START, endView.id, ConstraintSet.END)
}

internal fun ConstraintSet.constrainViewEndToEndOfView(startView: View, endView: View) {
    connect(startView.id, ConstraintSet.END, endView.id, ConstraintSet.END)
}

internal fun ConstraintSet.constrainViewTopToTopOfView(startView: View, endView: View) {
    connect(startView.id, ConstraintSet.BOTTOM, endView.id, ConstraintSet.BOTTOM)
}

internal fun ConstraintSet.constrainViewBottomToBottomOfView(startView: View, endView: View) {
    connect(startView.id, ConstraintSet.BOTTOM, endView.id, ConstraintSet.BOTTOM)
}

internal fun ConstraintSet.constrainViewToParent(view: View) {
    connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
    connect(view.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
    connect(view.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
    connect(view.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
}

internal fun ConstraintSet.horizontalChainInParent(vararg views: View) {
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
