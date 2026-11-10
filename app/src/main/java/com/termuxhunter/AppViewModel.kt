package com.termuxhunter

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termuxhunter.bridge.CommandResult
import com.termuxhunter.bridge.TermuxBridge
import com.termuxhunter.data.CustomCommand
import com.termuxhunter.data.CustomCommandStore
import com.termuxhunter.data.SettingsStore
import com.termuxhunter.tools.Tool
import com.termuxhunter.tools.ToolCatalog
import kotlinx.coroutines.launch

enum class RootState { UNKNOWN, CHECKING, AVAILABLE, UNAVAILABLE }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    private val commandStore = CustomCommandStore(app)
    private val settings = SettingsStore(app)

    var termuxInstalled by mutableStateOf(false)
        private set
    var permissionGranted by mutableStateOf(false)
        private set
    var installedBinaries by mutableStateOf<Set<String>?>(null)
        private set
    var scanning by mutableStateOf(false)
        private set
    var scanError by mutableStateOf<String?>(null)
        private set
    var rootState by mutableStateOf(RootState.UNKNOWN)
        private set
    var rootDetail by mutableStateOf<String?>(null)
        private set
    var preferRoot by mutableStateOf(settings.preferRoot)
        private set
    var customCommands by mutableStateOf(commandStore.load())
        private set
    var message by mutableStateOf<String?>(null)

    val ready: Boolean get() = termuxInstalled && permissionGranted
    val rootAvailable: Boolean get() = rootState == RootState.AVAILABLE

    val installedTools: List<Tool>
        get() = installedBinaries?.let { bins -> ToolCatalog.tools.filter { it.binary in bins } }.orEmpty()

    val missingTools: List<Tool>
        get() = installedBinaries?.let { bins -> ToolCatalog.tools.filter { it.binary !in bins } } ?: ToolCatalog.tools

    val otherBinaries: List<String>
        get() {
            val known = ToolCatalog.tools.map { it.binary }.toSet()
            return installedBinaries.orEmpty().filter { it !in known }.sorted()
        }

    fun refreshStatus() {
        termuxInstalled = TermuxBridge.isTermuxInstalled(context)
        permissionGranted = TermuxBridge.hasRunCommandPermission(context)
        if (ready && installedBinaries == null && !scanning) scanTools()
    }

    fun scanTools() {
        if (!ready) return
        scanning = true
        scanError = null
        viewModelScope.launch {
            val result = runCatching {
                TermuxBridge.runInBackground(context, LIST_BINARIES, asRoot = false, label = "Detectar herramientas")
            }
            scanning = false
            result.onSuccess { r ->
                if (r.errCode != -1 || r.exitCode == null) {
                    scanError = describeError(r)
                } else {
                    installedBinaries = r.stdout.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                }
            }.onFailure { scanError = it.message }
        }
    }

    fun checkRoot() {
        if (!ready) return
        rootState = RootState.CHECKING
        rootDetail = null
        viewModelScope.launch {
            val result = runCatching { TermuxBridge.runInBackground(context, "id -u", asRoot = true, label = "Comprobar root") }
            result.onSuccess { r ->
                if (r.stdout.trim() == "0") {
                    rootState = RootState.AVAILABLE
                } else {
                    rootState = RootState.UNAVAILABLE
                    rootDetail = (r.stderr.ifBlank { r.errMsg }).trim().ifBlank { "su no concedió acceso root" }
                    if (preferRoot) updatePreferRoot(false)
                }
            }.onFailure {
                rootState = RootState.UNAVAILABLE
                rootDetail = it.message
            }
        }
    }

    fun updatePreferRoot(value: Boolean) {
        preferRoot = value
        settings.preferRoot = value
    }

    fun runInTerminal(command: String, asRoot: Boolean, label: String? = null) {
        runCatching { TermuxBridge.runInTerminal(context, command, asRoot, label) }
            .onFailure { message = it.message }
    }

    suspend fun runInBackground(command: String, asRoot: Boolean, label: String? = null): CommandResult =
        try {
            TermuxBridge.runInBackground(context, command, asRoot, label)
        } catch (e: Exception) {
            CommandResult("", e.message.orEmpty(), null, 1, e.message.orEmpty())
        }

    suspend fun testConnection(): CommandResult = runInBackground("echo Termux OK; uname -a", asRoot = false)

    fun install(tool: Tool) {
        val cmd = tool.installCommand ?: return
        runInTerminal("$cmd && echo && echo '✔ ${tool.name} instalado. Vuelve a Termux Hunter y pulsa Escanear.'", asRoot = false, label = "Instalar ${tool.name}")
        installedBinaries = null
    }

    fun saveCommand(command: CustomCommand) {
        val list = customCommands.toMutableList()
        val index = list.indexOfFirst { it.id == command.id }
        if (index >= 0) list[index] = command else list += command
        customCommands = list
        commandStore.save(list)
    }

    fun deleteCommand(command: CustomCommand) {
        customCommands = customCommands.filter { it.id != command.id }
        commandStore.save(customCommands)
    }

    companion object {
        private const val LIST_BINARIES =
            "for f in ${TermuxBridge.PREFIX}/bin/*; do [ -f \"\$f\" ] && [ -x \"\$f\" ] && echo \"\${f##*/}\"; done; true"

        fun describeError(r: CommandResult): String = when {
            r.errMsg.contains("allow-external-apps", ignoreCase = true) ->
                "Termux bloquea apps externas. Activa allow-external-apps (ve a Ajustes)."
            r.errMsg.isNotBlank() -> r.errMsg.trim()
            r.stderr.isNotBlank() -> r.stderr.trim()
            else -> "Error desconocido (código ${r.errCode})"
        }
    }
}
