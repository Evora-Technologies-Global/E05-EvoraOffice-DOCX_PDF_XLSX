package com.evora.technologies.office.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun ProvideReducedMotion(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduced = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(LocalReducedMotion provides reduced, content = content)
}

@Composable
fun ShimmerDocListSkeleton(
    itemCount: Int = 5,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val extra = EvoraThemeTokens.colors
    val alpha = if (reducedMotion) {
        0.7f
    } else {
        val transition = rememberInfiniteTransition(label = "docShimmer")
        val animated by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "shimmerAlpha",
        )
        animated
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
    ) {
        repeat(itemCount) {
            Surface(
                shape = EvoraRadii.md,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(EvoraSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(EvoraRadii.sm)
                            .background(extra.surfaceSubtle.copy(alpha = alpha)),
                    )
                    Spacer(modifier = Modifier.width(EvoraSpacing.md))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .clip(EvoraRadii.xs)
                                .background(extra.surfaceSubtle.copy(alpha = alpha)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(12.dp)
                                .clip(EvoraRadii.xs)
                                .background(extra.surfaceSubtle.copy(alpha = alpha)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Description,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(EvoraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(EvoraSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(EvoraSpacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(EvoraSpacing.lg))
            EvoraPrimaryButton(
                text = primaryActionLabel,
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(0.85f),
            )
        }
    }
}

@Composable
fun ErrorStateCard(
    title: String,
    description: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
) {
    val extra = EvoraThemeTokens.colors
    val container = if (isWarning) extra.warningContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (isWarning) extra.onWarningContainer else MaterialTheme.colorScheme.onErrorContainer

    Surface(
        shape = EvoraRadii.md,
        color = container,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(EvoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = content,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = content,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
            )
            EvoraOutlinedButton(
                text = actionLabel,
                onClick = onActionClick,
            )
        }
    }
}

@Composable
fun PasswordUnlockDialog(
    fileName: String,
    errorMessage: String?,
    onUnlock: (String) -> Unit,
    onDismiss: () -> Unit,
    isUnlocking: Boolean = false,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Unlock Protected Document",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm)) {
                Text(
                    text = "$fileName is password-protected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter password") },
                    singleLine = true,
                    isError = errorMessage != null,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        EvoraIconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onUnlock(password) },
                enabled = password.isNotEmpty() && !isUnlocking,
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
