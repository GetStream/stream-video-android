package io.getstream.chat.android.dogfooding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.getstream.video.android.model.CallInput
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.viewmodel.CallViewModelFactory

class CallActivity : AppCompatActivity() {

    private val input: CallInput by lazy {
        requireNotNull(intent.getSerializableExtra(KEY_CALL_INPUT) as? CallInput)
    }

    private val factory by lazy {
        CallViewModelFactory(input, dogfoodingApp.streamCalls, dogfoodingApp.credentialsProvider)
    }

    private val callViewModel by viewModels<CallViewModel>(factoryProducer = { factory })

    @RequiresApi(M)
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
        super.onCreate(savedInstanceState)

        setContent {
            VideoCallContent(callViewModel, onLeaveCall = ::leaveCall)
        }
    }

    private fun leaveCall() {
        callViewModel.leaveCall()
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= M) {
            checkPermissions()
        } else {
            startVideoFlow()
        }
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
        callViewModel.connectToCall()
    }

    @RequiresApi(M)
    private fun checkPermissions() {
        val missing = getMissingPermissions()

        if (missing.isNotEmpty()) {
            requestPermissions(missing)
        } else {
            startVideoFlow()
        }
    }

    @RequiresApi(M)
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

    @RequiresApi(M)
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

    companion object {
        private const val KEY_CALL_INPUT = "call_input"

        internal fun getIntent(
            context: Context,
            callInput: CallInput
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(KEY_CALL_INPUT, callInput)
            }
        }
    }
}