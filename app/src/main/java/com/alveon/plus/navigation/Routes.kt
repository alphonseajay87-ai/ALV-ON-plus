package com.alveon.plus.navigation

import android.net.Uri
import com.alveon.plus.data.ServerEntry
import com.alveon.plus.data.StorageLocation

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SERIES = "series?key={key}"
    const val ADD_SERVER = "addServer"
    const val SETTINGS = "settings"
    const val BROWSE = "browse?kind={kind}&root={root}&dir={dir}&title={title}"
    const val PLAYER = "player?uri={uri}&name={name}"

    fun browseLocal(root: String, dir: String, title: String) =
        "browse?kind=local&root=${Uri.encode(root)}&dir=${Uri.encode(dir)}&title=${Uri.encode(title)}"

    fun browseLocation(location: StorageLocation) = browseLocal(location.treeUri, "", location.name)

    fun browseServer(serverId: String, url: String, title: String) =
        "browse?kind=server&root=${Uri.encode(serverId)}&dir=${Uri.encode(url)}&title=${Uri.encode(title)}"

    fun browseServer(server: ServerEntry) = browseServer(server.id, server.url, server.name)

    fun series(key: String) = "series?key=${Uri.encode(key)}"

    fun player(uri: String, name: String) = "player?uri=${Uri.encode(uri)}&name=${Uri.encode(name)}"
}
