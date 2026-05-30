package dev.jmx.client.ui.screens

import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jmx.client.data.models.SearchTagFilter
import dev.jmx.client.data.models.SearchTagFilterMode
import dev.jmx.client.store.HistorySearchManager
import dev.jmx.client.store.JmxDiagnostics
import dev.jmx.client.store.SearchTagManager
import dev.jmx.client.ui.components.AlbumSearchHistoryTag
import dev.jmx.client.ui.glass.LocalJmxGlassPalette
import dev.jmx.client.ui.razor.AppleIconButton
import dev.jmx.client.ui.razor.RazorIcon
import dev.jmx.client.ui.razor.RazorGlassButton
import dev.jmx.client.ui.razor.RazorChip
import dev.jmx.client.ui.razor.RazorText
import dev.jmx.client.ui.viewModel.AlbumViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun AlbumSearchScreen(
    albumViewModel: AlbumViewModel = koinActivityViewModel(),
    historySearchManager: HistorySearchManager = getKoin().get(),
    searchTagManager: SearchTagManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val palette = LocalJmxGlassPalette.current
    val focusRequester = remember { FocusRequester() }
    var searchText by remember { mutableStateOf("") }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagFilter by remember { mutableStateOf(SearchTagFilter()) }
    val historySearchState by historySearchManager.historySearchState.collectAsState()
    val savedTags by searchTagManager.searchTagState.collectAsState()

    fun onSearch(text: String) {
        val value = text.trim()
        if (value.isBlank()) return
        JmxDiagnostics.userAction(
            screen = "AlbumSearch",
            action = "submit_search",
            target = "search_field",
            metadata = mapOf(
                "input_length" to value.length,
                "from_history" to (value in historySearchState),
                "tag_filter_mode" to tagFilter.mode.name,
                "tag_filter_count" to tagFilter.tags.size
            )
        )
        tagFilter.tags.forEach(searchTagManager::addTag)
        historySearchManager.addItem(value)
        albumViewModel.changeSearchTagFilter(tagFilter)
        mainNavController.navigate("albumSearchResult/${Uri.encode(value)}") {
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.page)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppleIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                onClick = { mainNavController.popBackStack() }
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.contentSurface.copy(alpha = 0.76f))
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                RazorIcon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = palette.tertiaryText
                )
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = palette.primaryText,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { onSearch(searchText) }
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    if (searchText.isEmpty()) {
                        RazorText(
                            text = "搜索漫画、作者或标签",
                            style = TextStyle(
                                color = palette.tertiaryText,
                                fontSize = 16.sp,
                                lineHeight = 21.sp
                            )
                        )
                    }
                }
                if (searchText.isNotEmpty()) {
                    AppleIconButton(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清空",
                        modifier = Modifier.size(28.dp),
                        onClick = {
                            searchText = ""
                        },
                        tint = palette.secondaryText
                    )
                }
            }
            AppleIconButton(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = "标签过滤",
                selected = tagFilter.enabled,
                onClick = { showTagDialog = true }
            )
            AppleIconButton(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                selected = true,
                onClick = { onSearch(searchText) }
            )
        }
        if (tagFilter.enabled) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prefix = if (tagFilter.mode == SearchTagFilterMode.INCLUDE) "必须包含" else "排除"
                tagFilter.tags.forEach { tag ->
                    RazorChip(
                        label = "$prefix $tag",
                        selected = true,
                        onClick = { showTagDialog = true }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RazorText(
                text = "搜索历史",
                style = TextStyle(
                    color = palette.primaryText,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            )
            if (historySearchState.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(palette.contentSurface.copy(alpha = 0.72f))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .noRippleClickable { historySearchManager.clear() }
                ) {
                    RazorText(
                        text = "清空",
                        modifier = Modifier.align(Alignment.Center),
                        style = TextStyle(
                            color = palette.accent,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (historySearchState.isNotEmpty()) {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                historySearchState.forEach {
                    key(it) {
                        AlbumSearchHistoryTag(
                            label = it,
                            onClick = { onSearch(it) }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RazorIcon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = palette.tertiaryText
                    )
                    RazorText(
                        text = "暂无搜索历史",
                        style = TextStyle(
                            color = palette.tertiaryText,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }

    if (showTagDialog) {
        SearchTagFilterDialog(
            initialFilter = tagFilter,
            savedTags = savedTags,
            onSaveTag = searchTagManager::addTag,
            onRemoveTag = searchTagManager::removeTag,
            onApply = {
                tagFilter = it
                showTagDialog = false
            },
            onDismissRequest = { showTagDialog = false }
        )
    }
}

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = onClick
)

@Composable
private fun SearchTagFilterDialog(
    initialFilter: SearchTagFilter,
    savedTags: List<String>,
    onSaveTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onApply: (SearchTagFilter) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val palette = LocalJmxGlassPalette.current
    var mode by remember(initialFilter) { mutableStateOf(initialFilter.mode) }
    var input by remember { mutableStateOf("") }
    var selectedTags by remember(initialFilter) { mutableStateOf(initialFilter.tags) }

    fun normalizeTag(tag: String): String? {
        return tag.trim()
            .trim(',', '，', '、', '#', ' ')
            .takeIf { it.isNotBlank() }
    }

    fun addInputTags() {
        val tags = input
            .split(',', '，', '、', ' ', '\n', '\t')
            .mapNotNull(::normalizeTag)
        if (tags.isEmpty()) return
        val merged = (selectedTags + tags).distinctBy { it.lowercase() }
        selectedTags = merged
        tags.forEach(onSaveTag)
        input = ""
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.30f))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(palette.contentSurface.copy(alpha = 0.96f))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = {}
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RazorText(
                        text = "标签增强",
                        style = TextStyle(
                            color = palette.primaryText,
                            fontSize = 24.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.4).sp
                        )
                    )
                    RazorText(
                        text = "搜索主体仍来自搜索栏，标签用于进一步筛选结果。",
                        style = TextStyle(
                            color = palette.secondaryText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeButton(
                        label = "必须包含",
                        selected = mode == SearchTagFilterMode.INCLUDE,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = SearchTagFilterMode.INCLUDE }
                    )
                    ModeButton(
                        label = "结果排除",
                        selected = mode == SearchTagFilterMode.EXCLUDE,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = SearchTagFilterMode.EXCLUDE }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.page.copy(alpha = 0.58f))
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = palette.primaryText,
                                fontSize = 16.sp,
                                lineHeight = 21.sp
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { addInputTags() }
                            )
                        )
                        if (input.isBlank()) {
                            RazorText(
                                text = "输入标签，用逗号分隔",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    color = palette.tertiaryText,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                    AppleIconButton(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加标签",
                        modifier = Modifier.size(34.dp),
                        selected = true,
                        onClick = { addInputTags() }
                    )
                }
                TagSection(
                    title = "已选择",
                    emptyText = "还没有选择标签",
                    tags = selectedTags,
                    selectedTags = selectedTags,
                    onTagClick = { tag ->
                        selectedTags = selectedTags.filterNot { it.equals(tag, ignoreCase = true) }
                    },
                    trailing = {
                        RazorIcon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = palette.secondaryText
                        )
                    }
                )
                TagSection(
                    title = "已保存",
                    emptyText = "输入标签后会自动保存，便于下次使用",
                    tags = savedTags,
                    selectedTags = selectedTags,
                    onTagClick = { tag ->
                        selectedTags = (selectedTags + tag).distinctBy { it.lowercase() }
                    },
                    onDeleteTag = onRemoveTag
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RazorGlassButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onApply(SearchTagFilter())
                        }
                    ) {
                        RazorText(
                            text = "清除",
                            style = TextStyle(
                                color = palette.secondaryText,
                                fontSize = 15.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    RazorGlassButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val normalized = selectedTags
                                .mapNotNull(::normalizeTag)
                                .distinctBy { it.lowercase() }
                            normalized.forEach(onSaveTag)
                            onApply(SearchTagFilter(mode = mode, tags = normalized))
                        }
                    ) {
                        RazorText(
                            text = "应用",
                            style = TextStyle(
                                color = palette.primaryText,
                                fontSize = 15.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalJmxGlassPalette.current
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) {
                    palette.accent.copy(alpha = 0.92f)
                } else {
                    palette.page.copy(alpha = 0.58f)
                }
            )
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        RazorText(
            text = label,
            style = TextStyle(
                color = if (selected) palette.page else palette.secondaryText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun TagSection(
    title: String,
    emptyText: String,
    tags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit,
    onDeleteTag: ((String) -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalJmxGlassPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RazorText(
            text = title,
            style = TextStyle(
                color = palette.primaryText,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (tags.isEmpty()) {
            RazorText(
                text = emptyText,
                style = TextStyle(
                    color = palette.tertiaryText,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val selected = selectedTags.any { it.equals(tag, ignoreCase = true) }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (selected) {
                                    palette.accent.copy(alpha = 0.14f)
                                } else {
                                    palette.page.copy(alpha = 0.58f)
                                }
                            )
                            .clickable(
                                interactionSource = MutableInteractionSource(),
                                indication = null,
                                onClick = { onTagClick(tag) }
                            )
                            .padding(start = 12.dp, end = if (onDeleteTag == null) 12.dp else 4.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RazorText(
                            text = tag,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                color = if (selected) palette.accent else palette.secondaryText,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        trailing?.invoke()
                        if (onDeleteTag != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable(
                                        interactionSource = MutableInteractionSource(),
                                        indication = null,
                                        onClick = { onDeleteTag(tag) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                RazorIcon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除标签",
                                    modifier = Modifier.size(14.dp),
                                    tint = palette.tertiaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
