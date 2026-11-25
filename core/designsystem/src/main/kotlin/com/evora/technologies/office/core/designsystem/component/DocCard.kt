package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocumentItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocCard(
    document: DocumentItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
) {
    val extra = EvoraThemeTokens.colors
    val formattedSize = formatByteSize(document.sizeBytes)
    val formattedDate = formatEpochShort(document.modifiedAtEpochMs)
    val pageText = if (document.lastTotalPages > 0) {
        "Page ${document.lastPageIndex + 1}/${document.lastTotalPages}"
    } else {
        null
    }
    val accessibilitySummary = buildString {
        append("${document.format.name} file, ${document.displayName}, $formattedSize, $formattedDate")
        if (pageText != null) append(", $pageText")
    }

    Surface(
        shape = EvoraRadii.md,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = when {
                isSelected -> extra.brandAccent
                extra.isDark -> MaterialTheme.colorScheme.outlineVariant
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(EvoraRadii.md)
            .let { base ->
                if (onLongClick != null) {
                    base.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    base.clickable(onClick = onClick)
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                )
                Spacer(modifier = Modifier.width(EvoraSpacing.xs))
            }
            FormatBadge(
                format = document.format,
                extension = document.extension,
            )
            Spacer(modifier = Modifier.width(EvoraSpacing.md))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = accessibilitySummary
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "$formattedSize · $formattedDate",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        color = extra.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pageText != null) {
                        Surface(
                            shape = EvoraRadii.xs,
                            color = extra.surfaceSubtle,
                        ) {
                            Text(
                                text = pageText,
                                style = EvoraTabularMonoStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (document.hasPendingDraft) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(extra.warning),
                        )
                        Text(
                            text = "Draft auto-saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = extra.warning,
                        )
                    }
                }
            }
            if (!isSelectionMode) {
                EvoraIconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (document.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (document.isFavorite) {
                            "Remove ${document.displayName} from favorites"
                        } else {
                            "Add ${document.displayName} to favorites"
                        },
                        tint = if (document.isFavorite) extra.onProGoldContainer else extra.textMuted,
                    )
                }
                EvoraIconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More actions for ${document.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

fun formatByteSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

private val ShortDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
}

fun formatEpochShort(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    return ShortDateFormatter.get()?.format(Date(epochMs)).orEmpty()
}
