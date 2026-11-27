package com.alveon.plus.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.alveon.plus.AppGraph
import com.alveon.plus.data.StorageBrowser
import com.alveon.plus.data.StorageLocation

/** Returns a function that opens the system folder picker and remembers the chosen folder. */
@Composable
fun rememberFolderPicker(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            AppGraph.store.addLocation(StorageLocation(StorageBrowser.displayName(context, uri), uri.toString()))
        }
    }
    return remember(launcher) { { launchPicker(context, launcher) } }
}

private fun launchPicker(context: Context, launcher: ManagedActivityResultLauncher<Uri?, Uri?>) {
    try {
        launcher.launch(null)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "This device has no folder picker. Install a file manager app, or add a server instead.",
            Toast.LENGTH_LONG,
        ).show()
    }
}
