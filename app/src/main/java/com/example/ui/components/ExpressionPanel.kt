package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.CustomStickerEntity
import com.example.data.network.GifItem
import com.example.data.network.GifProvider
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraSand
import com.example.ui.theme.ClestraTextSoft
import com.example.ui.theme.ClestraWarmBeige

@Composable
fun ExpressionPanel(
    stickers: List<CustomStickerEntity>,
    gifItems: List<GifItem>,
    selectedGifProvider: GifProvider,
    gifQuery: String,
    isLoadingGifs: Boolean,
    gifNotice: String?,
    onSelectEmoji: (String) -> Unit,
    onBackspaceEmoji: () -> Unit,
    onSendSticker: (CustomStickerEntity) -> Unit,
    onCreateCustomSticker: (label: String, uri: Uri, styleTag: String) -> Unit,
    onDeleteSticker: (String) -> Unit,
    onSelectGifProvider: (GifProvider) -> Unit,
    onGifQueryChange: (String) -> Unit,
    onSendGif: (GifItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Emojis, 1 = Stickers, 2 = GIFs
    var emojiSearch by remember { mutableStateOf("") }
    var selectedEmojiCategory by remember { mutableStateOf("All") }
    var stickerSearch by remember { mutableStateOf("") }
    var stickerFilter by remember { mutableStateOf("All") } // All, My Stickers, Clestra Pack
    var pendingStickerUri by remember { mutableStateOf<Uri?>(null) }

    val stickerImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingStickerUri = uri
        }
    }

    Surface(
        color = ClestraCream,
        modifier = modifier
            .fillMaxWidth()
            .height(328.dp)
            .testTag("expression_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalDivider(color = ClestraSand.copy(alpha = 0.55f))

            // Top Tab Switcher: Separate Emojis | Stickers | GIFs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClestraCream)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressionTabButton(
                    selected = activeTab == 0,
                    icon = Icons.Default.EmojiEmotions,
                    label = "Emojis",
                    onClick = { activeTab = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_emojis")
                )
                ExpressionTabButton(
                    selected = activeTab == 1,
                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                    label = "Stickers",
                    onClick = { activeTab = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_stickers")
                )
                ExpressionTabButton(
                    selected = activeTab == 2,
                    icon = Icons.Default.GifBox,
                    label = "GIFs",
                    onClick = { activeTab = 2 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_gifs")
                )
            }

            // Tab-specific Search Bar
            val activeSearchValue = when (activeTab) {
                0 -> emojiSearch
                1 -> stickerSearch
                else -> gifQuery
            }
            val searchPlaceholder = when (activeTab) {
                0 -> "Search emojis (e.g. heart, fire, laugh)..."
                1 -> "Search custom & Clestra stickers..."
                else -> "Search GIPHY, Reactions & AnimeGIFs..."
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .neumorphicPressed(cornerRadius = 14.dp, containerColor = ClestraNeuWell)
                ) {
                    TextField(
                        value = activeSearchValue,
                        onValueChange = { next ->
                            when (activeTab) {
                                0 -> emojiSearch = next
                                1 -> stickerSearch = next
                                else -> onGifQueryChange(next)
                            }
                        },
                        placeholder = {
                            Text(
                                text = searchPlaceholder,
                                fontSize = 13.sp,
                                color = ClestraTextSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = ClestraTextSoft,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (activeSearchValue.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        when (activeTab) {
                                            0 -> emojiSearch = ""
                                            1 -> stickerSearch = ""
                                            else -> onGifQueryChange("")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = ClestraTextSoft,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ClestraCharcoal,
                            unfocusedTextColor = ClestraCharcoal,
                            cursorColor = ClestraCopper
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("expression_search_input")
                    )
                }

                if (activeTab == 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .neumorphicRaised(cornerRadius = 12.dp, elevation = 3.dp, containerColor = ClestraNeuSurface)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBackspaceEmoji() }
                            .testTag("emoji_backspace_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = ClestraCharcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Tab Body
            when (activeTab) {
                0 -> SeparateEmojisArea(
                    searchQuery = emojiSearch,
                    selectedCategory = selectedEmojiCategory,
                    onSelectCategory = { selectedEmojiCategory = it },
                    onSelectEmoji = onSelectEmoji
                )

                1 -> StickersArea(
                    stickers = stickers,
                    searchQuery = stickerSearch,
                    selectedFilter = stickerFilter,
                    onSelectFilter = { stickerFilter = it },
                    onLaunchCreateSticker = {
                        stickerImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSendSticker = onSendSticker,
                    onDeleteSticker = onDeleteSticker
                )

                else -> GifsProviderArea(
                    gifItems = gifItems,
                    selectedProvider = selectedGifProvider,
                    isLoading = isLoadingGifs,
                    notice = gifNotice,
                    onSelectProvider = onSelectGifProvider,
                    onSendGif = onSendGif
                )
            }
        }
    }

    if (pendingStickerUri != null) {
        CreateCustomStickerDialog(
            imageUri = pendingStickerUri!!,
            onDismiss = { pendingStickerUri = null },
            onConfirm = { label, styleTag ->
                onCreateCustomSticker(label, pendingStickerUri!!, styleTag)
                pendingStickerUri = null
                stickerFilter = "My Stickers"
            }
        )
    }
}

@Composable
private fun ExpressionTabButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val depthModifier = if (selected) {
        Modifier.neumorphicPressed(cornerRadius = 12.dp, containerColor = ClestraBubbleSent.copy(alpha = 0.65f))
    } else {
        Modifier.neumorphicRaised(cornerRadius = 12.dp, elevation = 3.dp, containerColor = ClestraNeuSurface)
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .then(depthModifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) ClestraCopperDark else ClestraTextSoft,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) ClestraCharcoal else ClestraTextSoft,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SeparateEmojisArea(
    searchQuery: String,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onSelectEmoji: (String) -> Unit
) {
    val filtered = remember(searchQuery, selectedCategory) {
        EmojiCatalog.allEmojis.filter { entry ->
            val matchesCat = selectedCategory == "All" || entry.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                entry.keywords.contains(searchQuery.trim(), ignoreCase = true) ||
                entry.symbol.contains(searchQuery.trim())
            matchesCat && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EmojiCatalog.categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClestraCopper,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No emojis match \"$searchQuery\"", color = ClestraTextSoft, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 46.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("emoji_grid")
            ) {
                items(filtered, key = { it.symbol }) { entry ->
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectEmoji(entry.symbol) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = entry.symbol, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StickersArea(
    stickers: List<CustomStickerEntity>,
    searchQuery: String,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onLaunchCreateSticker: () -> Unit,
    onSendSticker: (CustomStickerEntity) -> Unit,
    onDeleteSticker: (String) -> Unit
) {
    val filtered = remember(stickers, searchQuery, selectedFilter) {
        stickers.filter { sticker ->
            val matchesFilter = when (selectedFilter) {
                "My Stickers" -> sticker.isCustom
                "Clestra Pack" -> !sticker.isCustom
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                sticker.label.contains(searchQuery.trim(), ignoreCase = true) ||
                sticker.packName.contains(searchQuery.trim(), ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ClestraCopper,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onLaunchCreateSticker() }
                    .testTag("create_custom_sticker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Custom Sticker",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Create Sticker",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            listOf("All", "My Stickers", "Clestra Pack").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClestraBubbleSent,
                        selectedLabelColor = ClestraCharcoal
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedFilter == "My Stickers") {
                        "No custom stickers yet. Tap + Create Sticker to turn any photo into a sticker!"
                    } else {
                        "No matching stickers found."
                    },
                    color = ClestraTextSoft,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 84.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("stickers_grid")
            ) {
                items(filtered, key = { it.id }) { sticker ->
                    Box(
                        modifier = Modifier
                            .height(84.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { onSendSticker(sticker) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StickerVisual(
                            uri = sticker.uri,
                            label = sticker.label,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (sticker.isCustom) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(ClestraCharcoal.copy(alpha = 0.75f))
                                    .clickable { onDeleteSticker(sticker.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete sticker",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GifsProviderArea(
    gifItems: List<GifItem>,
    selectedProvider: GifProvider,
    isLoading: Boolean,
    notice: String?,
    onSelectProvider: (GifProvider) -> Unit,
    onSendGif: (GifItem) -> Unit
) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) { CoilGifSupport.getImageLoader(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GifProvider.entries.forEach { provider ->
                FilterChip(
                    selected = selectedProvider == provider,
                    onClick = { onSelectProvider(provider) },
                    label = { Text(provider.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClestraCopper,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("gif_provider_${provider.name.lowercase()}")
                )
            }
        }

        if (!notice.isNullOrBlank()) {
            Text(
                text = notice,
                color = ClestraTextSoft,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = ClestraCopper,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            gifItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search for GIFs across GIPHY, Reactions, and AnimeGIFs.",
                        color = ClestraTextSoft,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gifs_grid")
                ) {
                    items(gifItems, key = { it.id }) { gif ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(106.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ClestraWarmBeige)
                                .clickable { onSendGif(gif) }
                        ) {
                            AsyncImage(
                                model = gif.previewUrl,
                                imageLoader = gifImageLoader,
                                contentDescription = gif.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ClestraCharcoal.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = gif.provider.badge,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerVisual(
    uri: String?,
    label: String,
    modifier: Modifier = Modifier
) {
    val builtIn = remember(uri) { StickerStorageUtils.findBuiltInMeta(uri) }
    val context = LocalContext.current
    val gifImageLoader = remember(context) { CoilGifSupport.getImageLoader(context) }

    if (builtIn != null) {
        val bg = Color(builtIn.bgHex)
        val isDarkBg = builtIn.bgHex == 0xFF2C2018 || builtIn.bgHex == 0xFFC4763A
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bg,
            border = BorderStroke(2.dp, Color.White),
            shadowElevation = 2.dp,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = builtIn.emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkBg) Color.White else ClestraCharcoal
                ) {
                    Text(
                        text = builtIn.caption,
                        color = if (isDarkBg) ClestraCharcoal else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    } else if (!uri.isNullOrBlank()) {
        AsyncImage(
            model = uri,
            imageLoader = gifImageLoader,
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

@Composable
private fun CreateCustomStickerDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (label: String, styleTag: String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("OUTLINE") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ClestraCream),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Custom Sticker",
                    color = ClestraCharcoal,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                val borderMod = when (selectedStyle) {
                    "OUTLINE" -> Modifier.border(4.dp, Color.White, RoundedCornerShape(24.dp))
                    "BADGE" -> Modifier
                        .border(4.dp, ClestraCopper, RoundedCornerShape(24.dp))
                        .padding(3.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                    else -> Modifier
                }

                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .then(borderMod)
                        .clip(RoundedCornerShape(22.dp))
                        .background(ClestraWarmBeige)
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Custom Sticker Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "OUTLINE" to "White Rim",
                        "BADGE" to "Copper Rim",
                        "CLEAN" to "Clean"
                    ).forEach { (tag, title) ->
                        FilterChip(
                            selected = selectedStyle == tag,
                            onClick = { selectedStyle = tag },
                            label = { Text(title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClestraCopper,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ClestraPillInput(
                    value = label,
                    onValueChange = { label = it },
                    hint = "Sticker name / search tag (e.g. Coffee)",
                    containerColor = Color.White,
                    modifier = Modifier.testTag("custom_sticker_label_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ClestraTextSoft)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(label, selectedStyle) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClestraCopper,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("save_custom_sticker_button")
                    ) {
                        Text("Save Sticker", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
