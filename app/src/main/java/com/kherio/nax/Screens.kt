package com.kherio.nax

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Resumen

@Composable
fun SummaryScreen(vm: MainViewModel) {
    val t = vm.totals
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (vm.error != null) ErrorCard(vm.error!!)
        if (t == null) {
            if (vm.error == null) Text("Sin datos todavía. Pulsa actualizar.")
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Dispositivos", t.devices, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("Conectados", t.connected, OnlineColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Caídos", t.down, DownColor, Modifier.weight(1f))
            StatCard("Nuevos", t.newDevices, NewColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Favoritos", t.favorites, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            StatCard("Archivados", t.archived, OfflineColor, Modifier.weight(1f))
        }
        if (vm.lastUpdate.isNotBlank()) {
            Text(
                "Última actualización: ${vm.lastUpdate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text(value.toString(), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(msg: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(msg, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

// ----------------------------------------------------------- Dispositivos

private enum class DevFilter(val label: String) {
    ALL("Todos"), ONLINE("Online"), OFFLINE("Offline"), DOWN("Caídos"), NEW("Nuevos"), FAV("Favoritos")
}

@Composable
fun DevicesScreen(vm: MainViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(DevFilter.ALL) }

    val q = query.trim().lowercase()
    val list = vm.devices
        .filter {
            when (filter) {
                DevFilter.ALL -> it.status != Status.ARCHIVED
                DevFilter.ONLINE -> it.status == Status.ONLINE
                DevFilter.OFFLINE -> it.status == Status.OFFLINE
                DevFilter.DOWN -> it.status == Status.DOWN
                DevFilter.NEW -> it.isNew
                DevFilter.FAV -> it.favorite
            }
        }
        .filter {
            q.isEmpty() || it.title.lowercase().contains(q) || it.ip.contains(q) ||
                it.mac.lowercase().contains(q) || it.vendor.lowercase().contains(q)
        }
        .sortedWith(compareBy({ it.status.ordinal }, { it.title.lowercase() }))

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar nombre, IP, MAC o fabricante") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DevFilter.values().forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }
        if (vm.error != null) Box(Modifier.padding(horizontal = 16.dp)) { ErrorCard(vm.error!!) }
        if (list.isEmpty() && vm.error == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay dispositivos que mostrar.")
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(list) { DeviceRow(it) }
        }
    }
}

@Composable
private fun DeviceRow(d: Device) {
    val color = statusColor(d.status)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (d.favorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Star, contentDescription = "Favorito", tint = NewColor, modifier = Modifier.size(16.dp))
                    }
                    if (d.isNew) {
                        Spacer(Modifier.width(6.dp))
                        Text("NUEVO", color = NewColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val line2 = listOf(d.ip, d.vendor).filter { it.isNotBlank() }.joinToString(" · ")
                if (line2.isNotBlank()) Text(line2, style = MaterialTheme.typography.bodySmall)
                Text(d.mac, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (d.lastConnection.isNotBlank()) {
                    Text(
                        "Última conexión: ${d.lastConnection}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(statusLabel(d.status), color = color, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
    }
}

private fun statusColor(s: Status) = when (s) {
    Status.ONLINE -> OnlineColor
    Status.DOWN -> DownColor
    else -> OfflineColor
}

private fun statusLabel(s: Status) = when (s) {
    Status.ONLINE -> "Online"
    Status.OFFLINE -> "Offline"
    Status.DOWN -> "Caído"
    Status.ARCHIVED -> "Archivado"
}

// ---------------------------------------------------------------- Eventos

@Composable
fun EventsScreen(vm: MainViewModel) {
    val names = vm.devices.associate { it.mac.lowercase() to it.title }
    Column(Modifier.fillMaxSize()) {
        if (vm.error != null) Box(Modifier.padding(16.dp)) { ErrorCard(vm.error!!) }
        if (vm.events.isEmpty() && vm.error == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin eventos en las últimas 72 horas.")
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.events) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row {
                            Text(e.type.ifBlank { "Evento" }, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(e.dateTime, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(names[e.mac.lowercase()] ?: e.mac)
                        val extra = listOf(e.ip, e.info).filter { it.isNotBlank() }.joinToString(" · ")
                        if (extra.isNotBlank()) {
                            Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Ajustes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val p = vm.prefs
    var url by rememberSaveable { mutableStateOf(p.baseUrl) }
    var token by rememberSaveable { mutableStateOf(p.token) }
    var showToken by rememberSaveable { mutableStateOf(false) }
    var notifyNew by rememberSaveable { mutableStateOf(p.notifyNew) }
    var notifyDown by rememberSaveable { mutableStateOf(p.notifyDown) }
    var interval by rememberSaveable { mutableStateOf(p.intervalMin) }
    var status by remember { mutableStateOf("") }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Servidor NetAlertX", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL de la API (https://…)") },
            supportingText = { Text("Debe apuntar a la API (puerto 20212 o su proxy HTTPS)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("API_TOKEN") },
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = showToken, onCheckedChange = { showToken = it })
            Spacer(Modifier.width(8.dp))
            Text("Mostrar token")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                status = "Probando…"
                vm.testConnection(url, token) { status = it }
            }) { Text("Probar conexión") }
            Button(onClick = {
                val changed = url.trim().trimEnd('/') != p.baseUrl || token.trim() != p.token
                p.baseUrl = url
                p.token = token
                p.notifyNew = notifyNew
                p.notifyDown = notifyDown
                p.intervalMin = interval
                if (changed) p.resetBaseline()
                if (notifyNew || notifyDown) requestNotifPermission()
                Scheduler.apply(ctx)
                status = "Guardado."
                onSaved()
            }) { Text("Guardar") }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(8.dp))
        Text("Notificaciones", style = MaterialTheme.typography.titleMedium)
        SwitchRow("Avisar de dispositivos nuevos", notifyNew) { notifyNew = it }
        SwitchRow("Avisar de dispositivos caídos", notifyDown) { notifyDown = it }
        Text("Comprobar cada", style = MaterialTheme.typography.bodyMedium)
        val options = listOf(15, 30, 60, 120)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = interval == m,
                    onClick = { interval = m },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(if (m < 60) "$m min" else "${m / 60} h") }
            }
        }
        Text(
            "Android decide el momento exacto de las comprobaciones en segundo plano para ahorrar batería; el mínimo es 15 minutos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
