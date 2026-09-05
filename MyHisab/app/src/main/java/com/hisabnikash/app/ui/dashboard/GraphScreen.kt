package com.hisabnikash.app.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

// ক্যাটাগরির রঙ
private val categoryColors = listOf(
    Color(0xFF0A84FF), Color(0xFF34C759), Color(0xFFFF9F0A),
    Color(0xFFFF375F), Color(0xFF64D2FF), Color(0xFFBF5AF2),
    Color(0xFFFF6B35), Color(0xFF30D158), Color(0xFFFFD60A),
    Color(0xFF636366)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphBottomSheet(
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        GraphContent(transactions = transactions, onDismiss = onDismiss)
    }
}

@Composable
private fun GraphContent(
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
    ) {
        // হেডার
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "আর্থিক গ্রাফ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "বন্ধ করুন",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // ট্যাব বার
        GraphTabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        Spacer(modifier = Modifier.height(16.dp))

        // কন্টেন্ট
        when (selectedTab) {
            0 -> BarChartSection(transactions = transactions)
            1 -> PieChartSection(transactions = transactions)
            2 -> LineChartSection(transactions = transactions)
        }
    }
}

@Composable
private fun GraphTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(
        Triple(Icons.Default.BarChart, "বার", "দৈনিক"),
        Triple(Icons.Default.PieChart, "পাই", "ক্যাটাগরি"),
        Triple(Icons.Default.Timeline, "লাইন", "ট্রেন্ড")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, (icon, label, _) ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) Color(0xFF0A84FF)
                        else Color.Transparent
                    )
                    .clickableNoRipple { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ===== বার চার্ট =====
@Composable
private fun BarChartSection(transactions: List<TransactionEntity>) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(transactions) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }

    // শেষ ৭ দিনের ডেটা
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val last7Days = (6 downTo 0).map { offset ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
        val banglaDate = sdf.format(cal.time).map { c ->
            if (c in '0'..'9') "০১২৩৪৫৬৭৮৯"[c - '0'] else c
        }.joinToString("")
        val dayIncome = transactions.filter { it.isIncome && it.date.replace(" ", "") == banglaDate.replace(" ", "") }.sumOf { it.amount }
        val dayExpense = transactions.filter { !it.isIncome && it.date.replace(" ", "") == banglaDate.replace(" ", "") }.sumOf { it.amount }
        val dayLabel = SimpleDateFormat("d", Locale.getDefault()).format(cal.time).map { c ->
            if (c in '0'..'9') "০১২৩৪৫৬৭৮৯"[c - '0'] else c
        }.joinToString("")
        Triple(dayLabel, dayIncome, dayExpense)
    }

    val maxVal = last7Days.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
    ) {
        item {
            // সারসংক্ষেপ কার্ড
            val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
            val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
            SummaryCards(totalIncome, totalExpense)
            Spacer(modifier = Modifier.height(20.dp))
            Text("শেষ ৭ দিনের আয় ও ব্যয়", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            // লেজেন্ড
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = IncomeGreen, label = "আয়")
                LegendDot(color = ExpenseRed, label = "ব্যয়")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // বার চার্ট
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                val barGroupWidth = size.width / last7Days.size
                val barWidth = barGroupWidth * 0.3f
                val maxBarHeight = size.height - 30.dp.toPx()
                val anim = animProgress.value

                last7Days.forEachIndexed { index, (_, income, expense) ->
                    val groupX = barGroupWidth * index + barGroupWidth / 2f
                    val incomeHeight = ((income / maxVal) * maxBarHeight * anim).toFloat()
                    val expenseHeight = ((expense / maxVal) * maxBarHeight * anim).toFloat()

                    // আয়ের বার (বাম)
                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(groupX - barWidth - 2.dp.toPx(), size.height - 30.dp.toPx() - incomeHeight),
                        size = Size(barWidth, incomeHeight + 1f),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    // ব্যয়ের বার (ডান)
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(groupX + 2.dp.toPx(), size.height - 30.dp.toPx() - expenseHeight),
                        size = Size(barWidth, expenseHeight + 1f),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // গ্রিড লাইন (হালকা)
                    if (index == 0) {
                        for (i in 1..4) {
                            val y = size.height - 30.dp.toPx() - (maxBarHeight * i / 4f)
                            drawLine(
                                color = Color.White.copy(alpha = 0.07f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            // X-অক্ষ লেবেল
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                last7Days.forEach { (day, _, _) ->
                    Text(day, fontSize = 11.sp, color = TextWhiteSecondary, textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ===== পাই চার্ট =====
@Composable
private fun PieChartSection(transactions: List<TransactionEntity>) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(transactions) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    val expenses = transactions.filter { !it.isIncome }
    val categoryMap = expenses.groupBy { it.category.ifBlank { "অন্যান্য" } }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .filter { it.value > 0 }
        .toList()
        .sortedByDescending { it.second }

    val total = categoryMap.sumOf { it.second }.coerceAtLeast(1.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
    ) {
        item {
            Text("ক্যাটাগরি অনুযায়ী ব্যয়", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            if (categoryMap.isEmpty()) {
                EmptyChartMessage("কোনো ব্যয়ের ডেটা পাওয়া যায়নি")
            } else {
                // পাই চার্ট
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(220.dp)) {
                        val radius = min(size.width, size.height) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val strokeWidth = 52.dp.toPx()
                        var startAngle = -90f

                        categoryMap.forEachIndexed { index, (_, amount) ->
                            val sweep = ((amount / total) * 360f * animProgress.value).toFloat()
                            val color = categoryColors.getOrElse(index) { Color.Gray }
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(center.x - radius + strokeWidth / 2, center.y - radius + strokeWidth / 2),
                                size = Size(radius * 2 - strokeWidth, radius * 2 - strokeWidth),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep + 1.5f
                        }
                    }

                    // মাঝে মোট ব্যয়
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("মোট ব্যয়", fontSize = 11.sp, color = TextWhiteSecondary)
                        Text(
                            "৳${total.toBanglaString()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // লেজেন্ড লিস্ট
                categoryMap.forEachIndexed { index, (category, amount) ->
                    val color = categoryColors.getOrElse(index) { Color.Gray }
                    val percent = (amount / total * 100)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            category,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${percent.toBanglaString()}%",
                            fontSize = 13.sp,
                            color = color,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "৳${amount.toBanglaString()}",
                            fontSize = 13.sp,
                            color = TextWhiteSecondary
                        )
                    }
                    if (index < categoryMap.size - 1) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.07f),
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ===== লাইন চার্ট =====
@Composable
private fun LineChartSection(transactions: List<TransactionEntity>) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(transactions) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    // শেষ ৭ দিনের ব্যালেন্স
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("bn", "BD"))
    val last7Days = (6 downTo 0).map { offset ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
        val banglaDate = sdf.format(cal.time).map { c ->
            if (c in '0'..'9') "০১২৩৪৫৬৭৮৯"[c - '0'] else c
        }.joinToString("")
        val dayIncome = transactions.filter { it.isIncome && it.date.replace(" ", "") == banglaDate.replace(" ", "") }.sumOf { it.amount }
        val dayExpense = transactions.filter { !it.isIncome && it.date.replace(" ", "") == banglaDate.replace(" ", "") }.sumOf { it.amount }
        val dayLabel = SimpleDateFormat("d", Locale.getDefault()).format(cal.time).map { c ->
            if (c in '0'..'9') "০১২৩৪৫৬৭৮৯"[c - '0'] else c
        }.joinToString("")
        Pair(dayLabel, dayIncome - dayExpense)
    }

    val maxVal = last7Days.maxOf { it.second }.coerceAtLeast(1.0)
    val minVal = last7Days.minOf { it.second }.coerceAtMost(0.0)
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
    ) {
        item {
            Text("ব্যালেন্স ট্রেন্ড (শেষ ৭ দিন)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                val paddingLeft = 10.dp.toPx()
                val paddingRight = 10.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingBottom = 30.dp.toPx()
                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                val points = last7Days.mapIndexed { index, (_, balance) ->
                    val x = paddingLeft + (index / (last7Days.size - 1f)) * chartWidth
                    val normalizedY = ((balance - minVal) / range).toFloat()
                    val y = paddingTop + chartHeight - (normalizedY * chartHeight * animProgress.value)
                    Offset(x, y)
                }

                // গ্রিড লাইন
                for (i in 0..4) {
                    val y = paddingTop + chartHeight * i / 4f
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.size > 1) {
                    // গ্রেডিয়েন্ট ফিল
                    val fillPath = Path().apply {
                        moveTo(points.first().x, paddingTop + chartHeight)
                        points.forEachIndexed { i, point ->
                            if (i == 0) lineTo(point.x, point.y)
                            else {
                                val prev = points[i - 1]
                                val cp1 = Offset((prev.x + point.x) / 2f, prev.y)
                                val cp2 = Offset((prev.x + point.x) / 2f, point.y)
                                cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, point.x, point.y)
                            }
                        }
                        lineTo(points.last().x, paddingTop + chartHeight)
                        close()
                    }
                    drawPath(
                        fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(BalanceBlue.copy(alpha = 0.35f), Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )

                    // লাইন
                    val linePath = Path().apply {
                        points.forEachIndexed { i, point ->
                            if (i == 0) moveTo(point.x, point.y)
                            else {
                                val prev = points[i - 1]
                                val cp1 = Offset((prev.x + point.x) / 2f, prev.y)
                                val cp2 = Offset((prev.x + point.x) / 2f, point.y)
                                cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, point.x, point.y)
                            }
                        }
                    }
                    drawPath(
                        linePath,
                        color = BalanceBlue,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // ডট পয়েন্ট
                    points.forEach { point ->
                        drawCircle(color = surfaceColor, radius = 5.dp.toPx(), center = point)
                        drawCircle(color = BalanceBlue, radius = 4.dp.toPx(), center = point)
                    }
                }
            }

            // X-অক্ষ লেবেল
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last7Days.forEach { (day, _) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day, fontSize = 11.sp, color = TextWhiteSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ডেটা টেবিল
            Text("দৈনিক বিস্তারিত", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            last7Days.forEachIndexed { index, (day, balance) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", fontSize = 12.sp, color = TextWhiteSecondary, modifier = Modifier.width(20.dp))
                    Text("$day তারিখ", fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f))
                    Text(
                        if (balance >= 0) "+৳${balance.toBanglaString()}" else "-৳${(-balance).toBanglaString()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balance >= 0) IncomeGreen else ExpenseRed
                    )
                }
                if (index < last7Days.size - 1) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ===== হেল্পার কম্পোজেবল =====

@Composable
private fun SummaryCards(income: Double, expense: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "মোট আয়",
            amount = "৳${income.toBanglaString()}",
            color = IncomeGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "মোট ব্যয়",
            amount = "৳${expense.toBanglaString()}",
            color = ExpenseRed,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "ব্যালেন্স",
            amount = "৳${(income - expense).toBanglaString()}",
            color = BalanceBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = TextWhiteSecondary)
    }
}

@Composable
private fun EmptyChartMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextWhiteSecondary, fontSize = 14.sp)
    }
}

// ট্যাব ক্লিকের জন্য হেল্পার এক্সটেনশন
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onClick
        )
    )
}
