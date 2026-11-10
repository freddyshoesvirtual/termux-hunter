package com.termuxhunter.bridge

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class CommandResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int?,
    val errCode: Int,
    val errMsg: String,
) {
    val ok: Boolean get() = errCode == -1 && exitCode == 0
}

class TermuxException(message: String) : Exception(message)

object TermuxBridge {
    const val TERMUX_PACKAGE = "com.termux"
    const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"
    const val PREFIX = "/data/data/com.termux/files/usr"
    const val HOME = "/data/data/com.termux/files/home"
    const val BASH = "$PREFIX/bin/bash"

    private const val SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
    private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
    private const val SESSION_ACTION_SWITCH_AND_OPEN = "0"

    internal const val EXTRA_EXECUTION_ID = "com.termuxhunter.EXECUTION_ID"
    internal const val RESULT_BUNDLE = "result"
    internal const val RESULT_STDOUT = "stdout"
    internal const val RESULT_STDERR = "stderr"
    internal const val RESULT_EXIT_CODE = "exitCode"
    internal const val RESULT_ERR = "err"
    internal const val RESULT_ERRMSG = "errmsg"

    private val nextId = AtomicInteger(1000)
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<CommandResult>>()

    private const val FIND_SU =
        "SU=\"\"; for p in /debug_ramdisk/su /sbin/su /system/xbin/su /system/bin/su /su/bin/su /system/sbin/su \$(command -v su 2>/dev/null); do " +
            "if [ -z \"\$SU\" ] && [ -x \"\$p\" ]; then SU=\"\$p\"; fi; done; " +
            "if [ -z \"\$SU\" ]; then echo 'Root no disponible: no se encontró su' >&2; exit 127; fi"

    fun isTermuxInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun hasRunCommandPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION_RUN_COMMAND) == PackageManager.PERMISSION_GRANTED

    fun openTermux(context: Context) {
        context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    /** Wraps [command] so it runs as root through su, keeping Termux binaries in PATH. */
    fun wrapRoot(command: String): String =
        "$FIND_SU; exec \"\$SU\" -c \"export PATH=\\\"\$PATH:/system/bin:/system/xbin\\\"; ${escapeDoubleQuoted(command)}\""

    fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun escapeDoubleQuoted(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("`", "\\`")

    private fun baseIntent(command: String, asRoot: Boolean, label: String?): Intent {
        val script = if (asRoot) wrapRoot(command) else command
        return Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE, SERVICE)
            putExtra(EXTRA_COMMAND_PATH, BASH)
            putExtra(EXTRA_ARGUMENTS, arrayOf("-c", script))
            putExtra(EXTRA_WORKDIR, HOME)
            label?.let { putExtra(EXTRA_COMMAND_LABEL, it) }
        }
    }

    private fun checkReady(context: Context) {
        if (!isTermuxInstalled(context)) throw TermuxException("Termux no está instalado")
        if (!hasRunCommandPermission(context)) {
            throw TermuxException("Falta el permiso para ejecutar comandos en Termux (ve a Ajustes)")
        }
    }

    private fun send(context: Context, intent: Intent) {
        try {
            context.startService(intent)
        } catch (e: IllegalStateException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                throw e
            }
        } catch (e: SecurityException) {
            throw TermuxException("Termux rechazó el comando: ${e.message}")
        }
    }

    /** Opens a new visible Termux session running [command]. */
    fun runInTerminal(context: Context, command: String, asRoot: Boolean, label: String? = null) {
        checkReady(context)
        val intent = baseIntent(command, asRoot, label).apply {
            putExtra(EXTRA_BACKGROUND, false)
            putExtra(EXTRA_SESSION_ACTION, SESSION_ACTION_SWITCH_AND_OPEN)
        }
        send(context, intent)
        // Android 10+ blocks Termux from opening its own activity from the background
        // unless it has "display over other apps", so bring it to front from here.
        openTermux(context)
    }

    /** Runs [command] in the background inside Termux and returns its output. */
    suspend fun runInBackground(
        context: Context,
        command: String,
        asRoot: Boolean,
        label: String? = null,
    ): CommandResult {
        checkReady(context)
        val id = nextId.incrementAndGet()
        val deferred = CompletableDeferred<CommandResult>()
        pending[id] = deferred

        val resultIntent = Intent(context, TermuxResultReceiver::class.java)
            .putExtra(EXTRA_EXECUTION_ID, id)
        val flags = PendingIntent.FLAG_ONE_SHOT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val pendingIntent = PendingIntent.getBroadcast(context, id, resultIntent, flags)

        val intent = baseIntent(command, asRoot, label).apply {
            putExtra(EXTRA_BACKGROUND, true)
            putExtra(EXTRA_PENDING_INTENT, pendingIntent)
        }
        try {
            send(context, intent)
            return deferred.await()
        } finally {
            pending.remove(id)
        }
    }

    internal fun deliver(id: Int, result: CommandResult) {
        pending.remove(id)?.complete(result)
    }
}
