package com.termuxhunter.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termuxhunter.AppViewModel
import com.termuxhunter.BuildConfig
import com.termuxhunter.RootState
import com.termuxhunter.bridge.CommandResult
import com.termuxhunter.bridge.TermuxBridge
import kotlinx.coroutines.launch

private const val ENABLE_EXTERNAL_APPS =
    "mkdir -p ~/.termux && (grep -qx 'allow-external-apps=true' ~/.termux/termux.properties 2>/dev/null || " +
        "echo 'allow-external-apps=true' >> ~/.termux/termux.properties) && termux-reload-settings"

@Composable
fun SettingsScreen(vm: AppViewModel, modifier: Modifier, onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf<CommandResult?>(null) }
    var testing by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = HunterGreen)

        Step("1. Termux instalado", vm.termuxInstalled) {
            if (vm.termuxInstalled) {
                OutlinedButton(onClick = { TermuxBridge.openTermux(context) }) { Text("Abrir Termux") }
            } else {
                Text("Instala Termux desde F-Droid o GitHub y ábrelo una vez para que termine de instalarse.", style = MaterialTheme.typography.bodySmall)
                FilledTonalButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/")))
                }) { Text("Descargar Termux") }
            }
        }

        Step("2. Permiso para ejecutar comandos", vm.permissionGranted) {
            if (!vm.permissionGranted) {
                Text("Termux Hunter necesita el permiso \"Ejecutar comandos en Termux\".", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onRequestPermission, enabled = vm.termuxInstalled) { Text("Conceder") }
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }) { Text("Ajustes de la app") }
                }
            }
        }

        val connected = testResult?.ok
        Step("3. Permitir apps externas en Termux", connected) {
            Text("Copia este comando, pégalo en Termux y pulsa Enter:", style = MaterialTheme.typography.bodySmall)
            CodeBox(ENABLE_EXTERNAL_APPS)
            Text(
                "Si \"Abrir en Termux\" no abre la terminal, dale a Termux el permiso \"Mostrar sobre otras apps\" en los ajustes de Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { TermuxBridge.openTermux(context) }, enabled = vm.termuxInstalled) { Text("Abrir Termux") }
                FilledTonalButton(
                    onClick = {
                        testing = true
                        scope.launch {
                            testResult = vm.testConnection()
                            testing = false
                            if (testResult?.ok == true) vm.scanTools()
                        }
                    },
                    enabled = vm.ready && !testing,
                ) { Text("Probar conexión") }
                if (testing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            testResult?.let { CommandOutput(it, running = false) }
        }

        Step(
            "4. Root (opcional)",
            when (vm.rootState) {
                RootState.AVAILABLE -> true
                RootState.UNAVAILABLE -> false
                else -> null
            },
        ) {
            Text(
                "El acceso root se pide a través de Termux: Magisk/KernelSU mostrará la solicitud para Termux, no para esta app.",
                style = MaterialTheme.typography.bodySmall,
            )
            vm.rootDetail?.let { Text(it, color = HunterAmber, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = vm::checkRoot, enabled = vm.ready && vm.rootState != RootState.CHECKING) { Text("Detectar root") }
                if (vm.rootState == RootState.CHECKING) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Usar root por defecto en herramientas", modifier = Modifier.weight(1f))
                Switch(checked = vm.preferRoot, onCheckedChange = vm::updatePreferRoot, enabled = vm.rootAvailable)
            }
        }

        Text(
            "Termux Hunter ${BuildConfig.VERSION_NAME} · Úsalo solo en redes y equipos para los que tengas autorización.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun Step(title: String, done: Boolean?, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (done) {
                        true -> Icons.Filled.CheckCircle
                        false -> Icons.Filled.Cancel
                        null -> Icons.Filled.Help
                    },
                    contentDescription = null,
                    tint = when (done) {
                        true -> HunterGreen
                        false -> HunterRed
                        null -> MaterialTheme.colorScheme.outline
                    },
                )
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun CodeBox(code: String) {
    val clipboard = LocalClipboardManager.current
    Surface(color = Color(0xFF010409), shape = RoundedCornerShape(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp)) {
            SelectionContainer(Modifier.weight(1f)) {
                Text(code, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFC9D1D9))
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar")
            }
        }
    }
}
