package com.hisabnikash.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * ট্যাব আইটেমের মডেল ক্লাস
 */
data class TabItem(
    val icon: ImageVector,
    val title: String = ""
)

/**
 * ============================================================================
 * "COSMIC AURORA" HOLOGRAPHIC NAVIGATION (STABLE & CRASH-FREE)
 * ============================================================================
 * ক্র্যাশ ফিক্স করা হয়েছে (Modifier.padding-এর নেগেটিভ ভ্যালু দূর করে ক্যানভাসে ড্র করা হয়েছে)।
 * অ্যাক্টিভ ট্যাবের উপরের সাদা বারটি সম্পূর্ণ রিমুভ করা হয়েছে।
 */
@Composable
fun LiquidGlassIconNavigation(
    items: List<TabItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState? = null,
    tabWidth: Dp? = null,
    tabHeight: Dp = 48.dp,
    tabSpacing: Dp = 8.dp,
    cornerRadius: Dp = 15.dp,
    activeColor: Color = Color(0xFF00F0FF)
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val resolvedTabWidth = tabWidth ?: tabHeight
    val activeIndex = if (pagerState != null) pagerState.targetPage else selectedIndex

    // ১. জীবন্ত অরোরা আলোর ঘূর্ণন অ্যানিমেশন
    val infiniteTransition = rememberInfiniteTransition(label = "auroraTransition")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    // ২. কাইনেটিক ওয়ার্প স্ট্রেচ এনিমেশন ইঞ্জিন
    var lastIndex by remember { mutableIntStateOf(activeIndex) }
    val stretchScaleX = remember { Animatable(1.0f) }
    val stretchScaleY = remember { Animatable(1.0f) }
    val arrivalRipple = remember { Animatable(0.0f) }

    val targetOffsetDp = if (pagerState != null && pagerState.isScrollInProgress) {
        val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
        (resolvedTabWidth + tabSpacing) * position
    } else {
        (resolvedTabWidth + tabSpacing) * activeIndex
    }

    // মাখনের মতো মসৃণ কাইনেটিক স্প্রিং গ্লাইড
    val indicatorOffset by animateDpAsState(
        targetValue = targetOffsetDp,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = 520f
        ),
        label = "auroraOffset"
    )

    // ট্যাব পরিবর্তনের মুহূর্তে কাইনেটিক ওয়ার্প এবং এনার্জি পালস ট্রিগার
    LaunchedEffect(activeIndex) {
        if (activeIndex != lastIndex) {
            lastIndex = activeIndex
            // কাইনেটিক স্ট্রেচ
            launch {
                stretchScaleX.animateTo(1.22f, tween(110, easing = FastOutSlowInEasing))
                stretchScaleX.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(dampingRatio = 0.52f, stiffness = 550f)
                )
            }
            launch {
                stretchScaleY.animateTo(0.86f, tween(110, easing = FastOutSlowInEasing))
                stretchScaleY.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(dampingRatio = 0.52f, stiffness = 550f)
                )
            }
            // টার্গেটে পৌঁছার মুহূর্তে নিওন এনার্জি রিং স্প্ল্যাশ
            launch {
                arrivalRipple.snapTo(0.0f)
                arrivalRipple.animateTo(1.0f, tween(durationMillis = 380, easing = FastOutSlowInEasing))
            }
        }
    }

    // স্ক্রিনের সাথে অটো স্ক্রোল
    LaunchedEffect(activeIndex) {
        val targetScrollPx = with(density) {
            ((resolvedTabWidth + tabSpacing) * activeIndex - 100.dp).toPx().coerceAtLeast(0f)
        }
        scrollState.animateScrollTo(
            value = targetScrollPx.toInt(),
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )
    }

    val capsuleShape = RoundedCornerShape(cornerRadius)
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(tabHeight)
                .horizontalScroll(scrollState),
            contentAlignment = Alignment.CenterStart
        ) {
            // -------------------------------------------------------------
            // LAYER 1: Futuristic Inactive Pods (মডার্ন স্লিক মিনিমাল পডস)
            // -------------------------------------------------------------
            Row(
                horizontalArrangement = Arrangement.spacedBy(tabSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, _ ->
                    val isSelected = activeIndex == index
                    val podBg = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.035f)
                    val podBorder = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)

                    Box(
                        modifier = Modifier
                            .width(resolvedTabWidth)
                            .height(tabHeight)
                            .clip(capsuleShape)
                            .background(if (isSelected) Color.Transparent else podBg)
                            .border(
                                width = 0.85.dp,
                                color = if (isSelected) Color.Transparent else podBorder,
                                shape = capsuleShape
                            )
                    )
                }
            }

            // -------------------------------------------------------------
            // LAYER 2: THE "COSMIC AURORA" HOLOGRAPHIC POD
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(resolvedTabWidth)
                    .height(tabHeight)
                    .graphicsLayer {
                        scaleX = stretchScaleX.value
                        scaleY = stretchScaleY.value
                    },
                contentAlignment = Alignment.Center
            ) {
                // ক) এনার্জি রিং পালস (নিরাপদ ক্যানভাস ড্রয়িং - কোনো ক্র্যাশ নেই)
                if (arrivalRipple.value > 0.01f && arrivalRipple.value < 0.99f) {
                    val rippleProgress = arrivalRipple.value
                    val rippleAlpha = (1f - rippleProgress).coerceIn(0f, 1f) * 0.70f
                    val rippleExpandPx = with(density) { (rippleProgress * 12).dp.toPx() }

                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00F0FF).copy(alpha = rippleAlpha),
                                    Color(0xFF7000FF).copy(alpha = rippleAlpha * 0.40f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.75f + rippleExpandPx
                            ),
                            topLeft = Offset(-rippleExpandPx, -rippleExpandPx),
                            size = Size(size.width + 2f * rippleExpandPx, size.height + 2f * rippleExpandPx),
                            cornerRadius = CornerRadius(
                                cornerRadiusPx + rippleExpandPx,
                                cornerRadiusPx + rippleExpandPx
                            ),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // খ) ডিপ অ্যাম্বিয়েন্ট অরোরা গ্লো (Multi-colored Neon Bloom)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .shadow(
                            elevation = 14.dp,
                            shape = capsuleShape,
                            spotColor = Color(0xFF00F0FF).copy(alpha = 0.55f),
                            ambientColor = Color(0xFF7000FF).copy(alpha = 0.40f)
                        )
                        .clip(capsuleShape)
                ) {
                    // গ) ওবসিডিয়ান ক্রিস্টাল কাঁচের বডি (Deep Obsidian Glass)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (isDark) listOf(
                                        Color(0xFF1B2A4A).copy(alpha = 0.85f),
                                        Color(0xFF0C1424).copy(alpha = 0.92f),
                                        Color(0xFF060B14).copy(alpha = 0.96f)
                                    ) else listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        Color(0xFFE8F4FD).copy(alpha = 0.90f),
                                        Color(0xFFD3E9FA).copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // ঘ) নিওন অরোরা ডায়নামিক লাইট সেন্টার (Bioluminescent Center Bloom)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00F0FF).copy(alpha = if (isDark) 0.35f else 0.25f),
                                    Color(0xFF7000FF).copy(alpha = if (isDark) 0.20f else 0.10f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height * 0.45f),
                                radius = size.width * 0.70f
                            ),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )

                        // ৩ডি ডোম সফট লেন্স গ্লিসেনিং
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.25f else 0.40f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.50f
                            ),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    }

                    // ঙ) জীবন্ত ফাইবার-অপটিক অরোরা বর্ডার (Living Shimmering Aurora Rim)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val angleRad = shimmerPhase * 2f * Math.PI.toFloat()
                        val startX = size.width / 2f + (size.width / 2f) * cos(angleRad)
                        val startY = size.height / 2f + (size.height / 2f) * sin(angleRad)
                        val endX = size.width / 2f - (size.width / 2f) * cos(angleRad)
                        val endY = size.height / 2f - (size.height / 2f) * sin(angleRad)

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00F0FF), // সাইবার সায়ান
                                    Color(0xFF0072FF), // ডিপ ব্লু
                                    Color(0xFF9D00FF), // আল্ট্রাভায়োলেট
                                    Color(0xFFFF007A), // নিওন রোজ
                                    Color(0xFF00F0FF)  // আবার সায়ান
                                ),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY)
                            ),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // নোট: ইউজারের অনুরোধে উপরের সাদা বারটি সম্পূর্ণ রিমুভ করা হয়েছে
                    }
                }
            }

            // -------------------------------------------------------------
            // LAYER 3: Elevated Holographic Icons & Typography
            // -------------------------------------------------------------
            Row(
                horizontalArrangement = Arrangement.spacedBy(tabSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = activeIndex == index

                    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.42f)
                    val activeIconColor = if (isDark) Color(0xFF00F0FF) else Color(0xFF0066FF)
                    val activeTextColor = if (isDark) Color(0xFF7CEBFF) else Color(0xFF0056D6)

                    // আইকন লিফট-আপ এবং স্প্রিং স্কেল
                    val iconOffsetY by animateDpAsState(
                        targetValue = if (isSelected) (-5).dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                        label = "holoIconY"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 0.92f,
                        animationSpec = spring(dampingRatio = 0.58f, stiffness = 450f),
                        label = "holoIconScale"
                    )
                    val textAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "holoTextAlpha"
                    )
                    val textBlur by animateDpAsState(
                        targetValue = if (isSelected) 0.dp else 4.dp,
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "holoTextBlur"
                    )

                    Box(
                        modifier = Modifier
                            .width(resolvedTabWidth)
                            .height(tabHeight)
                            .clip(capsuleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onItemSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        // ১. সক্রিয় আইকন (নিওন গ্লো সহ থ্রিডি ফ্লোটিং আইকন)
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) activeIconColor else inactiveColor,
                            modifier = Modifier
                                .offset(y = iconOffsetY)
                                .size(21.dp)
                                .scale(iconScale)
                        )

                        // ২. টাইটেল (সক্রিয় অবস্থায় নিওন আভা সহ নিচে ভেসে উঠবে)
                        if (item.title.isNotEmpty()) {
                            Text(
                                text = item.title,
                                color = activeTextColor.copy(alpha = textAlpha),
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 3.5.dp)
                                    .alpha(textAlpha)
                                    .blur(textBlur)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * পূর্ববর্তী কোডবেজের সাথে ১০০% ব্যাকওয়ার্ড কম্প্যাটিবিলিটি বজায় রাখার জন্য
 * Wrapper ফাংশন।
 */
@Composable
fun LiquidGlassTabBar(
    tabs: List<TabItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState? = null,
    tabWidth: Dp = 48.dp,
    tabHeight: Dp = 48.dp,
    tabSpacing: Dp = 10.dp,
    cornerRadius: Dp = 15.dp
) {
    LiquidGlassIconNavigation(
        items = tabs,
        selectedIndex = selectedTab,
        onItemSelected = onTabSelected,
        modifier = modifier,
        pagerState = pagerState,
        tabWidth = tabWidth,
        tabHeight = tabHeight,
        tabSpacing = tabSpacing,
        cornerRadius = cornerRadius
    )
}
