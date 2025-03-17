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

package io.getstream.video.android.ui.call

import android.graphics.Bitmap
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QRCode(
    modifier: Modifier = Modifier,
    content: String,
    size: Dp = 80.dp,
    padding: Dp = 2.dp,
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx().toInt() }

    val qrCode = generateQRCode(content, sizePx)

    Box(
        modifier = Modifier
            .size(size)
            .background(Color.White)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = qrCode.asImageBitmap(),
            contentDescription = "QR Code for $content",
        )
    }
}

private fun generateQRCode(content: String, size: Int): Bitmap {
    return generateQRCodeBitmap(generateQRCodeBitMatrix(content, size), size)
}

private fun generateQRCodeBitMatrix(content: String, size: Int): BitMatrix {
    val qrCodeWriter = QRCodeWriter()
    val hints = hashMapOf<EncodeHintType, Any>().apply {
        put(EncodeHintType.CHARACTER_SET, "UTF-8")
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        put(EncodeHintType.MARGIN, 0)
    }

    return qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
}

private fun generateQRCodeBitmap(bitMatrix: BitMatrix, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            val color = if (bitMatrix[x, y]) BLACK else WHITE
            bitmap.setPixel(x, y, color)
        }
    }

    return bitmap
}
