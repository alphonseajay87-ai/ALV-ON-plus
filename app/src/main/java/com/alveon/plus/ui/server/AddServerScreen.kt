package com.alveon.plus.ui.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import com.alveon.plus.AppGraph
import com.alveon.plus.data.ServerEntry
import com.alveon.plus.data.ServerType
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddServerScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ServerType.HTTP) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun buildServer(): ServerEntry? {
        var u = url.trim()
        if (u.isEmpty()) {
            message = "Enter the server address."; messageIsError = true
            return null
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        return ServerEntry(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { u.substringAfter("://").substringBefore('/') },
            url = u,
            type = type,
            username = user.trim(),
            password = pass,
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvButton("‹  Back", onBack)
            Spacer(Modifier.width(24.dp))
            Text("Add a server", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(4.dp))

        TvTextField("Name (optional)", name, { name = it })
        TvTextField("Address, e.g. 192.168.1.20:8080/movies", url, { url = it }, KeyboardType.Uri)

        Text("Server type", color = Palette.Muted, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton("Web folder / direct link", { type = ServerType.HTTP }, selected = type == ServerType.HTTP)
            TvButton("WebDAV", { type = ServerType.WEBDAV }, selected = type == ServerType.WEBDAV)
        }

        TvTextField("Username (optional)", user, { user = it })
        TvTextField("Password (optional)", pass, { pass = it }, KeyboardType.Password, hidden = true)

        message?.let {
            Text(it, color = if (messageIsError) Palette.Danger else Palette.Silver, style = MaterialTheme.typography.bodyLarge)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton(if (busy) "Testing…" else "Test connection", {
                val server = buildServer() ?: return@TvButton
                if (busy) return@TvButton
                busy = true; message = null
                scope.launch {
                    AppGraph.store.addServer(server) // so auth applies to the test request
                    val result = AppGraph.serverBrowser.list(server, server.url)
                    AppGraph.store.removeServer(server.id)
                    result.onSuccess {
                        message = "Connected. Found ${it.size} folders/videos."; messageIsError = false
                    }.onFailure {
                        message = it.message ?: "Couldn't connect."; messageIsError = true
                    }
                    busy = false
                }
            })
            TvButton("Save server", {
                val server = buildServer() ?: return@TvButton
                AppGraph.store.addServer(server)
                onSaved()
            }, selected = true)
        }
        Text(
            "SMB shares aren't supported yet. Most NAS boxes can also expose a web folder or WebDAV.",
            color = Palette.Muted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun TvTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    hidden: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Column {
        Text(label, color = Palette.Muted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Palette.Silver, fontSize = 19.sp),
            cursorBrush = SolidColor(Palette.Silver),
            visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .onFocusChanged { focused = it.hasFocus }
                .background(Palette.Surface, RoundedCornerShape(10.dp))
                .border(if (focused) 2.dp else 1.dp, if (focused) Palette.Silver else Palette.SurfaceHigh, RoundedCornerShape(10.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}
