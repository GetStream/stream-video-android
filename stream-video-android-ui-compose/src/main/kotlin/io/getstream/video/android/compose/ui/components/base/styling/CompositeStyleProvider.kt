package io.getstream.video.android.compose.ui.components.base.styling

/**
 * Composite styles provider providing various components styles.
 */
public open class CompositeStyleProvider(
    public val iconStyles: IconStyleProvider = IconStyles,
    public val textFieldStyles: TextFieldStyleProvider = StreamTextFieldStyles,
    public val textStyles: TextStyleProvider = StreamTextStyles,
    public val buttonStyles: ButtonStyleProvider = ButtonStyles,
    public val dialogStyles: DialogStyleProvider = StreamDialogStyles,
    public val badgeStyles: BadgeStyleProvider = StreamBadgeStyles,
)