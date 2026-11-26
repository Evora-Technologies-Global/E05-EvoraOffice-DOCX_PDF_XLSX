package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens

/**
 * Honest pre-action RewardGateSheet per CD-07 / US-110.
 * Shown upfront before a Pro power tool starts rather than locking Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvoraRewardGateSheet(
    featureTitle: String,
    onWatchRewardedPass: () -> Unit,
    onUpgradePro: () -> Unit,
    onDismiss: () -> Unit,
) {
    val extra = EvoraThemeTokens.colors
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EvoraSpacing.xl, vertical = EvoraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
        ) {
            Surface(
                shape = EvoraRadii.full,
                color = extra.proGoldContainer,
            ) {
                Text(
                    text = "PRO UTILITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = extra.onProGoldContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Text(
                text = "Unlock $featureTitle",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Choose how to proceed before processing starts. Standard viewing, editing, and saving remain 100% free.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EvoraPrimaryButton(
                text = "Unlock 24h Pro Pass",
                onClick = onWatchRewardedPass,
            )
            EvoraOutlinedButton(
                text = "Upgrade to Evora Pro",
                onClick = onUpgradePro,
            )
            Spacer(modifier = Modifier.height(EvoraSpacing.lg))
        }
    }
}
