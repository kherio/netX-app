package com.kherio.nax

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    var devices by mutableStateOf<List<Device>>(emptyList())
        private set
    var events by mutableStateOf<List<NetEvent>>(emptyList())
        private set
    var totals by mutableStateOf<Totals?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var lastUpdate by mutableStateOf("")
        private set

    fun refresh() {
        if (!prefs.configured) {
            error = "Configura la URL y el token en Ajustes."
            return
        }
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val api = NaxApi(prefs.baseUrl, prefs.token)
                val result = withContext(Dispatchers.IO) {
                    val d = api.devices()
                    val t = runCatching { api.totals() }.getOrNull() ?: Totals.from(d)
                    val e = runCatching { api.events() }.getOrDefault(emptyList())
                    Triple(d, t, e)
                }
                devices = result.first
                totals = result.second
                events = result.third
                lastUpdate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            } catch (e: ApiException) {
                error = e.message
            } catch (e: IOException) {
                error = networkMessage(e)
            } catch (e: Exception) {
                error = "Respuesta inesperada del servidor: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    /** Prueba una URL/token sin guardarlos. Devuelve un texto para mostrar al usuario. */
    fun testConnection(url: String, token: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val msg = try {
                val n = withContext(Dispatchers.IO) { NaxApi(url, token).devices().size }
                "Conexión correcta: $n dispositivos."
            } catch (e: ApiException) {
                e.message ?: "Error de la API"
            } catch (e: IOException) {
                networkMessage(e)
            } catch (e: Exception) {
                "Respuesta inesperada: ${e.message}"
            }
            onResult(msg)
        }
    }
}
