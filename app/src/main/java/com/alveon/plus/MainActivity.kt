package com.alveon.plus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.alveon.plus.navigation.AlveonNavHost
import com.alveon.plus.ui.theme.AlveonTheme

class MainActivity : ComponentActivity() {
    private var externalUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalUri = videoUriFrom(intent)
        setContent {
            AlveonTheme {
                AlveonNavHost(
                    externalUri = externalUri,
                    onExternalHandled = { externalUri = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        videoUriFrom(intent)?.let { externalUri = it }
    }

    private fun videoUriFrom(intent: Intent?): String? =
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.toString() else null
}
