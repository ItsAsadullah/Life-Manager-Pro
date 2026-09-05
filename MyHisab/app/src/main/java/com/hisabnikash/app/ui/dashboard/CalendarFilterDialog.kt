package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CalendarFilterDialog(
    isActive: Boolean = false,
    onDismiss: () -> Unit,
    onOptionSelected: (String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} 
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surface) // Dark Theme
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "নির্বাচন করুন",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    FilterOptionButton("তারিখ অনুযায়ী", onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOptionSelected("date") 
                    })
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FilterOptionButton("মাস অনুযায়ী", onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOptionSelected("month") 
                    })
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FilterOptionButton("বছর অনুযায়ী", onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOptionSelected("year") 
                    })
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FilterOptionButton("কাস্টম রেঞ্জ", onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOptionSelected("custom") 
                    })

                    // যদি ফিল্টার চালু থাকে, তাহলে "ফিল্টার মুছুন" অপশন দেখাবে
                    if (isActive) {
                        Spacer(modifier = Modifier.height(24.dp))
                        FilterOptionButton("ফিল্টার মুছুন", color = Color(0xFFFF3B30), onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOptionSelected(null) 
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun FilterOptionButton(text: String, color: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) 
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 16.sp, color = color, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
    }
}
