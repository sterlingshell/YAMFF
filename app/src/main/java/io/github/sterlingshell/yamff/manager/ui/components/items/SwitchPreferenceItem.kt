package io.github.sterlingshell.yamff.manager.ui.components.items

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable

@Composable
fun SwitchPreferenceItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    PreferenceItem(
        title = title,
        summary = summary,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
