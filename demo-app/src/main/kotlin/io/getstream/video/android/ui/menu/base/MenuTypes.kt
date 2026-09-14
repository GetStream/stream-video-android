/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.ui.menu.base

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

/**
 * The icon of a [MenuItem]. Most menu icons come from the design system drawables, but a few
 * entries have no design system equivalent and keep their material icon.
 */
sealed class MenuIcon {

    /** An icon from the design system drawables. */
    data class Resource(@DrawableRes val id: Int) : MenuIcon()

    /** A material icon, for the entries the design system has no icon for. */
    data class Vector(val image: ImageVector) : MenuIcon()
}

fun menuIcon(@DrawableRes id: Int): MenuIcon = MenuIcon.Resource(id)

fun menuIcon(image: ImageVector): MenuIcon = MenuIcon.Vector(image)

@Composable
internal fun MenuIcon.painter(): Painter = when (this) {
    is MenuIcon.Resource -> painterResource(id)
    is MenuIcon.Vector -> rememberVectorPainter(image)
}

/**
 * Parent class on all menu items.
 *
 * @param title - title of the item, used to display in the menu, or a subtitle to the sub menu.
 * @param icon - the icon to be shown with the item.
 * @param highlight -  if the icon should be highlighted or not (usually tinted with primary color)
 */
abstract class MenuItem(
    val title: String,
    val icon: MenuIcon,
    val highlight: Boolean = false,
)

/**
 * Same as [MenuItem] but additionally has an action associated with it.
 *
 * @param action - the action that will execute when the item is clicked.
 */
class ActionMenuItem(
    title: String,
    icon: MenuIcon,
    highlight: Boolean = false,
    val action: () -> Unit,
) : MenuItem(title, icon, highlight)

/**
 * Unlike the [ActionMenuItem] the [SubMenuItem] contains a list of [MenuItem] that create a new submenu.
 * Clicking a [SubMenuItem] will show the [items].
 *
 * @param items - the items will be shown in the menu.
 */
open class SubMenuItem(title: String, icon: MenuIcon, val items: List<MenuItem>) :
    MenuItem(title, icon)

/**
 * Similar to the [SubMenuItem] the [DynamicSubMenuItem] contains an [itemsLoader] function to load the items.
 * The [DynamicMenu] knows how to invoke this function to dynamically load the items while showing a progress indicator.
 *
 * @param itemsLoader the items provider function.
 */
class DynamicSubMenuItem(
    title: String,
    icon: MenuIcon,
    val itemsLoader: suspend () -> List<MenuItem>,
) : SubMenuItem(title, icon, emptyList())
