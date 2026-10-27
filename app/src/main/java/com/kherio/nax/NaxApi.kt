package com.kherio.nax

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String) : Exception(message)

/** Quita espacios y barras finales; si falta el esquema, asume https://. */
fun normalizeUrl(raw: String): String {
    val t = raw.trim().trimEnd('/')
    return if (t.isNotEmpty() && !t.contains("://")) "https://$t" else t
}

/** Texto legible para errores de red. */
fun networkMessage(e: IOException): String {
    val m = e.message ?: "error de red"
    return if (m.contains("Cleartext", ignoreCase = true)) {
        "La URL usa http:// y Android lo bloquea. Usa https://"
    } else {
        "No se pudo conectar: $m"
    }
}

/**
 * Cliente mínimo de la API REST de NetAlertX (solo lectura).
 * Autenticación: cabecera "Authorization: Bearer <API_TOKEN>".
 */
class NaxApi(baseUrl: String, private val token: String) {

    private val base = normalizeUrl(baseUrl)

    private fun get(path: String): String {
        val conn = try {
            URL(base + path).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw ApiException("URL no válida: $base")
        }
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        try {
            val code = conn.responseCode
            if (code == 401 || code == 403) {
                throw ApiException("Token rechazado (HTTP $code). Revisa el API_TOKEN.")
            }
            if (code !in 200..299) throw ApiException("Error HTTP $code en $path")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun devices(): List<Device> {
        val arr = extractArray(get("/devices"), "devices", "data")
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.toDevice() }
    }

    fun totals(): Totals {
        val t = get("/devices/totals").trim()
        if (t.startsWith("[")) return totalsFromArray(JSONArray(t))
        val o = JSONObject(t)
        o.optJSONArray("totals")?.let { return totalsFromArray(it) }
        return Totals(
            devices = o.i("devices", "total"),
            connected = o.i("connected", "online"),
            favorites = o.i("favorites"),
            newDevices = o.i("new"),
            down = o.i("down"),
            archived = o.i("archived"),
        )
    }

    fun events(): List<NetEvent> {
        val body = try {
            get("/events/72")
        } catch (e: ApiException) {
            get("/events/last")
        }
        val arr = extractArray(body, "events", "data")
        return (0 until arr.length())
            .mapNotNull { arr.optJSONObject(it) }
            .map {
                NetEvent(
                    mac = it.s("eveMac", "mac"),
                    ip = it.s("eveIp", "ip"),
                    dateTime = it.s("eveDateTime", "dateTime"),
                    type = it.s("eveEventType", "type"),
                    info = it.s("eveAdditionalInfo", "info"),
                )
            }
            .sortedByDescending { it.dateTime }
    }

    // ---------- Parseo tolerante ----------

    private fun totalsFromArray(a: JSONArray) = Totals(
        devices = a.optInt(0), connected = a.optInt(1), favorites = a.optInt(2),
        newDevices = a.optInt(3), down = a.optInt(4), archived = a.optInt(5),
    )

    private fun extractArray(body: String, vararg keys: String): JSONArray {
        val t = body.trim()
        if (t.startsWith("[")) return JSONArray(t)
        val o = JSONObject(t)
        if (o.has("success") && !o.optBoolean("success", true)) {
            throw ApiException(o.optString("error", "La API devolvió un error"))
        }
        for (k in keys) o.optJSONArray(k)?.let { return it }
        o.optJSONObject("data")?.let { d -> for (k in keys) d.optJSONArray(k)?.let { return it } }
        return JSONArray()
    }

    private fun JSONObject.toDevice(): Device {
        val statusRaw = s("devStatus", "status").lowercase()
        val present = i("devPresentLastScan")
        val archived = i("devIsArchived") == 1 || statusRaw.contains("archiv")
        val status = when {
            archived -> Status.ARCHIVED
            statusRaw.contains("down") -> Status.DOWN
            statusRaw.contains("off") -> Status.OFFLINE
            statusRaw.contains("on") || statusRaw == "connected" -> Status.ONLINE
            present == 1 -> Status.ONLINE
            else -> Status.OFFLINE
        }
        return Device(
            mac = s("devMac", "devMAC", "id", "mac"),
            name = s("devName", "title", "name"),
            ip = s("devLastIP", "devIP", "ip"),
            vendor = s("devVendor", "vendor"),
            type = s("devType", "type"),
            status = status,
            isNew = i("devIsNew") == 1 || statusRaw.contains("new"),
            favorite = i("devFavorite", "favorite") == 1,
            lastConnection = s("devLastConnection"),
            firstConnection = s("devFirstConnection"),
        )
    }

    private fun JSONObject.s(vararg keys: String): String {
        for (k in keys) if (has(k) && !isNull(k)) return optString(k).trim()
        return ""
    }

    private fun JSONObject.i(vararg keys: String): Int {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                return when (val v = opt(k)) {
                    is Number -> v.toInt()
                    is Boolean -> if (v) 1 else 0
                    is String -> v.toIntOrNull() ?: 0
                    else -> 0
                }
            }
        }
        return 0
    }
}
