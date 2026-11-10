package com.termuxhunter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.termuxhunter.AppViewModel
import com.termuxhunter.RootState
import com.termuxhunter.bridge.CommandResult
import com.termuxhunter.data.CustomCommand
import com.termuxhunter.data.RunMode
import kotlinx.coroutines.launch

@Composable
fun CommandsScreen(vm: AppViewModel, modifier: Modifier) {
    var editing by remember { mutableStateOf<CustomCommand?>(null) }
    var outputFor by remember { mutableStateOf<CustomCommand?>(null) }
    var result by remember { mutableStateOf<CommandResult?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run(cmd: CustomCommand) {
        if (cmd.mode == RunMode.TERMINAL) {
            vm.runInTerminal(cmd.command, cmd.asRoot, cmd.name)
            return
        }
        outputFor = cmd
        result = null
        running = true
        scope.launch {
            result = vm.runInBackground(cmd.command, cmd.asRoot, cmd.name)
            running = false
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("Comandos personalizados", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = HunterGreen)
                    Text("Toca para ejecutar, usa el lápiz para editar.", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(vm.customCommands, key = { it.id }) { cmd ->
                Card(
                    onClick = { run(cmd) },
                    enabled = vm.ready,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cmd.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2)
                            IconButton(onClick = { editing = cmd }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                        }
                        Text(
                            cmd.command,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.padding(top = 6.dp))
                        Row {
                            Badge(if (cmd.mode == RunMode.TERMINAL) "TERMINAL" else "EN APP", MaterialTheme.colorScheme.secondary)
                            if (cmd.asRoot) {
                                Spacer(Modifier.width(4.dp))
                                Badge("ROOT", HunterAmber)
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = CustomCommand(name = "", command = "") },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Nuevo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editing?.let { cmd ->
        CommandEditor(
            initial = cmd,
            isNew = vm.customCommands.none { it.id == cmd.id },
            rootAllowed = vm.rootState != RootState.UNAVAILABLE,
            onSave = { vm.saveCommand(it); editing = null },
            onDelete = { vm.deleteCommand(cmd); editing = null },
            onDismiss = { editing = null },
        )
    }

    outputFor?.let { cmd ->
        AlertDialog(
            onDismissRequest = { if (!running) outputFor = null },
            title = { Text(cmd.name) },
            text = { CommandOutput(result, running) },
            confirmButton = { TextButton(onClick = { outputFor = null }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun CommandEditor(
    initial: CustomCommand,
    isNew: Boolean,
    rootAllowed: Boolean,
    onSave: (CustomCommand) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var command by remember { mutableStateOf(initial.command) }
    var asRoot by remember { mutableStateOf(initial.asRoot) }
    var mode by remember { mutableStateOf(initial.mode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nuevo comando" else "Editar comando") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre del botón") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    command, { command = it },
                    label = { Text("Comando") },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Dónde ejecutar", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == RunMode.TERMINAL, onClick = { mode = RunMode.TERMINAL }, label = { Text("Terminal Termux") })
                    FilterChip(selected = mode == RunMode.BACKGROUND, onClick = { mode = RunMode.BACKGROUND }, label = { Text("En la app") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ejecutar como root", modifier = Modifier.weight(1f))
                    Switch(checked = asRoot, onCheckedChange = { asRoot = it }, enabled = rootAllowed || asRoot)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(initial.copy(name = name.trim(), command = command.trim(), asRoot = asRoot, mode = mode)) },
                enabled = name.isNotBlank() && command.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text("Borrar", color = HunterRed) }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}
