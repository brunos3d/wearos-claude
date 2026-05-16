package com.brunos3d.wearosclaude.ui

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender

/**
 * Wear OS pattern for text input on a watch: a `RemoteInput` activity bound to a
 * single named field. The user dictates or types, the IME activity returns,
 * we read the string out of the result intent.
 *
 * Used by the Settings screen to edit the backend URL and the optional shared
 * secret without needing a phone companion.
 */
class WearTextInputContract(
    private val label: String,
    private val initialValue: String,
) : ActivityResultContract<Unit, String?>() {

    private val key = "wearos_claude_text"

    override fun createIntent(context: android.content.Context, input: Unit): Intent {
        val remoteInputs = listOf(
            RemoteInput.Builder(key)
                .setLabel(label)
                .wearableExtender {
                    setEmojisAllowed(false)
                    setInputActionType(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
                }
                .build(),
        )
        return RemoteInputIntentHelper.createActionRemoteInputIntent()
            .let { intent ->
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                if (initialValue.isNotEmpty()) {
                    val bundle = android.os.Bundle().apply { putCharSequence(key, initialValue) }
                    intent.putExtra(android.app.RemoteInput.EXTRA_RESULTS_DATA, bundle)
                }
                intent
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        val bundle = RemoteInput.getResultsFromIntent(intent) ?: return null
        return bundle.getCharSequence(key)?.toString()
    }
}

@Composable
fun rememberRemoteInputLauncher(
    label: String,
    initial: String,
    onResult: (String?) -> Unit,
): ActivityResultLauncher<Unit> {
    val contract = remember(label, initial) { WearTextInputContract(label, initial) }
    return rememberLauncherForActivityResult(contract) { onResult(it) }
}
