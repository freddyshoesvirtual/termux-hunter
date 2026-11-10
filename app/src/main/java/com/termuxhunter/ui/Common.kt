package com.termuxhunter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termuxhunter.bridge.CommandResult

val HunterGreen = Color(0xFF2ECC71)
val HunterRed = Color(0xFFE74C3C)
val HunterAmber = Color(0xFFF1C40F)

private val colors = darkColorScheme(
    primary = HunterGreen,
    onPrimary = Color(0xFF00210E),
    primaryContainer = Color(0xFF14361F),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFF58A6FF),
    background = Color(0xFF0D1117),
    surface = Color(0xFF0D1117),
    surfaceVariant = Color(0xFF161B22),
    surfaceContainer = Color(0xFF161B22),
    surfaceContainerHigh = Color(0xFF1C2230),
    error = HunterRed,
)

@Composable
fun HunterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun Badge(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun CommandOutput(result: CommandResult?, running: Boolean, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val text = when {
        running -> "Ejecutando…"
        result == null -> ""
        else -> buildString {
            if (result.stdout.isNotEmpty()) append(result.stdout)
            if (result.stderr.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(result.stderr)
            }
            if (result.errCode != -1 && result.errMsg.isNotBlank() && result.errMsg != result.stderr) {
                if (isNotEmpty()) append("\n")
                append("[Termux] ").append(result.errMsg.trim())
            }
            if (isEmpty()) append("(sin salida)")
        }
    }
    if (text.isEmpty()) return
    Surface(color = Color(0xFF010409), shape = RoundedCornerShape(10.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val code = result?.exitCode
                when {
                    running -> Badge("EN CURSO", HunterAmber)
                    code == 0 -> Badge("SALIDA 0", HunterGreen)
                    code != null -> Badge("SALIDA $code", HunterRed)
                    else -> Badge("ERROR", HunterRed)
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, enabled = !running) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar salida")
                }
            }
            SelectionContainer {
                Text(
                    text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFC9D1D9),
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}
