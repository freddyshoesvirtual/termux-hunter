package com.termuxhunter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.termuxhunter.AppViewModel
import com.termuxhunter.RootState
import com.termuxhunter.bridge.CommandResult
import com.termuxhunter.tools.Tool
import com.termuxhunter.tools.ToolField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(vm: AppViewModel, tool: Tool, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val values = remember(tool.id) { mutableStateMapOf<String, Any>().apply { putAll(tool.defaultValues()) } }
    var override by remember(tool.id) { mutableStateOf<String?>(null) }
    var asRoot by remember(tool.id) { mutableStateOf(tool.requiresRoot || (vm.preferRoot && vm.rootAvailable)) }
    var result by remember(tool.id) { mutableStateOf<CommandResult?>(null) }
    var running by remember(tool.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val command = override ?: tool.buildCommand(values)
    val missing = if (override == null) tool.missingRequired(values) else emptyList()
    val canRun = vm.ready && missing.isEmpty() && command.isNotBlank()
    fun set(key: String, value: Any) {
        values[key] = value
        override = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(tool.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(tool.description, style = MaterialTheme.typography.bodyMedium)
            if (tool.requiresRoot && vm.rootState == RootState.UNAVAILABLE) {
                Text("Esta herramienta necesita root y no se detectó acceso root.", color = HunterAmber)
            }

            tool.fields.forEach { field ->
                when (field) {
                    is ToolField.Flag -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = values[field.key] as? Boolean == true, onCheckedChange = { set(field.key, it) })
                        Text(field.label)
                    }
                    is ToolField.Text -> OutlinedTextField(
                        value = values[field.key] as? String ?: "",
                        onValueChange = { set(field.key, it) },
                        label = { Text(if (field.required) "${field.label} *" else field.label) },
                        placeholder = { if (field.hint.isNotEmpty()) Text(field.hint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is ToolField.Choice -> ChoiceField(field, values[field.key] as? Int ?: field.defaultIndex) { set(field.key, it) }
                }
            }

            OutlinedTextField(
                value = command,
                onValueChange = { override = it },
                label = { Text("Comando (editable)") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (missing.isNotEmpty()) {
                Text("Falta: ${missing.joinToString()}", color = HunterRed, style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ejecutar como root")
                    Text(
                        when (vm.rootState) {
                            RootState.AVAILABLE -> "Root disponible"
                            RootState.UNAVAILABLE -> "Root no disponible"
                            RootState.CHECKING -> "Comprobando root…"
                            RootState.UNKNOWN -> "Root sin comprobar (Ajustes)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = asRoot, onCheckedChange = { asRoot = it }, enabled = vm.rootState != RootState.UNAVAILABLE || asRoot)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.runInTerminal(command, asRoot, tool.name) },
                    enabled = canRun,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Terminal, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir en Termux")
                }
                OutlinedButton(
                    onClick = {
                        running = true
                        scope.launch {
                            result = vm.runInBackground(command, asRoot, tool.name)
                            running = false
                        }
                    },
                    enabled = canRun && !running && !tool.interactive,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ejecutar aquí")
                }
            }
            Text(
                if (tool.interactive) {
                    "Herramienta interactiva: ábrela en Termux."
                } else {
                    "\"Ejecutar aquí\" muestra la salida al terminar. Para procesos largos o interactivos usa \"Abrir en Termux\"."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            CommandOutput(result, running)
        }
    }
}

@Composable
private fun ChoiceField(field: ToolField.Choice, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(field.label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(field.options.getOrNull(selected)?.first.orEmpty(), modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                field.options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option.first) }, onClick = {
                        onSelect(index)
                        expanded = false
                    })
                }
            }
        }
    }
}
