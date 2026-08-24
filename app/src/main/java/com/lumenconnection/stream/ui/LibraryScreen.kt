package com.lumenconnection.stream.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumenconnection.stream.Graph
import com.lumenconnection.stream.R
import com.lumenconnection.stream.db.MediaItem
import com.lumenconnection.stream.ui.theme.Lumen
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(onOpenMedia: (Long) -> Unit) {
    val c = Lumen.colors
    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<MediaItem?>(null) }
    var deleting by remember { mutableStateOf<MediaItem?>(null) }

    val dao = Graph.db.mediaDao()
    val items by remember(query, favoritesOnly) {
        when {
            favoritesOnly -> dao.observeFavorites()
            query.isBlank() -> dao.observeAll()
            else -> dao.search(query)
        }
    }.collectAsState(initial = emptyList())

    val allTags = remember(items) {
        items.flatMap { it.tags.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }
            .distinct().sorted()
    }
    val shown = remember(items, tagFilter) {
        val t = tagFilter
        if (t == null) items
        else items.filter { item ->
            item.tags.split(",").any { it.trim().equals(t, ignoreCase = true) }
        }
    }

    editing?.let { item ->
        TagsDialog(item = item, onDismiss = { editing = null })
    }
    deleting?.let { item ->
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = c.bgCard,
            titleContentColor = c.text,
            textContentColor = c.textMuted,
            shape = RoundedCornerShape(12.dp),
            title = { Text(stringResource(R.string.delete_media_title), fontSize = 17.sp) },
            text = { Text(stringResource(R.string.delete_media_body), fontSize = 13.5.sp) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { Graph.db.mediaDao().delete(item) }
                    deleting = null
                }) { Text(stringResource(R.string.action_delete), color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel), color = c.textMuted)
                }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        PageHeader(
            title = stringResource(R.string.library_title),
            subtitle = stringResource(R.string.library_subtitle),
        )
        Spacer(Modifier.height(16.dp))

        LumenTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.library_search),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatChip(
                label = stringResource(R.string.all_media),
                selected = !favoritesOnly && tagFilter == null,
                onClick = { favoritesOnly = false; tagFilter = null },
            )
            FormatChip(
                label = stringResource(R.string.favorites),
                selected = favoritesOnly,
                onClick = { favoritesOnly = true; tagFilter = null },
            )
            allTags.forEach { tag ->
                FormatChip(
                    label = "#$tag",
                    selected = tagFilter == tag,
                    onClick = { tagFilter = if (tagFilter == tag) null else tag },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (shown.isEmpty()) {
            Text(stringResource(R.string.library_empty), color = c.textMuted, fontSize = 14.sp)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 158.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(shown, key = { it.id }) { item ->
                    MediaCard(
                        item = item,
                        onClick = { onOpenMedia(item.id) },
                        onEditTags = { editing = item },
                        onDelete = { deleting = item },
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Lumen.colors
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(Lumen.dimens.cardRounding.dp),
        colors = CardDefaults.cardColors(containerColor = c.bgCard, contentColor = c.text),
        border = BorderStroke(1.dp, c.border),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(c.bgInput),
        ) {
            if (item.kind == "audio" && item.thumbnailUrl == null) {
                Text(
                    "🎵",
                    fontSize = 30.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                AsyncImage(
                    model = item.thumbnailUrl ?: item.contentUri,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (item.favorite) {
                Text(
                    "★",
                    color = c.accent,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }

        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                item.title,
                color = c.text,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.uploader?.let {
                Text(it, color = c.textFaint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (item.tags.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    item.tags.split(",").filter { it.isNotBlank() }.joinToString(" ") { "#${it.trim()}" },
                    color = c.accent,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconAction(if (item.favorite) "★" else "☆", if (item.favorite) c.accent else c.textMuted) {
                    scope.launch { Graph.db.mediaDao().update(item.copy(favorite = !item.favorite)) }
                }
                IconAction("🏷", c.textMuted, onClick = onEditTags)
                Spacer(Modifier.width(1.dp))
                IconAction("🗑", c.textMuted, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun IconAction(glyph: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text(glyph, color = tint, fontSize = 15.sp)
    }
}

/** Edição de tags (item do backlog 1.x): lista separada por vírgulas. */
@Composable
private fun TagsDialog(item: MediaItem, onDismiss: () -> Unit) {
    val c = Lumen.colors
    val scope = rememberCoroutineScope()
    var text by remember(item.id) { mutableStateOf(item.tags) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgCard,
        titleContentColor = c.text,
        shape = RoundedCornerShape(12.dp),
        title = { Text(stringResource(R.string.tags_edit), fontSize = 17.sp) },
        text = {
            LumenTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = stringResource(R.string.tags_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val normalized = text.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(", ")
                scope.launch { Graph.db.mediaDao().update(item.copy(tags = normalized)) }
                onDismiss()
            }) { Text(stringResource(R.string.action_save), color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = c.textMuted)
            }
        },
    )
}
