package com.alveon.plus.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alveon.plus.AppGraph
import com.alveon.plus.ui.components.Glyph
import com.alveon.plus.ui.components.GlyphKind
import com.alveon.plus.ui.components.MediaCard
import com.alveon.plus.ui.components.TvButton
import com.alveon.plus.ui.home.HomeItem
import com.alveon.plus.ui.home.groupLibrary
import com.alveon.plus.ui.theme.Palette
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (HomeItem) -> Unit) {
    val library by AppGraph.library.items.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(250)
        runCatching { fieldFocus.requestFocus() }
        keyboard?.show()
    }

    val (movies, series) = remember(library) { groupLibrary(library) }
    val results = remember(query, movies, series) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList() else (movies + series).filter { it.title.lowercase().contains(q) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvButton("‹  Back", onBack)
            Spacer(Modifier.width(20.dp))
            Row(
                Modifier
                    .weight(1f)
                    .background(Palette.Surface, RoundedCornerShape(50))
                    .border(
                        if (focused) 2.dp else 1.dp,
                        if (focused) Palette.Silver else Palette.SurfaceHigh,
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(GlyphKind.Search, Palette.Muted, Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Palette.Silver, fontSize = 18.sp),
                    cursorBrush = SolidColor(Palette.Silver),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldFocus)
                        .androidxOnFocus { focused = it },
                )
            }
        }

        when {
            query.isBlank() -> Text("Type a title to search your library.", color = Palette.Muted, style = MaterialTheme.typography.bodyLarge)
            results.isEmpty() -> Text("No results for \"${query.trim()}\".", color = Palette.Muted, style = MaterialTheme.typography.bodyLarge)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(112.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(results, key = { it.key }) { item ->
                    MediaCard(
                        title = item.title,
                        imageUrl = item.info?.posterUrl ?: item.info?.backdropUrl,
                        onClick = { onOpen(item) },
                    )
                }
            }
        }
    }
}

private fun Modifier.androidxOnFocus(onChange: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { onChange(it.hasFocus) }
