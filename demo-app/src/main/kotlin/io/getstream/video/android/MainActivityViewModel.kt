package io.getstream.video.android

import androidx.lifecycle.ViewModel
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    appUpdateManager: AppUpdateManager
) : ViewModel() {
    val appUpdateResultFlow: Flow<AppUpdateResult> = appUpdateManager
        .requestUpdateFlow()
        .catch { emit(AppUpdateResult.NotAvailable) }
}