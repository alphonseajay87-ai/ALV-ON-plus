package com.alveon.plus.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alveon.plus.AppGraph
import com.alveon.plus.data.MediaEntry
import com.alveon.plus.ui.browser.BrowserScreen
import com.alveon.plus.ui.home.HomeScreen
import com.alveon.plus.ui.home.openHomeItem
import com.alveon.plus.ui.player.PlayerScreen
import com.alveon.plus.ui.search.SearchScreen
import com.alveon.plus.ui.series.SeriesScreen
import com.alveon.plus.ui.server.AddServerScreen
import com.alveon.plus.ui.settings.SettingsScreen
import com.alveon.plus.ui.splash.SplashScreen
import com.alveon.plus.ui.welcome.WelcomeScreen

private fun stringArg(name: String) = navArgument(name) {
    type = NavType.StringType
    defaultValue = ""
}

private fun NavHostController.playEntry(entry: MediaEntry) {
    navigate(Routes.player(entry.uri, entry.name))
}

/**
 * [externalUri] is a video another app asked us to open ("Open with ALVéON+").
 */
@Composable
fun AlveonNavHost(externalUri: String?, onExternalHandled: () -> Unit) {
    val nav = rememberNavController()

    fun playExternal(uri: String) {
        val name = Uri.parse(uri).lastPathSegment?.substringAfterLast('/') ?: "Video"
        AppGraph.queue = emptyList()
        nav.navigate(Routes.player(uri, name))
        onExternalHandled()
    }

    // Intent arriving while the app is already running
    LaunchedEffect(externalUri) {
        val route = nav.currentDestination?.route.orEmpty()
        if (externalUri != null && route.isNotEmpty() && !route.startsWith("splash") && !route.startsWith("welcome")) {
            playExternal(externalUri)
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(220)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(220)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen {
                val next = if (AppGraph.store.onboarded) Routes.HOME else Routes.WELCOME
                nav.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
                if (next == Routes.HOME) externalUri?.let { playExternal(it) }
            }
        }

        composable(Routes.WELCOME) {
            WelcomeScreen {
                AppGraph.store.onboarded = true
                nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                externalUri?.let { playExternal(it) }
            }
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpen = { item ->
                    openHomeItem(item, onPlay = { nav.playEntry(it) }, onSeries = { nav.navigate(Routes.series(it.key)) })
                },
                onEpisodes = { nav.navigate(Routes.series(it.key)) },
                onSearch = { nav.navigate(Routes.SEARCH) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { nav.popBackStack() },
                onOpen = { item ->
                    openHomeItem(item, onPlay = { nav.playEntry(it) }, onSeries = { nav.navigate(Routes.series(it.key)) })
                },
            )
        }

        composable(Routes.SERIES, arguments = listOf(stringArg("key"))) { entry ->
            SeriesScreen(
                key = entry.arguments?.getString("key").orEmpty(),
                onBack = { nav.popBackStack() },
                onPlay = { ep ->
                    AppGraph.queue = AppGraph.library.queueFor(ep.entry)
                    nav.playEntry(ep.entry)
                },
            )
        }

        composable(
            Routes.BROWSE,
            arguments = listOf(stringArg("kind"), stringArg("root"), stringArg("dir"), stringArg("title")),
        ) { entry ->
            val args = entry.arguments
            val kind = args?.getString("kind").orEmpty()
            val root = args?.getString("root").orEmpty()
            BrowserScreen(
                kind = kind,
                root = root,
                dir = args?.getString("dir").orEmpty(),
                title = args?.getString("title").orEmpty(),
                onBack = { nav.popBackStack() },
                onOpenDir = { dirEntry ->
                    nav.navigate(
                        if (kind == "local") Routes.browseLocal(root, dirEntry.uri, dirEntry.name)
                        else Routes.browseServer(root, dirEntry.uri, dirEntry.name)
                    )
                },
                onPlay = { nav.playEntry(it) },
            )
        }

        composable(Routes.ADD_SERVER) {
            AddServerScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onAddServer = { nav.navigate(Routes.ADD_SERVER) },
                onBrowseFolder = { nav.navigate(Routes.browseLocation(it)) },
                onBrowseServer = { nav.navigate(Routes.browseServer(it)) },
            )
        }

        composable(Routes.PLAYER, arguments = listOf(stringArg("uri"), stringArg("name"))) { entry ->
            PlayerScreen(
                uri = entry.arguments?.getString("uri").orEmpty(),
                name = entry.arguments?.getString("name").orEmpty(),
                onExit = { nav.popBackStack() },
                onPlayOther = { next ->
                    nav.navigate(Routes.player(next.uri, next.name)) {
                        popUpTo(Routes.PLAYER) { inclusive = true }
                    }
                },
            )
        }
    }
}
