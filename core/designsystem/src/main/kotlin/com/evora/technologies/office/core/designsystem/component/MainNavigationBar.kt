package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.R
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens

enum class MainTab(
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOME(Icons.Outlined.Home, R.string.evora_nav_home),
    RECENT(Icons.Outlined.History, R.string.evora_nav_recent),
    STORAGE(Icons.Outlined.FolderOpen, R.string.evora_nav_storage),
    TOOLS(Icons.Outlined.Build, R.string.evora_nav_tools),
}

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        color = extra.surfaceElevated,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(min = 80.dp)
                .padding(horizontal = EvoraSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomTabItem(MainTab.HOME, selectedTab == MainTab.HOME) { onTabSelected(MainTab.HOME) }
            BottomTabItem(MainTab.RECENT, selectedTab == MainTab.RECENT) { onTabSelected(MainTab.RECENT) }

            FloatingActionButton(
                onClick = onScanClick,
                containerColor = extra.brandAccent,
                contentColor = extra.onBrandAccent,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DocumentScanner,
                    contentDescription = stringResource(R.string.evora_nav_scan_cd),
                )
            }

            BottomTabItem(MainTab.STORAGE, selectedTab == MainTab.STORAGE) { onTabSelected(MainTab.STORAGE) }
            BottomTabItem(MainTab.TOOLS, selectedTab == MainTab.TOOLS) { onTabSelected(MainTab.TOOLS) }
        }
    }
}

@Composable
private fun RowScope.BottomTabItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else EvoraThemeTokens.colors.textMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .sizeIn(minWidth = 48.dp, minHeight = 56.dp)
            .clip(EvoraRadii.md)
            .semantics { selected = isSelected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = tint,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MainNavigationRail(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .background(extra.surfaceElevated),
        containerColor = extra.surfaceElevated,
        header = {
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = extra.brandAccent,
                contentColor = extra.onBrandAccent,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DocumentScanner,
                    contentDescription = stringResource(R.string.evora_nav_scan_cd),
                )
            }
        },
    ) {
        MainTab.entries.forEach { tab ->
            val label = stringResource(tab.labelRes)
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = label,
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}
