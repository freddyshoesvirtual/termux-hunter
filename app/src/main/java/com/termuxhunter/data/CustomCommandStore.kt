package com.termuxhunter.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

enum class RunMode { TERMINAL, BACKGROUND }

@Serializable
data class CustomCommand(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val command: String,
    val asRoot: Boolean = false,
    val mode: RunMode = RunMode.TERMINAL,
)

class CustomCommandStore(context: Context) {
    private val prefs = context.getSharedPreferences("custom_commands", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<CustomCommand> {
        val raw = prefs.getString(KEY, null) ?: return defaults().also(::save)
        return runCatching { json.decodeFromString<List<CustomCommand>>(raw) }.getOrElse { defaults() }
    }

    fun save(commands: List<CustomCommand>) {
        prefs.edit().putString(KEY, json.encodeToString(commands)).apply()
    }

    private fun defaults() = listOf(
        CustomCommand(name = "IP pública", command = "curl -s https://ifconfig.me; echo", mode = RunMode.BACKGROUND),
        CustomCommand(name = "Interfaces de red", command = "ip -brief addr 2>/dev/null || ifconfig", mode = RunMode.BACKGROUND),
        CustomCommand(name = "Actualizar Termux", command = "pkg update -y && pkg upgrade -y"),
        CustomCommand(name = "Shell root", command = "bash", asRoot = true),
    )

    private companion object {
        const val KEY = "commands_v1"
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var preferRoot: Boolean
        get() = prefs.getBoolean("prefer_root", false)
        set(value) = prefs.edit().putBoolean("prefer_root", value).apply()
}
