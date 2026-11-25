package com.evora.technologies.office.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem

private val PreviewSampleDoc = DocumentItem(
    id = "1",
    uriString = "file:///demo/Contract_Signed_v2.pdf",
    displayName = "Contract_Signed_v2.pdf",
    extension = "pdf",
    format = DocFormat.PDF,
    sizeBytes = 1_887_436L,
    modifiedAtEpochMs = 1795600000000L,
    parentFolderName = "Documents",
    isFavorite = true,
    lastPageIndex = 2,
    lastTotalPages = 12,
    hasPendingDraft = true,
)

@Preview(name = "Compact Light", widthDp = 390, heightDp = 640, showBackground = true)
@Preview(
    name = "Compact Dark 2x Font",
    widthDp = 390,
    heightDp = 640,
    fontScale = 2.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun DesignSystemCompactPreview() {
    EvoraTheme {
        Scaffold(
            bottomBar = {
                MainBottomBar(
                    selectedTab = MainTab.HOME,
                    onTabSelected = {},
                    onScanClick = {},
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatFilterChip(label = "All", selected = true, onClick = {})
                    FormatFilterChip(label = "PDF", selected = false, onClick = {})
                }
                DocCard(
                    document = PreviewSampleDoc,
                    onClick = {},
                    onToggleFavorite = {},
                    onMoreClick = {},
                )
                ShimmerDocListSkeleton(itemCount = 2)
            }
        }
    }
}

@Preview(name = "Expanded Tablet", widthDp = 880, heightDp = 520, showBackground = true)
@Composable
private fun DesignSystemExpandedPreview() {
    EvoraTheme {
        Row {
            MainNavigationRail(
                selectedTab = MainTab.HOME,
                onTabSelected = {},
                onScanClick = {},
            )
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DocCard(
                    document = PreviewSampleDoc,
                    onClick = {},
                    onToggleFavorite = {},
                    onMoreClick = {},
                )
                EmptyStateView(
                    title = "No files found",
                    description = "Scan a document or pick a folder via SAF.",
                    primaryActionLabel = "Open Folder Picker",
                    onPrimaryAction = {},
                )
            }
        }
    }
}
