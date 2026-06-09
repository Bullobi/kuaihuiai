package comkuaihuiai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import comkuaihuiai.R
import comkuaihuiai.data.TagMatchType
import comkuaihuiai.data.TagSuggestion

@Composable
fun PromptTagTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: @Composable (() -> Unit),
    suggestions: List<TagSuggestion>,
    onSuggestionClick: (TagSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSuggestions: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
    highlightQuery: String? = null,
    maxCollapsedLines: Int = 2,
    minCollapsedLines: Int = 2,
    minExpandedLines: Int = 3,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    var anchorTopPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .onGloballyPositioned { coords ->
                    anchorWidthPx = coords.size.width
                    anchorTopPx = coords.positionInWindow().y
                },
            enabled = enabled,
            label = label,
            maxLines = if (expanded) Int.MAX_VALUE else maxCollapsedLines,
            minLines = if (expanded) minExpandedLines else minCollapsedLines,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            },
        )
    }

    if (showSuggestions && suggestions.isNotEmpty() && anchorWidthPx > 0) {
        val widthDp = with(density) { anchorWidthPx.toDp() }
        val gapDp = 8.dp
        val gapPx = with(density) { gapDp.toPx() }

        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val statusTopPx = WindowInsets.statusBars.getTop(density)
        val navBottomPx = WindowInsets.navigationBars.getBottom(density)
        val bottomInsetPx = maxOf(imeBottomPx, navBottomPx)

        val availableAbovePx = (anchorTopPx - statusTopPx - gapPx).coerceAtLeast(0f)
        val maxHeightDp = with(density) {
            minOf(280.dp, availableAbovePx.toDp())
        }
        Popup(
            popupPositionProvider = remember(statusTopPx, bottomInsetPx) {
                AnchorPositionProvider(
                    safeTopPx = statusTopPx,
                    bottomInsetPx = bottomInsetPx,
                )
            },
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
        ) {
            Card(
                modifier = Modifier.width(widthDp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeightDp),
                ) {
                    items(
                        items = suggestions,
                        key = { it.replacementTag },
                    ) { suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            highlightQuery = highlightQuery,
                            onClick = { onSuggestionClick(suggestion) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: TagSuggestion, highlightQuery: String?, onClick: () -> Unit) {
    val displayPrimary = if (suggestion.matchType == TagMatchType.Embedding) {
        suggestion.primaryText
    } else {
        suggestion.primaryText.replace('_', ' ')
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (suggestion.matchType == TagMatchType.Embedding) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        categoryColor(suggestion.category)
                    },
                    shape = CircleShape,
                ),
        )
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = highlightSubstring(displayPrimary, highlightQuery),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            suggestion.secondaryText?.takeIf { it.isNotBlank() }?.let { secondary ->
                Text(
                    text = highlightSubstring(secondary, highlightQuery),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (suggestion.postCount > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatPostCount(suggestion.postCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MatchTypeBadge(suggestion.matchType)
    }
}

@Composable
private fun MatchTypeBadge(matchType: TagMatchType) {
    val label = when (matchType) {
        TagMatchType.Alias -> "Alias"
        TagMatchType.Correction -> "Fix"
        TagMatchType.Embedding -> "Embed"
        else -> return
    }
    val container = when (matchType) {
        TagMatchType.Correction -> MaterialTheme.colorScheme.errorContainer
        TagMatchType.Embedding -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = when (matchType) {
        TagMatchType.Correction -> MaterialTheme.colorScheme.onErrorContainer
        TagMatchType.Embedding -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Spacer(Modifier.width(8.dp))
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = onContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun categoryColor(category: Int): Color = when (category) {
    1 -> Color(0xFFE53935)  // rating
    3 -> Color(0xFFAB47BC)  // artist
    4 -> Color(0xFF43A047)  // copyright
    5 -> Color(0xFFFB8C00)  // character
    else -> MaterialTheme.colorScheme.outline  // general / unknown
}

private fun formatPostCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 10_000 -> "${n / 1_000}k"
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}

private fun normalizeForHighlight(value: String): String = value.lowercase().replace(' ', '_').replace('-', '_')

private fun highlightSubstring(text: String, query: String?): androidx.compose.ui.text.AnnotatedString {
    if (query.isNullOrBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val normText = normalizeForHighlight(text)
    val normQuery = normalizeForHighlight(query.trim())
    if (normQuery.isEmpty()) return androidx.compose.ui.text.AnnotatedString(text)
    val idx = normText.indexOf(normQuery)
    if (idx < 0) return androidx.compose.ui.text.AnnotatedString(text)
    val end = (idx + normQuery.length).coerceAtMost(text.length)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(idx, end))
        }
        append(text.substring(end))
    }
}

private class AnchorPositionProvider(private val safeTopPx: Int, private val bottomInsetPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = 8
        val visibleBottom = (windowSize.height - bottomInsetPx).coerceAtLeast(0)

        val aboveY = anchorBounds.top - popupContentSize.height - gap
        val belowY = anchorBounds.bottom + gap

        val y = when {
            aboveY >= safeTopPx -> aboveY
            belowY + popupContentSize.height <= visibleBottom -> belowY
            else -> aboveY.coerceAtLeast(safeTopPx)
        }
        val x = anchorBounds.left.coerceIn(
            0,
            (windowSize.width - popupContentSize.width).coerceAtLeast(0),
        )
        return IntOffset(x, y)
    }
}
