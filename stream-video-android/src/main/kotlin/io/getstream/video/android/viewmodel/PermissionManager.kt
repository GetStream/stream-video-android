package io.getstream.video.android.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

public interface PermissionManager {

    public val hasAudioPermission: Boolean
    public val hasVideoPermission: Boolean

}

public class PermissionManagerImpl(
    private val appContext: Context
) : PermissionManager {

    override val hasAudioPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    override val hasVideoPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

}