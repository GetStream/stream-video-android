/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.menu.base

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Parent class on all menu items.
 *
 * @param title - title of the item, used to display in the menu, or a subtitle to the sub menu.
 * @param icon - the icon to be shown with the item.
 * @param highlight -  if the icon should be highlighted or not (usually tinted with primary color)
 */
internal abstract class MenuItem(
    val title: String,
    val icon: ImageVector,
    val highlight: Boolean = false,
)

/**
 * Same as [MenuItem] but additionally has an action associated with it.
 *
 * @param action - the action that will execute when the item is clicked.
 */
internal class ActionMenuItem(
    title: String,
    icon: ImageVector,
    highlight: Boolean = false,
    val action: () -> Unit,
) : MenuItem(title, icon, highlight)

/**
 * Unlike the [ActionMenuItem] the [SubMenuItem] contains a list of [MenuItem] that create a new submenu.
 * Clicking a [SubMenuItem] will show the [items].
 *
 * @param items - the items will be shown in the menu.
 */
internal open class SubMenuItem(title: String, icon: ImageVector, val items: List<MenuItem>) :
    MenuItem(title, icon)

/**
 * Similar to the [SubMenuItem] the [DynamicSubMenuItem] contains an [itemsLoader] function to load the items.
 * The [DynamicMenu] knows how to invoke this function to dynamically load the items while showing a progress indicator.
 *
 * @param itemsLoader the items provider function.
 */
internal class DynamicSubMenuItem(
    title: String,
    icon: ImageVector,
    val itemsLoader: suspend () -> List<MenuItem>,
) : SubMenuItem(title, icon, emptyList())
