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

package io.getstream.video.android.ui.join.barcode

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.getstream.video.android.DeeplinkingActivity
import io.getstream.video.android.R
import io.getstream.video.android.analytics.FirebaseEvents
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun BarcodeScanner(navigateBack: () -> Unit = {}) {
    val executor: Executor = Executors.newSingleThreadExecutor()
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
        .build()
    val barcodeScanner = remember { BarcodeScanning.getClient(options) }
    val qrCodeCallback = rememberQrCodeCallback()
    val imageAnalysis = ImageAnalysis.Builder()
        .build()
        .also {
            it.setAnalyzer(executor) { imageProxy ->
                processImageProxy(imageProxy, barcodeScanner, qrCodeCallback)
            }
        }

    // Camera permission
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA,
    )

    when (val cameraPermissionStatus = cameraPermissionState.status) {
        PermissionStatus.Granted -> {
            val color = VideoTheme.colors.brandPrimary
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(imageAnalysis = imageAnalysis)
                CornerRectWithArcs(color = color, cornerRadius = 32f, strokeWidth = 12f)
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    onClick = {
                        navigateBack()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    text = stringResource(id = R.string.scan_qr_code_to_enter),
                )
            }
        }

        is PermissionStatus.Denied -> {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    if (cameraPermissionStatus.shouldShowRationale) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(8.dp),
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            text = stringResource(
                                id = io.getstream.video.android.ui.common.R.string.stream_video_permissions_title,
                            ),
                        )
                        StreamButton(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            text = "Request permission",
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                        )
                    } else {
                        LaunchedEffect(key1 = "") {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    imageAnalysis: ImageAnalysis,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder()
                    .build()
                    .apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis,
                    )
                } catch (e: Exception) {
                    Log.e("BarcodeScanner", "Could not bind camera to lifecycle", e)
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
    )
}

@Composable
private fun BoxScope.CornerRectWithArcs(color: Color, cornerRadius: Float, strokeWidth: Float) {
    Canvas(
        modifier = Modifier
            .align(Alignment.Center)
            .size(250.dp, 250.dp)
            .padding(32.dp),
    ) {
        val cornerData = listOf(
            Pair(180f, Offset(0f, 0f)),
            Pair(270f, Offset(size.width - cornerRadius * 2, 0f)),
            Pair(90f, Offset(0f, size.height - cornerRadius * 2)),
            Pair(0f, Offset(size.width - cornerRadius * 2, size.height - cornerRadius * 2)),
        )
        cornerData.forEach {
            drawArc(
                color = color,
                startAngle = it.first,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(cornerRadius * 2, cornerRadius * 2),
                topLeft = it.second,
            )
        }
    }
}

@Composable
private fun rememberQrCodeCallback(): OnSuccessListener<Barcode> {
    val context = LocalContext.current
    val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }
    var codeScanned = false

    return remember {
        OnSuccessListener<Barcode> {
            if (codeScanned) {
                Log.d("BarcodeScanner", "Barcode already processed - skipping")
                return@OnSuccessListener
            }

            val linkUrl = try {
                Uri.parse(it.url?.url)
            } catch (e: Exception) {
                // Nothing will happen
                null
            }
            if (linkUrl != null) {
                codeScanned = true
                firebaseAnalytics.logEvent(FirebaseEvents.SCAN_QR_CODE, null)
                context.startActivity(DeeplinkingActivity.createIntent(context, linkUrl))
            } else {
                Toast.makeText(
                    context,
                    "Unrecognised meeting QR code format",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    barcodeScanner: BarcodeScanner,
    onBarcodeScanned: OnSuccessListener<Barcode>,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    // Handle the scanned barcode data
                    onBarcodeScanned.onSuccess(barcode)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@ExperimentalGetImage
@Preview
@Composable
private fun BarcodeScanUIPreview() {
    VideoTheme {
        BarcodeScanner()
    }
}
