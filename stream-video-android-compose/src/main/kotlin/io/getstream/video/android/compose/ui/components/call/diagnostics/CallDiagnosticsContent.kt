package io.getstream.video.android.compose.ui.components.call.diagnostics

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
public fun CallDiagnosticsContent(
    call: Call,
    onCloseClick: () -> Unit,
) {
    var publisherText by remember { mutableStateOf("Initial Computed Value") }
    var subscriberText by remember { mutableStateOf("Initial Computed Value") }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = Color(0x60000000))
    ) {
        IconButton(onClick = onCloseClick) {
            Icon(Icons.Filled.Close, contentDescription = null)
        }

        val stats by call.statsReport.collectAsStateWithLifecycle()

        LaunchedEffect(stats) { // This effect re-launches every time dataFromFlow changes
            scope.launch(Dispatchers.Default) {
                val pubText = stats?.publisherReport?.parsed.toString()
                val subText = stats?.subscriberReport?.parsed.toString()
                withContext(Dispatchers.Main) {
                    publisherText = pubText
                    subscriberText = subText
                }
            }
        }

        val configuration = LocalConfiguration.current
        if (configuration.orientation == ORIENTATION_PORTRAIT) {
            Column {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text("Publisher")
                    }
                    item {
                        Text(publisherText)
                    }
                }
                LazyColumn(modifier = Modifier.weight(1.5f)) {
                    item {
                        Text("Subscriber")
                    }
                    item {
                        Text(subscriberText)
                    }
                }
            }
        } else {
            Row {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text("Publisher")
                    }
                    item {
                        Text(publisherText)
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text("Subscriber")
                    }
                    item {
                        Text(subscriberText)
                    }
                }
            }
        }
    }
}