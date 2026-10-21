package com.example.migo.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.migo.data.local.MigoPreferences
import com.example.migo.notifications.DailyReminder

@Composable
internal fun ReminderSettingsCard() {
    val context = LocalContext.current.applicationContext
    val preferences = remember { MigoPreferences(context) }
    var enabled by remember {
        mutableStateOf(preferences.isReminderEnabled() && DailyReminder.canPostNotifications(context))
    }
    var permissionDenied by remember { mutableStateOf(false) }

    fun apply(on: Boolean) {
        preferences.setReminderEnabled(on)
        if (on) DailyReminder.schedule(context) else DailyReminder.cancel(context)
        enabled = on
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) apply(true)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Günlük hatırlatma", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (permissionDenied) {
                        "Bildirim izni verilmedi. Telefonun ayarlarından izin verebilirsin."
                    } else {
                        "Her akşam ${DailyReminder.REMINDER_HOUR}:00'da bugünün adımını hatırlatır. Bildirim yalnızca cihazında oluşturulur."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { wantsOn ->
                    permissionDenied = false
                    when {
                        !wantsOn -> apply(false)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !DailyReminder.canPostNotifications(context) ->
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> apply(true)
                    }
                }
            )
        }
    }
}
