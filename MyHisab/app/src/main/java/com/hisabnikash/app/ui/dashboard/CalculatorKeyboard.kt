package com.hisabnikash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun CustomCalculatorKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val handleInput = { input: String ->
        val currentVal = value.toEnglishDigits().trim()
        when (input) {
            "AC" -> onValueChange("")
            "⌫" -> if (currentVal.isNotEmpty()) onValueChange(currentVal.dropLast(1))
            "=" -> {
                val result = evaluateMathExpression(currentVal)
                if (result != null) onValueChange(result.toEnglishDigits())
            }
            "%" -> {
                val result = evaluateMathExpression(currentVal)
                if (result != null) {
                    val percentage = (result.toEnglishDouble()) / 100.0
                    onValueChange(formatResult(percentage).toEnglishDigits())
                } else if (currentVal.isNotEmpty() && currentVal.last().isDigit()) {
                    onValueChange("$currentVal%")
                }
            }
            else -> {
                val operators = charArrayOf('+', '-', '×', '÷')
                val isOperator = input in listOf("+", "-", "×", "÷")
                val lastChar = currentVal.lastOrNull()
                
                if (isOperator) {
                    if (currentVal.isEmpty()) {
                        if (input == "-") onValueChange("-")
                    } else if (lastChar != null && lastChar in operators) {
                        onValueChange(currentVal.dropLast(1) + input)
                    } else if (lastChar == '.') {
                        onValueChange(currentVal.dropLast(1) + input)
                    } else {
                        onValueChange(currentVal + input)
                    }
                } else if (input == ".") {
                    val lastOpIndex = currentVal.indexOfLast { it in operators }
                    val lastSegment = if (lastOpIndex == -1) currentVal else currentVal.substring(lastOpIndex + 1)
                    
                    if (currentVal.isEmpty()) {
                        onValueChange("0.")
                    } else if (lastChar != null && lastChar in operators) {
                        onValueChange("${currentVal}0.")
                    } else if (!lastSegment.contains('.')) {
                        onValueChange("$currentVal.")
                    }
                } else {
                    // Digit 0-9
                    val englishDigit = input.toEnglishDigits()
                    val lastOpIndex = currentVal.indexOfLast { it in operators }
                    val prefix = if (lastOpIndex == -1) "" else currentVal.substring(0, lastOpIndex + 1)
                    val lastSegment = if (lastOpIndex == -1) currentVal else currentVal.substring(lastOpIndex + 1)
                    
                    if (lastSegment == "0" || lastSegment == "০" || (lastSegment.isNotEmpty() && lastSegment.all { it == '0' || it == '০' })) {
                        if (englishDigit == "0") {
                            onValueChange(prefix + "0")
                        } else {
                            onValueChange(prefix + englishDigit)
                        }
                    } else {
                        onValueChange((currentVal + englishDigit).toEnglishDigits())
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            CalcButton("AC", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("AC") }
            CalcButton("%", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("%") }
            CalcButton("÷", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), Color(0xFF0A84FF)) { handleInput("÷") }
            CalcButton("×", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), Color(0xFF0A84FF)) { handleInput("×") }
        }
        // Row 2
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            CalcButton("1", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("1") }
            CalcButton("2", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("2") }
            CalcButton("3", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("3") }
            CalcButton("-", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), Color(0xFF0A84FF)) { handleInput("-") }
        }
        // Rows 3, 4, 5 block
        Row(modifier = Modifier.fillMaxWidth().height(144.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            // Left 3 columns
            Column(modifier = Modifier.weight(3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    CalcButton("4", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("4") }
                    CalcButton("5", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("5") }
                    CalcButton("6", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("6") }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    CalcButton("7", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("7") }
                    CalcButton("8", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("8") }
                    CalcButton("9", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("9") }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)).clickable { handleInput("⌫") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                    CalcButton("0", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("0") }
                    CalcButton(".", Modifier.weight(1f), androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput(".") }
                }
            }
            // Right 1 column
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                CalcButton("+", Modifier.weight(2f), androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), Color(0xFF0A84FF)) { handleInput("+") }
                CalcButton("=", Modifier.weight(1f), Color(0xFF0A84FF).copy(alpha = 0.8f), androidx.compose.material3.MaterialTheme.colorScheme.onSurface) { handleInput("=") }
            }
        }
    }
}

@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

// Basic Math Evaluator
fun evaluateMathExpression(expression: String): String? {
    if (expression.isBlank()) return null
    try {
        var exp = expression.toEnglishDigits().replace("×", "*").replace("÷", "/")
        val tokens = tokenize(exp)
        if (tokens.isEmpty()) return null
        
        val value = parseExpression(tokens) ?: return null
        return formatResult(value)
    } catch (e: Exception) {
        return null
    }
}

private fun formatResult(value: Double): String {
    val format = DecimalFormat("0.##")
    return format.format(value)
}

private fun tokenize(exp: String): List<String> {
    val tokens = mutableListOf<String>()
    var currentNumber = ""
    for (char in exp) {
        if (char.isDigit() || char == '.') {
            currentNumber += char
        } else if (char == '%') {
            if (currentNumber.isNotEmpty()) {
                tokens.add(currentNumber)
                currentNumber = ""
            }
            tokens.add("%")
        } else if (char in listOf('+', '-', '*', '/')) {
            if (currentNumber.isNotEmpty()) {
                tokens.add(currentNumber)
                currentNumber = ""
            }
            tokens.add(char.toString())
        }
    }
    if (currentNumber.isNotEmpty()) {
        tokens.add(currentNumber)
    }
    return tokens
}

private fun parseExpression(tokens: List<String>): Double? {
    if (tokens.isEmpty()) return null
    
    val processedTokens = mutableListOf<String>()
    var i = 0
    while (i < tokens.size) {
        if (tokens[i] == "%") {
            if (processedTokens.isNotEmpty()) {
                val last = processedTokens.removeLast().toDoubleOrNull() ?: 0.0
                processedTokens.add((last / 100.0).toString())
            }
        } else {
            processedTokens.add(tokens[i])
        }
        i++
    }
    
    val multDivTokens = mutableListOf<String>()
    i = 0
    while (i < processedTokens.size) {
        val t = processedTokens[i]
        if (t == "*" || t == "/") {
            val left = multDivTokens.removeLast().toDoubleOrNull() ?: 0.0
            val right = if (i + 1 < processedTokens.size) processedTokens[i+1].toDoubleOrNull() ?: 0.0 else 0.0
            val res = if (t == "*") left * right else (if (right != 0.0) left / right else 0.0)
            multDivTokens.add(res.toString())
            i++ 
        } else {
            multDivTokens.add(t)
        }
        i++
    }
    
    var result = multDivTokens.firstOrNull()?.toDoubleOrNull() ?: 0.0
    i = 1
    while (i < multDivTokens.size - 1) {
        val op = multDivTokens[i]
        val right = multDivTokens[i+1].toDoubleOrNull() ?: 0.0
        if (op == "+") result += right
        if (op == "-") result -= right
        i += 2
    }
    
    return result
}
