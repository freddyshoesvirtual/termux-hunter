package com.termuxhunter.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(TermuxBridge.EXTRA_EXECUTION_ID, -1)
        val bundle = intent.getBundleExtra(TermuxBridge.RESULT_BUNDLE) ?: return
        val exitCode = if (bundle.containsKey(TermuxBridge.RESULT_EXIT_CODE)) {
            bundle.getInt(TermuxBridge.RESULT_EXIT_CODE)
        } else {
            null
        }
        TermuxBridge.deliver(
            id,
            CommandResult(
                stdout = bundle.getString(TermuxBridge.RESULT_STDOUT, ""),
                stderr = bundle.getString(TermuxBridge.RESULT_STDERR, ""),
                exitCode = exitCode,
                errCode = bundle.getInt(TermuxBridge.RESULT_ERR, -1),
                errMsg = bundle.getString(TermuxBridge.RESULT_ERRMSG, ""),
            ),
        )
    }
}
