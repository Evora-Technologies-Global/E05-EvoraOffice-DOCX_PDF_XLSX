package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.OfficeKind

/**
 * Reusable S-09 TemplatesSheet displaying bundled offline Word, Excel, and PowerPoint presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvoraTemplatesSheet(
    templates: List<DocTemplatePreset>,
    selectedKind: OfficeKind?,
    isCreating: Boolean,
    onSelectKind: (OfficeKind?) -> Unit,
    onTemplateClicked: (DocTemplatePreset) -> Unit,
    onDismiss: () -> Unit,
) {
    val filtered = if (selectedKind == null) templates else templates.filter { it.kind == selectedKind }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EvoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
        ) {
            Text(
                text = "Create From Offline Template",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Starter documents ready offline with zero server download.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isCreating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
            ) {
                item(key = "all") {
                    FormatFilterChip(
                        label = "All",
                        selected = selectedKind == null,
                        onClick = { onSelectKind(null) },
                    )
                }
                items(OfficeKind.entries, key = { it.name }) { kind ->
                    FormatFilterChip(
                        label = kind.label,
                        selected = selectedKind == kind,
                        onClick = { onSelectKind(kind) },
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = EvoraSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                items(filtered, key = { it.id }) { preset ->
                    val docFormat = when (preset.kind) {
                        OfficeKind.WORD -> DocFormat.WORD
                        OfficeKind.EXCEL -> DocFormat.EXCEL
                        OfficeKind.PPT -> DocFormat.PPT
                    }
                    val palette = EvoraThemeTokens.formats.paletteFor(docFormat)
                    Surface(
                        shape = EvoraRadii.md,
                        color = palette.tintBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 92.dp)
                            .clip(EvoraRadii.md)
                            .clickable(enabled = !isCreating) { onTemplateClicked(preset) },
                    ) {
                        Column(
                            modifier = Modifier.padding(EvoraSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Surface(shape = EvoraRadii.xs, color = palette.headerBg) {
                                    Text(
                                        text = preset.assetFileName.substringAfterLast('.').uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.headerContent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                Text(
                                    text = preset.kind.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.inkText,
                                )
                            }
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.inkText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(EvoraSpacing.lg))
        }
    }
}
