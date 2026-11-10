package com.termuxhunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.termuxhunter.bridge.TermuxBridge
import com.termuxhunter.tools.Tool
import com.termuxhunter.ui.CommandsScreen
import com.termuxhunter.ui.HunterTheme
import com.termuxhunter.ui.SettingsScreen
import com.termuxhunter.ui.ToolDetailScreen
import com.termuxhunter.ui.ToolsScreen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HunterTheme {
                HunterApp(vm) { permissionLauncher.launch(TermuxBridge.PERMISSION_RUN_COMMAND) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshStatus()
    }
}

private enum class Tab(val title: String) { TOOLS("Herramientas"), COMMANDS("Comandos"), SETTINGS("Ajustes") }

@Composable
private fun HunterApp(vm: AppViewModel, onRequestPermission: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openTool by remember { mutableStateOf<Tool?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    val tool = openTool
    if (tool != null) {
        BackHandler { openTool = null }
        ToolDetailScreen(vm, tool, snackbar) { openTool = null }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.TOOLS -> Icons.Filled.Build
                                    Tab.COMMANDS -> Icons.Filled.Terminal
                                    Tab.SETTINGS -> Icons.Filled.Settings
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(t.title) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (Tab.entries[tab]) {
            Tab.TOOLS -> ToolsScreen(vm, modifier, onOpenTool = { openTool = it }, onGoSettings = { tab = Tab.SETTINGS.ordinal })
            Tab.COMMANDS -> CommandsScreen(vm, modifier)
            Tab.SETTINGS -> SettingsScreen(vm, modifier, onRequestPermission)
        }
    }
}
