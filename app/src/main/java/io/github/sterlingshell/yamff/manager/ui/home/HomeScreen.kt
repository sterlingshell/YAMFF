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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.manager.ui.components.LocalSettingsViewModel
import io.github.sterlingshell.yamff.manager.ui.components.PreferenceCard
import io.github.sterlingshell.yamff.manager.ui.components.SwitchPreference
import io.github.sterlingshell.yamff.manager.ui.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel()
) {
    val settingsViewModel = LocalSettingsViewModel.current
    val openCount by viewModel.openCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_home)) })
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

            PreferenceCard {
                Column(modifier = Modifier.padding(16.dp)) {
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
            }

            Text(
                text = stringResource(R.string.home_quick_settings),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            PreferenceCard {
                SwitchPreference(
                    title = stringResource(R.string.pref_colored_controller),
                    checked = settingsViewModel.config.coloredController,
                    onCheckedChange = { settingsViewModel.updateColoredController(it) }
                )
                SwitchPreference(
                    title = stringResource(R.string.pref_back_home),
                    checked = settingsViewModel.config.recentsBackHome,
                    onCheckedChange = { settingsViewModel.updateRecentsBackHome(it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.currentToWindow() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.enter_window))
                }
                Button(
                    onClick = { viewModel.createWindow() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.RocketLaunch, null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.new_window))
                }
            }

            if (settingsViewModel.config.useAppList) {
                Button(
                    onClick = { viewModel.openAppList() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.open_app_list))
                }
            }

            /*
            // TODO: 暂未完成 - 最近动态功能
            PreferenceCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_recent_logs),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.home_log_window_reset),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            */
            
            Button(
                onClick = { viewModel.resetAllWindow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.reset_all_window))
            }
        }
    }
}

@Composable
fun StatusCard(viewModel: MainViewModel) {
    val config by viewModel.config.collectAsState()
    val activated = viewModel.buildTimeSystem == viewModel.buildTimeModule
    val notActivated = viewModel.buildTimeSystem == 0L
    val needReboot = !activated && !notActivated

    // 状态识别
    var animationState by remember(activated, needReboot) {
        mutableStateOf(
            when {
                notActivated -> "none"
                needReboot -> "breathing_bg"
                activated -> {
                    val last = config.lastSeenActivatedBuildTime
                    val current = viewModel.buildTimeSystem
                    when {
                        last == 0L -> "first_activated"
                        last < current -> "updated"
                        else -> "activated_glow"
                    }
                }
                else -> "none"
            }
        )
    }

    // 更新时间戳
    LaunchedEffect(activated, viewModel.buildTimeSystem) {
        if (activated && config.lastSeenActivatedBuildTime < viewModel.buildTimeSystem) {
            viewModel.updateConfig { it.lastSeenActivatedBuildTime = viewModel.buildTimeSystem }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val animationsEnabled = config.enableStatusAnimations

    // 1. 柔和呼吸光晕 (Glow)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animationsEnabled && animationState != "none" && animationState != "breathing_bg") 0.6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 2. 动态呼吸背景 (Breathing BG)
    val breathingBgColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.tertiaryContainer,
        targetValue = if (animationsEnabled && animationState == "breathing_bg") 
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f) 
            else MaterialTheme.colorScheme.tertiaryContainer,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingBg"
    )

    // 3. 图标脉冲与光环 (Pulse & Halo)
    val haloScale = remember { Animatable(0f) }
    val haloAlpha = remember { Animatable(0f) }
    val iconPulseScale = remember { Animatable(1f) }

    LaunchedEffect(animationState) {
        if (animationsEnabled && animationState == "first_activated") {
            repeat(3) {
                // 并行执行脉冲和光环动画
                val pulse = launch {
                    iconPulseScale.animateTo(1.15f, tween(300))
                    iconPulseScale.animateTo(1f, tween(300))
                }
                val halo = launch {
                    haloScale.snapTo(1f)
                    haloAlpha.snapTo(0.6f)
                    // 动态计算光环缩放，确保不超出 Padding 范围 (24dp padding / 24dp radius = 2.0)
                    haloScale.animateTo(1.8f, tween(600))
                    haloAlpha.animateTo(0f, tween(600))
                }
                pulse.join()
                halo.join()
                delay(200)
            }
            animationState = "activated_glow"
        }
    }

    // 4. 流动渐变闪光 (Shimmer) - 使用 0f 到 1f 的进度
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    LaunchedEffect(animationState) {
        if (animationsEnabled && animationState == "updated") {
            delay(5000)
            animationState = "activated_glow"
        }
    }

    val containerColor = when {
        notActivated -> MaterialTheme.colorScheme.errorContainer
        needReboot -> if (animationsEnabled) breathingBgColor else MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when {
        notActivated -> MaterialTheme.colorScheme.onErrorContainer
        needReboot -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawBehind {
                if (animationsEnabled && activated && animationState != "none" && animationState != "breathing_bg") {
                    // 自适应光晕扩散：卡片高度的 12%
                    val maxSpread = size.height * 0.12f
                    val spread = maxSpread * glowAlpha
                    val glowColor = containerColor.copy(alpha = glowAlpha * 0.3f)
                    drawRoundRect(
                        color = glowColor,
                        topLeft = Offset(-spread, -spread),
                        size = size.copy(
                            width = size.width + spread * 2,
                            height = size.height + spread * 2
                        ),
                        // 圆角比例同步
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.15f)
                    )
                }
            }
            .clip(CardDefaults.shape)
            .drawBehind {
                if (animationsEnabled && animationState == "updated") {
                    // 自适应流光宽度：卡片宽度的 40%
                    val shimmerWidth = size.width * 0.4f
                    val xPos = (size.width + shimmerWidth) * shimmerProgress - shimmerWidth
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
            },
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
            Box(contentAlignment = Alignment.Center) {
                // Halo effect
                if (animationsEnabled && animationState == "first_activated") {
                    Canvas(modifier = Modifier.size(48.dp)) {
                        drawCircle(
                            color = contentColor.copy(alpha = haloAlpha.value),
                            radius = (size.minDimension / 2) * haloScale.value,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                
                Icon(
                    imageVector = when {
                        notActivated -> Icons.Default.Error
                        needReboot -> Icons.Default.Warning
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            if (animationsEnabled && animationState == "first_activated") {
                                scaleX = iconPulseScale.value
                                scaleY = iconPulseScale.value
                            }
                        },
                    tint = contentColor
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = stringResource(
                        when {
                            notActivated -> R.string.not_activated
                            needReboot -> R.string.need_reboot
                            else -> R.string.activated
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (activated) {
                    Text(
                        text = "${viewModel.versionName} (${viewModel.versionCode})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (needReboot) {
                    Text(
                        text = stringResource(R.string.status_system, viewModel.versionName),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(R.string.status_module, io.github.sterlingshell.yamff.BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
