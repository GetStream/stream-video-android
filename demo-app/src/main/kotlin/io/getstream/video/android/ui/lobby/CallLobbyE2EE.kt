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

package io.getstream.video.android.ui.lobby

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamDialogPositiveNegative
import io.getstream.video.android.compose.ui.components.base.StreamTextField
import io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamDialogStyles
import io.getstream.video.android.core.Call
import kotlinx.coroutines.launch
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salt, iteration count and key length are fixed across the web, iOS and Android demos so the same
 * passphrase produces the same key on every platform — otherwise a cross-platform test call just
 * silently fails to decrypt. Real integrations distribute key material out of band instead of
 * deriving it from a secret typed into a UI, which is why this lives in the demo and not the SDK.
 */
private const val KDF_SALT = "stream-e2ee"
private const val KDF_ITERATIONS = 100_000
private const val KDF_KEY_BITS = 128

/** Derives the AES-128 key that the demo shares between participants. */
internal fun deriveE2EEKey(passphrase: String): ByteArray {
    val spec = PBEKeySpec(
        passphrase.toCharArray(),
        KDF_SALT.toByteArray(Charsets.UTF_8),
        KDF_ITERATIONS,
        KDF_KEY_BITS,
    )
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
}

/**
 * Lobby toggle for end-to-end encryption. It belongs here rather than in the in-call menu because
 * keys have to be set before [Call.join] — the publisher and subscriber capture the encryption
 * manager when the session is created.
 */
@Composable
internal fun E2EELobbyButton(
    call: Call,
    onEnable: suspend (String) -> Unit,
    onDisable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val encrypted by call.state.e2eeEnabled.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    IconButton(
        modifier = modifier.testTag("Stream_LobbyE2EEButton"),
        onClick = {
            if (encrypted) {
                onDisable()
            } else {
                showDialog = true
            }
        },
    ) {
        Icon(
            imageVector = if (encrypted) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = if (encrypted) "Disable encryption" else "Enable encryption",
            tint = if (encrypted) {
                VideoTheme.colors.brandPrimary
            } else {
                VideoTheme.colors.basePrimary
            },
        )
    }

    if (showDialog) {
        E2EEPassphraseDialog(
            error = error,
            onDismiss = {
                showDialog = false
                error = null
            },
            onConfirm = { passphrase ->
                // Surfaced rather than swallowed: joining a call the user believes is encrypted
                // when it is not would be worse than refusing to enable it.
                scope.launch {
                    val failure = runCatching {
                        onEnable(passphrase)
                    }.exceptionOrNull()
                    error = failure?.message ?: failure?.javaClass?.simpleName
                    showDialog = failure != null
                }
            },
        )
    }
}

@Composable
private fun E2EEPassphraseDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf(TextFieldValue("")) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val fieldError = validationError ?: error

    StreamDialogPositiveNegative(
        style = StreamDialogStyles.defaultDialogStyle(),
        onDismiss = onDismiss,
        icon = Icons.Default.Lock,
        title = "Encrypt this call",
        contentText = "Everyone on the call has to use the same passphrase. Recording, " +
            "transcription, closed captions and HLS are unavailable on encrypted calls.",
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                StreamTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("Stream_E2EEPassphraseField"),
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        validationError = null
                    },
                    placeholder = "Passphrase",
                    error = fieldError != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                    ),
                )
                if (fieldError != null) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = fieldError,
                        style = VideoTheme.typography.bodyS,
                        color = VideoTheme.colors.alertWarning,
                    )
                }
            }
        },
        positiveButton = Triple("Enable", ButtonStyles.secondaryButtonStyle()) {
            if (passphrase.text.isBlank()) {
                validationError = "Passphrase cannot be empty."
            } else {
                onConfirm(passphrase.text)
            }
        },
        negativeButton = Triple("Cancel", ButtonStyles.tertiaryButtonStyle()) {
            onDismiss()
        },
    )
}
