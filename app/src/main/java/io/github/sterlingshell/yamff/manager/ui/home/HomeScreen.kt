package io.github.sterlingshell.yamff.manager.ui.home

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.manager.ui.common.LocalSettingsViewModel
import io.github.sterlingshell.yamff.manager.ui.components.cards.PreferenceCard
import io.github.sterlingshell.yamff.manager.ui.components.items.SwitchPreferenceItem
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class CardStatus {
    NOT_ACTIVATED,
    NEED_REBOOT,
    FIRST_ACTIVATED,
    UPDATED,
    ACTIVATED_GLOW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val settingsViewModel = LocalSettingsViewModel.current
    val openCount by viewModel.openCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_home)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (settingsViewModel.config.useAppList) {
                        viewModel.openAppList()
                    } else {
                        viewModel.currentToWindow()
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (settingsViewModel.config.useAppList) Icons.Default.Apps else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            if (settingsViewModel.config.useAppList) R.string.open_app_list else R.string.enter_window
                        )
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(viewModel)

            OpenCountCard(viewModel, openCount)

            Text(
                text = stringResource(R.string.home_quick_settings),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            PreferenceCard {
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_colored_controller),
                    checked = settingsViewModel.config.coloredController,
                    onCheckedChange = { settingsViewModel.updateColoredController(it) }
                )
                SwitchPreferenceItem(
                    title = stringResource(R.string.pref_back_home),
                    checked = settingsViewModel.config.recentsBackHome,
                    onCheckedChange = { settingsViewModel.updateRecentsBackHome(it) }
                )
            }
        }
    }
}

@Composable
fun StatusCard(viewModel: HomeViewModel) {
    val config by viewModel.config.collectAsState()
    val activated = viewModel.buildTimeSystem == viewModel.buildTimeModule
    val notActivated = viewModel.buildTimeSystem == 0L
    val needReboot = !activated && !notActivated

    var cardStatus by remember(activated, needReboot) {
        mutableStateOf(
            when {
                notActivated -> CardStatus.NOT_ACTIVATED
                needReboot -> CardStatus.NEED_REBOOT
                activated -> {
                    val last = config.lastSeenActivatedBuildTime
                    val current = viewModel.buildTimeSystem
                    when {
                        last == 0L -> CardStatus.FIRST_ACTIVATED
                        last < current -> CardStatus.UPDATED
                        else -> CardStatus.ACTIVATED_GLOW
                    }
                }
                else -> CardStatus.NOT_ACTIVATED
            }
        )
    }

    LaunchedEffect(activated, viewModel.buildTimeSystem) {
        if (activated && config.lastSeenActivatedBuildTime < viewModel.buildTimeSystem) {
            viewModel.updateConfig { it.lastSeenActivatedBuildTime = viewModel.buildTimeSystem }
        }
    }

    LaunchedEffect(cardStatus) {
        if (config.enableStatusAnimations && cardStatus == CardStatus.UPDATED) {
            delay(5.seconds)
            cardStatus = CardStatus.ACTIVATED_GLOW
        }
    }

    val containerColor = when (cardStatus) {
        CardStatus.NOT_ACTIVATED -> MaterialTheme.colorScheme.errorContainer
        CardStatus.NEED_REBOOT -> {
            if (config.enableStatusAnimations) {
                rememberBreathingBgColor()
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
        }
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when (cardStatus) {
        CardStatus.NOT_ACTIVATED -> MaterialTheme.colorScheme.onErrorContainer
        CardStatus.NEED_REBOOT -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val glowAlpha = rememberGlowAlpha(config.enableStatusAnimations, cardStatus)
    val shimmerProgress = rememberShimmerProgress(config.enableStatusAnimations && cardStatus == CardStatus.UPDATED)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawGlowEffect(glowAlpha, containerColor)
            .clip(CardDefaults.shape)
            .drawShimmerEffect(shimmerProgress),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIconArea(
                cardStatus = cardStatus,
                animationsEnabled = config.enableStatusAnimations,
                contentColor = contentColor,
                onFirstActivatedAnimationEnd = { cardStatus = CardStatus.ACTIVATED_GLOW }
            )

            Spacer(modifier = Modifier.size(16.dp))

            StatusTextInfo(
                cardStatus = cardStatus,
                versionName = viewModel.versionName,
                versionCode = viewModel.versionCode.toString()
            )
        }
    }
}

@Composable
private fun StatusIconArea(
    cardStatus: CardStatus,
    animationsEnabled: Boolean,
    contentColor: Color,
    onFirstActivatedAnimationEnd: () -> Unit
) {
    val haloScale = remember { Animatable(0f) }
    val haloAlpha = remember { Animatable(0f) }
    val iconPulseScale = remember { Animatable(1f) }

    LaunchedEffect(cardStatus) {
        if (animationsEnabled && cardStatus == CardStatus.FIRST_ACTIVATED) {
            repeat(3) {
                val pulse = launch {
                    iconPulseScale.animateTo(1.15f, tween(300))
                    iconPulseScale.animateTo(1f, tween(300))
                }
                val halo = launch {
                    haloScale.snapTo(1f)
                    haloAlpha.snapTo(0.6f)
                    haloScale.animateTo(1.8f, tween(600))
                    haloAlpha.animateTo(0f, tween(600))
                }
                pulse.join()
                halo.join()
                delay(200.milliseconds)
            }
            onFirstActivatedAnimationEnd()
        }
    }

    Box(contentAlignment = Alignment.Center) {
        if (animationsEnabled && cardStatus == CardStatus.FIRST_ACTIVATED) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawCircle(
                    color = contentColor.copy(alpha = haloAlpha.value),
                    radius = (size.minDimension / 2) * haloScale.value,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Icon(
            imageVector = when (cardStatus) {
                CardStatus.NOT_ACTIVATED -> Icons.Default.Error
                CardStatus.NEED_REBOOT -> Icons.Default.Warning
                else -> Icons.Default.CheckCircle
            },
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    if (animationsEnabled && cardStatus == CardStatus.FIRST_ACTIVATED) {
                        scaleX = iconPulseScale.value
                        scaleY = iconPulseScale.value
                    }
                },
            tint = contentColor
        )
    }
}

@Composable
private fun StatusTextInfo(
    cardStatus: CardStatus,
    versionName: String,
    versionCode: String
) {
    Column {
        Text(
            text = stringResource(
                when (cardStatus) {
                    CardStatus.NOT_ACTIVATED -> R.string.not_activated
                    CardStatus.NEED_REBOOT -> R.string.need_reboot
                    else -> R.string.activated
                }
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        when (cardStatus) {
            CardStatus.NOT_ACTIVATED -> {}
            CardStatus.NEED_REBOOT -> {
                Text(
                    text = stringResource(R.string.status_system, versionName),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.status_module, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> {
                Text(
                    text = "$versionName ($versionCode)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


@Composable
private fun OpenCountCard(viewModel: HomeViewModel, openCount: Int) {
    PreferenceCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.open_count),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = openCount.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black
                )
            }
            if (openCount > 0) {
                IconButton(
                    onClick = { viewModel.resetAllWindow() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.reset_all_window)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberBreathingBgColor(): Color {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.tertiaryContainer,
        targetValue = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingBg"
    )
    return color
}

@Composable
private fun rememberGlowAlpha(enabled: Boolean, status: CardStatus): Float {
    if (!enabled || status == CardStatus.NOT_ACTIVATED || status == CardStatus.NEED_REBOOT) return 0f
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    return alpha
}

@Composable
private fun rememberShimmerProgress(enabled: Boolean): Float {
    if (!enabled) return 0f
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    return progress
}

private fun Modifier.drawGlowEffect(glowAlpha: Float, glowContainerColor: Color) = this.drawBehind {
    if (glowAlpha > 0f) {
        val maxSpread = size.height * 0.12f
        val spread = maxSpread * glowAlpha
        val glowColor = glowContainerColor.copy(alpha = glowAlpha * 0.3f)
        drawRoundRect(
            color = glowColor,
            topLeft = Offset(-spread, -spread),
            size = size.copy(
                width = size.width + spread * 2,
                height = size.height + spread * 2
            ),
            cornerRadius = CornerRadius(size.height * 0.15f)
        )
    }
}

private fun Modifier.drawShimmerEffect(progress: Float) = this.drawBehind {
    if (progress > 0f) {
        val shimmerWidth = size.width * 0.4f
        val xPos = (size.width + shimmerWidth) * progress - shimmerWidth
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.3f),
                Color.Transparent
            ),
            start = Offset(xPos, 0f),
            end = Offset(xPos + shimmerWidth, size.height)
        )
        drawRect(brush = brush)
    }
}