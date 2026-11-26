package com.evora.technologies.office.feature.monetization.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.component.EvoraBrandMark
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.component.EvoraRewardGateSheet
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.BillingOffer
import com.evora.technologies.office.core.model.SubscriptionPlan

@Composable
internal fun PaywallScreen(
    state: PaywallState,
    onIntent: (PaywallIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    val benefits = listOf(
        "Remove all ad slots globally across every tab",
        "Batch Merge > 3 PDFs & High-DPI Compress",
        "Dual-Side ID Card Scan, Batch OCR & Watermark",
        "Unlimited Offline PDF -> DOCX & PKCS#7 Signing",
    )
    val offerByPlan = state.offers.associateBy { it.plan }
    val selectedOffer = offerByPlan[state.selectedPlan] ?: BillingOffer(state.selectedPlan)

    val ctaText = when {
        state.proStatus.isPro -> "Evora Pro Active"
        state.selectedPlan == SubscriptionPlan.LIFETIME -> "Unlock Lifetime Pro"
        state.selectedPlan == SubscriptionPlan.WEEKLY && selectedOffer.hasFreeTrial -> "Start 3-Day Free Trial"
        state.selectedPlan == SubscriptionPlan.YEARLY && selectedOffer.hasFreeTrial -> "Start 7-Day Free Trial"
        else -> "Subscribe to ${state.selectedPlan.title}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EvoraIconButton(onClick = { onIntent(PaywallIntent.SkipClicked) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Skip paywall",
                    )
                }
                Surface(
                    shape = EvoraRadii.full,
                    color = extra.proGoldContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            tint = extra.onProGoldContainer,
                        )
                        Text(
                            text = if (state.proStatus.isPro) "PRO ACTIVE" else "EVORA PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = extra.onProGoldContainer,
                        )
                    }
                }
                TextButton(
                    onClick = { onIntent(PaywallIntent.RestorePurchasesClicked) },
                    enabled = !state.isProcessing,
                ) {
                    Text("Restore")
                }
            }

            Spacer(modifier = Modifier.height(EvoraSpacing.sm))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EvoraBrandMark(size = 56.dp)
                Spacer(modifier = Modifier.height(EvoraSpacing.md))
                Text(
                    text = "Unlock Full PDF & Pro Suite",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(EvoraSpacing.xs))
                Text(
                    text = "Viewing, editing & saving your docs is always 100% free.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (!state.statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                Surface(
                    shape = EvoraRadii.md,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(EvoraSpacing.md),
                    )
                }
            }

            Spacer(modifier = Modifier.height(EvoraSpacing.lg))
            Surface(
                shape = EvoraRadii.md,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(EvoraSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    benefits.forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = extra.success,
                            )
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvoraSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm)) {
                SubscriptionPlan.entries.forEach { plan ->
                    val offer = offerByPlan[plan] ?: BillingOffer(plan)
                    val isSelected = state.selectedPlan == plan
                    Surface(
                        shape = EvoraRadii.lg,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) extra.brandAccent else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(EvoraRadii.lg)
                            .clickable(enabled = !state.isProcessing) {
                                onIntent(PaywallIntent.SelectPlan(plan))
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(EvoraSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = plan.title,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    if (plan.badge != null) {
                                        Surface(
                                            shape = EvoraRadii.xs,
                                            color = extra.proGoldContainer,
                                        ) {
                                            Text(
                                                text = plan.badge!!,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = extra.onProGoldContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = offer.billingSubtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(EvoraSpacing.sm))
                            Text(
                                text = offer.formattedPrice,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EvoraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs),
        ) {
            EvoraPrimaryButton(
                text = ctaText,
                onClick = { onIntent(PaywallIntent.StartTrialClicked) },
                isLoading = state.isProcessing,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = { onIntent(PaywallIntent.GrantRewardPassClicked) },
                    enabled = !state.isProcessing,
                ) {
                    Text("24h Pro Pass")
                }
                TextButton(
                    onClick = { onIntent(PaywallIntent.SkipClicked) },
                    enabled = !state.isProcessing,
                    modifier = Modifier.testTag("paywall_skip_btn"),
                ) {
                    Text("Continue Free")
                }
            }
        }
    }
}

/**
 * Honest pre-action RewardGateSheet per CD-07 / US-110.
 * Shown upfront before a Pro power tool starts rather than locking Save.
 */
@Composable
fun RewardGateSheet(
    featureTitle: String,
    onWatchRewardedPass: () -> Unit,
    onUpgradePro: () -> Unit,
    onDismiss: () -> Unit,
) {
    EvoraRewardGateSheet(
        featureTitle = featureTitle,
        onWatchRewardedPass = onWatchRewardedPass,
        onUpgradePro = onUpgradePro,
        onDismiss = onDismiss,
    )
}

@Preview(showBackground = true)
@Composable
private fun PaywallScreenPreview() {
    EvoraTheme {
        PaywallScreen(
            state = PaywallState(),
            onIntent = {},
        )
    }
}
