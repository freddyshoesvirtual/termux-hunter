package com.termuxhunter.tools

import com.termuxhunter.bridge.TermuxBridge

sealed interface ToolField {
    val key: String
    val label: String

    /** Boolean switch that adds [arg] when enabled. */
    data class Flag(
        override val key: String,
        override val label: String,
        val arg: String,
        val default: Boolean = false,
    ) : ToolField

    /** Free text. If [arg] is null the value is passed as a positional argument. */
    data class Text(
        override val key: String,
        override val label: String,
        val arg: String? = null,
        val hint: String = "",
        val default: String = "",
        val required: Boolean = false,
        val raw: Boolean = false,
    ) : ToolField

    /** Single choice among options; each option maps to its arguments (may be empty). */
    data class Choice(
        override val key: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        val defaultIndex: Int = 0,
    ) : ToolField
}

enum class Category(val title: String) {
    RECON("Reconocimiento"),
    WEB("Web"),
    PASSWORDS("Contraseñas"),
    NETWORK("Red"),
    WIRELESS("Inalámbrico"),
    EXPLOIT("Explotación"),
    UTILS("Utilidades"),
    OTHER("Otras"),
}

data class Tool(
    val id: String,
    val name: String,
    val binary: String,
    val description: String,
    val category: Category,
    val installCommand: String?,
    val requiresRoot: Boolean = false,
    val interactive: Boolean = false,
    val fields: List<ToolField> = emptyList(),
) {
    fun defaultValues(): Map<String, Any> = fields.associate { field ->
        field.key to when (field) {
            is ToolField.Flag -> field.default
            is ToolField.Text -> field.default
            is ToolField.Choice -> field.defaultIndex
        }
    }

    fun buildCommand(values: Map<String, Any>): String {
        val parts = mutableListOf(binary)
        val positional = mutableListOf<String>()
        for (field in fields) {
            when (field) {
                is ToolField.Flag -> if (values[field.key] as? Boolean == true) parts += field.arg
                is ToolField.Choice -> {
                    val index = values[field.key] as? Int ?: field.defaultIndex
                    val arg = field.options.getOrNull(index)?.second.orEmpty()
                    if (arg.isNotBlank()) parts += arg
                }
                is ToolField.Text -> {
                    val value = (values[field.key] as? String).orEmpty().trim()
                    if (value.isEmpty()) continue
                    val rendered = if (field.raw) value else TermuxBridge.shellQuote(value)
                    if (field.arg == null) positional += rendered else parts += "${field.arg} $rendered"
                }
            }
        }
        return (parts + positional).joinToString(" ")
    }

    fun missingRequired(values: Map<String, Any>): List<String> = fields
        .filterIsInstance<ToolField.Text>()
        .filter { it.required && (values[it.key] as? String).isNullOrBlank() }
        .map { it.label }

    companion object {
        /** Generic form for any installed binary that is not in the catalog. */
        fun generic(binary: String): Tool = Tool(
            id = "bin:$binary",
            name = binary,
            binary = binary,
            description = "Herramienta detectada en Termux. Escribe los argumentos que quieras pasarle.",
            category = Category.OTHER,
            installCommand = null,
            fields = listOf(
                ToolField.Text("args", "Argumentos", hint = "--help", raw = true),
            ),
        )
    }
}
