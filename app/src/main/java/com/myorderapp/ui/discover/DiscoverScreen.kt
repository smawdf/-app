package com.myorderapp.ui.discover

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyMainTopBar
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyMotionVisibility
import com.myorderapp.ui.components.CozyPage
import com.myorderapp.ui.components.CozyPill
import com.myorderapp.ui.theme.OnPrimaryContainer
import com.myorderapp.ui.theme.Outline
import com.myorderapp.ui.theme.Primary
import com.myorderapp.ui.theme.Secondary
import com.myorderapp.ui.theme.SecondaryContainer
import org.koin.androidx.compose.koinViewModel

private val DiscoverPrimary = Primary
private val DiscoverCard = SecondaryContainer
private val DiscoverCardBorder = Secondary
private val DiscoverInput = Color(0xFFE7E2DC)
private val DiscoverSurface = Color(0xFFFEF8F2)
private val DiscoverCreamCard = Color(0xFFFFFCF8)
private const val DiscoverSearchPlaceholder = "搜索菜品、做法、食材"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var detailItem by remember { mutableStateOf<DiscoverDishSearchItem?>(null) }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    detailItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { detailItem = null }) {
            DiscoverDishDetailSheet(
                item = item,
                onAddToMenu = {
                    detailItem = null
                    viewModel.addToMenu(item)
                },
                onClose = { detailItem = null }
            )
        }
    }

    CozyPage(decorative = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 172.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                userScrollEnabled = uiState.results.size > 2
            ) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            text = "发现 - 探索新菜谱",
                            color = DiscoverPrimary,
                            fontSize = 29.sp,
                            lineHeight = 37.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        )
                        Text(
                            text = "搜一搜，给你们的小饭桌加点新菜",
                            color = CozyMuted,
                            fontSize = 17.sp,
                            lineHeight = 25.sp
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StitchDiscoverSearchField(
                            value = uiState.query,
                            onValueChange = viewModel::onQueryChanged,
                            placeholder = if (uiState.query.isBlank()) "想吃点什么？例如：糖醋排骨..." else DiscoverSearchPlaceholder,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (uiState.recommendations.isNotEmpty()) {
                    item {
                        DiscoverRecommendationRow(
                            recommendations = uiState.recommendations,
                            onClick = { detailItem = it.item },
                            onRecoverImage = viewModel::recoverRecommendationImageFor
                        )
                    }
                }

                uiState.errorMessage?.let {
                    item {
                        Text(
                            text = "部分网络结果暂不可用，先展示可用结果",
                            color = CozyMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                when {
                    uiState.query.isBlank() -> {
                        item {
                            DiscoverSearchPrompt()
                        }
                    }

                    uiState.isSearching -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = DiscoverPrimary,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "正在搜索菜品...",
                                    color = CozyMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    uiState.results.isEmpty() -> {
                        item {
                            DiscoverEmptyState(query = uiState.query)
                        }
                    }

                    else -> {
                        items(uiState.results, key = { it.id }) { result ->
                            CozyMotionVisibility(delayMillis = (uiState.results.indexOf(result).coerceAtMost(5)) * 28) {
                                DiscoverResultCard(
                                    result = result,
                                    onClick = { detailItem = result },
                                    onAddToMenu = viewModel::addToMenu,
                                    onEnsureImage = viewModel::ensureImageFor,
                                    onRecoverImage = viewModel::recoverImageFor
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 172.dp),
            snackbar = { data ->
                DiscoverToastSnackbar(data = data)
            }
        )
    }
}

@Composable
private fun DiscoverRecommendationRow(
    recommendations: List<DiscoverRecommendationItem>,
    onClick: (DiscoverRecommendationItem) -> Unit,
    onRecoverImage: (DiscoverDishSearchItem) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        recommendations.take(2).forEachIndexed { index, recommendation ->
            CozyMotionVisibility(
                modifier = Modifier.weight(1f),
                delayMillis = index * 40
            ) {
                DiscoverRecommendationCard(
                    recommendation = recommendation,
                    onClick = { onClick(recommendation) },
                    onRecoverImage = onRecoverImage
                )
            }
        }
        if (recommendations.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DiscoverRecommendationCard(
    recommendation: DiscoverRecommendationItem,
    onClick: () -> Unit,
    onRecoverImage: (DiscoverDishSearchItem) -> Unit
) {
    val item = recommendation.item
    LaunchedEffect(item.id, item.imageUrl) {
        if (item.imageUrl.isNullOrBlank()) {
            onRecoverImage(item)
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
            .scale(if (pressed) 0.98f else 1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = DiscoverCreamCard.copy(alpha = 0.96f),
        border = BorderStroke(2.dp, DiscoverCardBorder.copy(alpha = 0.45f)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFEAF0),
                border = BorderStroke(1.dp, DiscoverCardBorder.copy(alpha = 0.34f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DishImageOrPlaceholder(
                        imageUrl = item.imageUrl,
                        contentDescription = item.name,
                        onLoadError = { onRecoverImage(item) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                text = recommendation.title,
                color = DiscoverPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = item.name,
                color = CozyCocoa,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = recommendation.subtitle,
                color = CozyMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RecommendationVideoLinks(query = item.name)
        }
    }
}

@Composable
private fun RecommendationVideoLinks(query: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoSearchChip(
            text = "抖音",
            onClick = { openVideoAppSearch(context, VideoPlatform.Douyin, query) }
        )
        VideoSearchChip(
            text = "哔站",
            onClick = { openVideoAppSearch(context, VideoPlatform.Bilibili, query) }
        )
    }
}

@Composable
private fun DiscoverToastSnackbar(data: SnackbarData) {
    CozyMotionVisibility {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFFFFCF8),
            border = BorderStroke(1.dp, DiscoverCardBorder.copy(alpha = 0.42f)),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFFFD1DC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = DiscoverPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    text = data.visuals.message,
                    color = CozyCocoa,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DiscoverResultCard(
    result: DiscoverDishSearchItem,
    onClick: () -> Unit,
    onAddToMenu: (DiscoverDishSearchItem) -> Unit,
    onEnsureImage: (DiscoverDishSearchItem) -> Unit,
    onRecoverImage: (DiscoverDishSearchItem) -> Unit
) {
    LaunchedEffect(result.id, result.imageUrl) {
        if (result.imageUrl.isNullOrBlank()) {
            onEnsureImage(result)
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.98f else 1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = DiscoverCard,
        border = BorderStroke(2.dp, DiscoverCardBorder),
        tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEAF0),
                    border = BorderStroke(2.dp, DiscoverCardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        DishImageOrPlaceholder(
                            imageUrl = result.imageUrl,
                            contentDescription = result.name,
                            onLoadError = { onRecoverImage(result) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = result.name,
                        color = OnPrimaryContainer,
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiscoverSourceChip(result.sourceLabel.displaySourceName())
                        Text(
                            text = result.subtitle.ifBlank { "暂无描述" },
                            color = CozyMuted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(modifier = Modifier.height(11.dp))
                    SquishyDiscoverButton(
                        text = if (result.isAdded) "已在我的小店" else "加入我的小店",
                        enabled = !result.isAdded,
                        onClick = { onAddToMenu(result) }
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    RecipeVideoLinkIcons(query = result.name)
                }
            }
        }
    }
}

@Composable
private fun StitchDiscoverTopBar() {
    CozyMainTopBar(title = "发现")
    return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(58.dp)
            .background(DiscoverSurface.copy(alpha = 0.84f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "喜欢",
                tint = DiscoverPrimary
            )
        }
        Text(
            text = "发现",
            color = DiscoverPrimary,
            fontSize = 25.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "通知",
                tint = DiscoverPrimary
            )
        }
    }
}

@Composable
private fun StitchDiscoverSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(60.dp),
        shape = RoundedCornerShape(999.dp),
        color = DiscoverInput,
        border = BorderStroke(2.dp, DiscoverCardBorder.copy(alpha = 0.32f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = CozyCocoa,
                    fontSize = 17.sp,
                    lineHeight = 25.sp
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            color = Outline,
                            fontSize = 17.sp,
                            lineHeight = 25.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun DiscoverSourceChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (text == "我的小店") Color(0xFFF4A7B9) else DiscoverPrimary,
        border = if (text == "我的小店") BorderStroke(1.dp, DiscoverPrimary) else null
    ) {
        Text(
            text = text,
            color = if (text == "我的小店") OnPrimaryContainer else Color.White,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun SquishyDiscoverButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .scale(if (pressed) 0.96f else 1f)
            .defaultMinSize(minHeight = 38.dp)
    ) {
        Surface(
            modifier = Modifier.clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
            shape = RoundedCornerShape(999.dp),
            color = if (enabled) DiscoverPrimary else Color(0xFFF0ECE4)
        ) {
            Text(
                text = text,
                color = if (enabled) Color.White else Color(0xFF8B7164),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun DiscoverSearchPrompt() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DiscoverCreamCard.copy(alpha = 0.84f),
        border = BorderStroke(2.dp, DiscoverCardBorder.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFFFE8EE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = DiscoverPrimary, modifier = Modifier.size(32.dp))
            }
            Text(
                text = "搜一搜新菜谱",
                color = CozyCocoa,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "输入菜名、食材或做法，找到合适的菜后加入我的小店。",
                color = CozyMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DiscoverEmptyState(query: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DiscoverCreamCard.copy(alpha = 0.92f),
        border = BorderStroke(2.dp, DiscoverCardBorder.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFE8EE)),
                contentAlignment = Alignment.Center
            ) {
                Text("菜", color = DiscoverPrimary, fontWeight = FontWeight.Black)
            }
            Text(
                text = "没有找到相关菜品",
                color = CozyCocoa,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "换个更准确的菜名试试，或者直接加入我的小店后再编辑",
                color = CozyMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            if (query.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                RecipeVideoLinkIcons(query = query, compact = false)
            }
        }
    }
}

@Composable
private fun RecipeVideoLinkIcons(
    query: String,
    compact: Boolean = true
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (compact) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoSearchChip(
            text = if (compact) "抖音视频" else "去抖音看看",
            onClick = { openVideoAppSearch(context, VideoPlatform.Douyin, query) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        VideoSearchChip(
            text = if (compact) "哔站视频" else "去哔站看看",
            onClick = { openVideoAppSearch(context, VideoPlatform.Bilibili, query) }
        )
    }
}

@Composable
private fun VideoSearchChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFFFFCF8),
        border = BorderStroke(1.dp, DiscoverCardBorder.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = DiscoverPrimary, modifier = Modifier.size(15.dp))
            Text(text, color = DiscoverPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun String.displaySourceName(): String {
    return when (this) {
        "xiachufang" -> "下厨房"
        "bimissing" -> "中文菜谱"
        "bing" -> "网络图片"
        "builtin" -> "下厨房"
        "tian" -> "天行"
        "local" -> "我的小店"
        "menu" -> "我的小店"
        else -> "天行"
    }
}

private enum class VideoPlatform {
    Douyin,
    Bilibili
}

private fun openVideoAppSearch(context: Context, platform: VideoPlatform, query: String) {
    val encodedQuery = Uri.encode(query)
    val searchIntents = when (platform) {
        VideoPlatform.Douyin -> listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("snssdk1128://search?keyword=$encodedQuery"))
                .setPackage("com.ss.android.ugc.aweme"),
            Intent(Intent.ACTION_VIEW, Uri.parse("snssdk1128://search?keyword=$encodedQuery"))
                .setPackage("com.ss.android.ugc.aweme.lite"),
            Intent(Intent.ACTION_VIEW, Uri.parse("aweme://search?keyword=$encodedQuery"))
                .setPackage("com.ss.android.ugc.aweme"),
            Intent(Intent.ACTION_VIEW, Uri.parse("snssdk1128://search?keyword=$encodedQuery"))
        )
        VideoPlatform.Bilibili -> listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("bilibili://search?keyword=$encodedQuery"))
                .setPackage("tv.danmaku.bili"),
            Intent(Intent.ACTION_VIEW, Uri.parse("bilibili://search?keyword=$encodedQuery"))
                .setPackage("com.bilibili.app.in"),
            Intent(Intent.ACTION_VIEW, Uri.parse("bilibili://search?keyword=$encodedQuery"))
        )
    }
    val launchPackages = when (platform) {
        VideoPlatform.Douyin -> listOf("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.lite")
        VideoPlatform.Bilibili -> listOf("tv.danmaku.bili", "com.bilibili.app.in")
    }
    val openedSearch = searchIntents.any { context.tryStartActivity(it) }
    if (!openedSearch) {
        val openedApp = launchPackages
            .mapNotNull { context.packageManager.getLaunchIntentForPackage(it) }
            .any { context.tryStartActivity(it) }
        if (!openedApp) {
            val appName = when (platform) {
                VideoPlatform.Douyin -> "抖音"
                VideoPlatform.Bilibili -> "哔站"
            }
            Toast.makeText(context, "未找到$appName，请先安装后再试", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Context.tryStartActivity(intent: Intent): Boolean {
    return try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Composable
private fun DiscoverDishDetailSheet(
    item: DiscoverDishSearchItem,
    onAddToMenu: () -> Unit,
    onClose: () -> Unit
) {
    val suggestedPrice = "¥%.2f".format(item.price)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFDDE7)),
            contentAlignment = Alignment.Center
        ) {
            DishImageOrPlaceholder(
                imageUrl = item.imageUrl,
                contentDescription = item.name,
                onLoadError = {},
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(item.name, color = CozyCocoa, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(item.subtitle.ifBlank { "暂无描述" }, color = CozyMuted, style = MaterialTheme.typography.bodyMedium)
        CozyPill("建议售价 $suggestedPrice", color = DiscoverPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("关闭") }
            Button(
                onClick = onAddToMenu,
                enabled = !item.isAdded,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DiscoverPrimary, contentColor = Color.White)
            ) {
                Text(if (item.isAdded) "已在我的小店" else "加入我的小店")
            }
        }
    }
}

@Composable
private fun DishImageOrPlaceholder(
    imageUrl: String?,
    contentDescription: String,
    onLoadError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loadFailed by remember(imageUrl) { mutableStateOf(false) }

    if (imageUrl.isNullOrBlank() || loadFailed) {
        Box(
            modifier = modifier
                .background(Color(0xFFFFF6EF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无图片",
                color = DiscoverPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            onError = {
                loadFailed = true
                onLoadError()
            },
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
