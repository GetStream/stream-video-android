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

package io.getstream.video.android.xml.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.getstream.video.android.CallViewModelFactoryProvider
import io.getstream.video.android.StreamVideoProvider
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.permission.PermissionManagerImpl
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.viewmodel.CallViewModelFactory
import io.getstream.video.android.xml.databinding.ActivityCallBinding
import kotlinx.coroutines.launch

public abstract class AbstractXmlCallActivity :
    AppCompatActivity(),
    StreamVideoProvider,
    CallViewModelFactoryProvider {

    private val binding by lazy { ActivityCallBinding.inflate(layoutInflater) }

    private val streamVideo by lazy { getStreamVideo(this) }

    private val factory by lazy {
        getCallViewModelFactory() ?: defaultViewModelFactory()
    }

    private val callViewModel by viewModels<CallViewModel>(factoryProducer = { factory })

    /**
     * Provides the default ViewModel factory.
     */
    public fun defaultViewModelFactory(): CallViewModelFactory {
        return CallViewModelFactory(
            streamVideo = streamVideo,
            permissionManager = PermissionManagerImpl(applicationContext),
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private val permissionsContract = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val missing = getMissingPermissions()
        val deniedCamera = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        val deniedMicrophone =
            !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

        when {
            missing.isNotEmpty() && !deniedCamera && !deniedMicrophone -> requestPermissions(missing)
            isGranted -> startVideoFlow()
            deniedCamera || deniedMicrophone -> showPermissionsDialog()
            else -> {
                checkPermissions()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        /*binding.participantsView.layoutManager = GridLayoutManager(this, 4, RecyclerView.HORIZONTAL, false)
        binding.participantsView.adapter = object : RecyclerView.Adapter<ItemViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
                return ItemViewHolder(AdapterItemBinding.inflate(layoutInflater))
            }

            override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
                holder.bind(position)
            }

            override fun getItemCount(): Int = 10

        }*/

        lifecycleScope.launchWhenCreated {
            callViewModel.streamCallState.collect {
                if (it is StreamCallState.Idle) {
                    // TODO
                    // finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        } else {
            startVideoFlow()
        }
        /*lifecycleScope.launch {
            repeat(4) { index ->
                binding.participantsView.show(count = 4 - index)
//                binding.participantsView.show(count = index + 1)
                delay(1500)
            }
        }*/
    }

    private fun startSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                val uri = Uri.fromParts("package", packageName, null)
                data = uri
            }
        )
    }

    private fun startVideoFlow() {
        val isInitialized = callViewModel.isVideoInitialized.value
        if (isInitialized) return
        callViewModel.connectToCall(getDefaultCallSettings())
    }

    protected fun getDefaultCallSettings(): CallSettings = CallSettings(
        audioOn = false,
        videoOn = true,
        speakerOn = false
    )

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        val missing = getMissingPermissions()

        if (missing.isNotEmpty()) {
            requestPermissions(missing)
        } else {
            startVideoFlow()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermissions(permissions: Array<out String>) {
        val deniedCamera = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        val deniedMicrophone =
            !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

        if (!deniedCamera && !deniedMicrophone) {
            permissionsContract.launch(permissions.first())
        } else {
            showPermissionsDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getMissingPermissions(): Array<out String> {
        val permissionsToCheck = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val missing = permissionsToCheck
            .map { it to (checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED) }
            .filter { (_, isGranted) -> !isGranted }
            .map { it.first }

        return missing.toTypedArray()
    }

    private fun showPermissionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions required to launch the app")
            .setMessage("Open settings to allow camera and microphone permissions.")
            .setPositiveButton("Launch settings") { dialog, _ ->
                startSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                finish()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
