package com.termuxhunter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.termuxhunter.AppViewModel
import com.termuxhunter.tools.Tool

@Composable
fun ToolsScreen(vm: AppViewModel, modifier: Modifier, onOpenTool: (Tool) -> Unit, onGoSettings: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var showOthers by rememberSaveable { mutableStateOf(false) }
    val q = query.trim().lowercase()
    fun Tool.matches() = q.isEmpty() || name.lowercase().contains(q) || binary.contains(q) || description.lowercase().contains(q)

    val installed = vm.installedTools.filter { it.matches() }
    val missing = vm.missingTools.filter { it.matches() }
    val others = vm.otherBinaries.filter { q.isEmpty() || it.contains(q) }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Termux Hunter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = HunterGreen)
                    val count = vm.installedBinaries?.size
                    Text(
                        when {
                            vm.scanning -> "Detectando herramientas…"
                            count != null -> "${vm.installedTools.size} herramientas reconocidas · $count binarios en Termux"
                            else -> "Herramientas sin detectar"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (vm.scanning) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                } else {
                    IconButton(onClick = vm::scanTools, enabled = vm.ready) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Escanear")
                    }
                }
            }
        }

        val problem = when {
            !vm.termuxInstalled -> "Termux no está instalado."
            !vm.permissionGranted -> "Falta el permiso para ejecutar comandos en Termux."
            vm.scanError != null -> vm.scanError
            else -> null
        }
        if (problem != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = HunterRed.copy(alpha = 0.12f))) {
                    Column(Modifier.padding(14.dp)) {
                        Text(problem, color = HunterRed, fontWeight = FontWeight.SemiBold)
                        Text("Completa la configuración en Ajustes.", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = onGoSettings) { Text("Ir a Ajustes") }
                            if (vm.ready) TextButton(onClick = vm::scanTools) { Text("Reintentar") }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Buscar herramienta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (installed.isNotEmpty()) {
            item { SectionTitle("Instaladas") }
            installed.groupBy { it.category }.toSortedMap().forEach { (category, tools) ->
                item { Text(category.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary) }
                items(tools, key = { it.id }) { tool -> ToolCard(tool, installed = true, onClick = { onOpenTool(tool) }) }
            }
        }

        if (others.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showOthers = !showOthers }.padding(vertical = 4.dp),
                ) {
                    SectionTitle("Otros programas en Termux (${others.size})", Modifier.weight(1f))
                    Icon(if (showOthers) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
            }
            if (showOthers || q.isNotEmpty()) {
                items(others, key = { "bin:$it" }) { bin ->
                    Text(
                        bin,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenTool(Tool.generic(bin)) }.padding(vertical = 8.dp, horizontal = 4.dp),
                    )
                }
            }
        }

        if (missing.isNotEmpty()) {
            item { SectionTitle("No instaladas") }
            items(missing, key = { "missing:" + it.id }) { tool ->
                ToolCard(tool, installed = false, onClick = { onOpenTool(tool) }, onInstall = if (tool.installCommand != null && vm.ready) ({ vm.install(tool) }) else null)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = modifier.padding(top = 8.dp))
}

@Composable
private fun ToolCard(tool: Tool, installed: Boolean, onClick: () -> Unit, onInstall: (() -> Unit)? = null) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tool.name, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    if (tool.requiresRoot) Badge("ROOT", HunterAmber)
                    if (installed) {
                        Spacer(Modifier.width(4.dp))
                        Badge("OK", HunterGreen)
                    }
                }
                Text(tool.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            if (!installed) {
                if (onInstall != null) {
                    FilledTonalButton(onClick = onInstall) {
                        Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Instalar")
                    }
                } else if (tool.installCommand == null) {
                    Badge("MANUAL", MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
