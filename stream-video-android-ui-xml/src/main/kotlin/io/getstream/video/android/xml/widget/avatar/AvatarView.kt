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

package io.getstream.video.android.xml.widget.avatar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.AbsoluteCornerSize
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.utils.initials
import io.getstream.video.android.model.User
import io.getstream.video.android.xml.utils.extensions.createStreamThemeWrapper
import io.getstream.video.android.xml.utils.extensions.load
import io.getstream.video.android.xml.widget.avatar.internal.AvatarPlaceholderDrawable

/**
 * Represents the avatar that's shown on the Messages screen or in headers of DMs.
 *
 * Based on the state we either show an image or name initials.
 */
public class AvatarView : ShapeableImageView {

    /**
     * Style for the avatar.
     */
    private lateinit var avatarStyle: AvatarStyle

    /**
     * [Paint] that will be used to draw the border.
     */
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context.createStreamThemeWrapper(),
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs)
    }

    /**
     * Loads the data for the avatar, from the user.
     *
     * @param user The [User] for which we want to show the avatar.
     */
    public fun setData(initials: String, userImage: String?) {
        load(
            data = userImage,
            placeholderDrawable = AvatarPlaceholderDrawable(
                context = context,
                initials = initials,
                initialsTextStyle = avatarStyle.avatarInitialsTextStyle,
            ),
        )
    }

    /**
     * Loads the data for the avatar, from the user.
     *
     * @param user The [CallUser] for which we want to show the avatar for.
     */
    public fun setData(user: CallUser) {
        load(
            data = user.imageUrl,
            placeholderDrawable = AvatarPlaceholderDrawable(
                context = context,
                initials = (user.name.takeUnless { it.isNullOrBlank() } ?: user.id).initials(),
                initialsTextStyle = avatarStyle.avatarInitialsTextStyle,
            ),
        )
    }

    /**
     * Sets the text we want to show inside the avatar.
     *
     * @param text The text we wish to show inside the view.
     */
    public fun setData(text: String) {
        background = AvatarPlaceholderDrawable(
            context = context,
            initials = text,
            initialsTextStyle = avatarStyle.avatarInitialsTextStyle,
        )
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        setAvatarStyle(AvatarStyle(context, attrs))
    }

    private fun setAvatarStyle(avatarStyle: AvatarStyle) {
        this.avatarStyle = avatarStyle

        val padding = (avatarStyle.avatarBorderWidth - AVATAR_SIZE_EXTRA).coerceAtLeast(0)
        setPadding(padding, padding, padding, padding)

        borderPaint.color = avatarStyle.avatarBorderColor
        borderPaint.strokeWidth = avatarStyle.avatarBorderWidth.toFloat()

        shapeAppearanceModel = when (avatarStyle.avatarShape) {
            AvatarShape.CIRCLE -> {
                ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCornerSizes(RelativeCornerSize(0.5f))
                    .build()
            }
            AvatarShape.ROUND_RECT -> {
                ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCornerSizes(AbsoluteCornerSize(avatarStyle.borderRadius))
                    .build()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(0, widthMeasureSpec)
        val height = resolveSize(0, heightMeasureSpec)
        val avatarViewSize = if (width > height) height else width

        setMeasuredDimension(avatarViewSize, avatarViewSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBorder(canvas)
    }

    /**
     * Draws the avatar border on the canvas.
     *
     * @param canvas The [Canvas] to draw on.
     */
    private fun drawBorder(canvas: Canvas) {
        if (avatarStyle.avatarBorderWidth == 0) return

        val borderOffset = (avatarStyle.avatarBorderWidth / 2).toFloat()
        when (avatarStyle.avatarShape) {
            AvatarShape.ROUND_RECT -> {
                canvas.drawRoundRect(
                    borderOffset,
                    borderOffset,
                    width.toFloat() - borderOffset,
                    height.toFloat() - borderOffset,
                    avatarStyle.borderRadius,
                    avatarStyle.borderRadius,
                    borderPaint,
                )
            }
            else -> {
                canvas.drawCircle(
                    width / 2f,
                    height / 2f,
                    width / 2f - borderOffset,
                    borderPaint,
                )
            }
        }
    }

    private companion object {
        private const val AVATAR_SIZE_EXTRA = 1
    }
}
