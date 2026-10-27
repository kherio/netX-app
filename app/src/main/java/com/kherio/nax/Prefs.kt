package com.kherio.nax

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("nax", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString("url", "") ?: ""
        set(v) = sp.edit().putString("url", v.trim().trimEnd('/')).apply()

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(v) = sp.edit().putString("token", v.trim()).apply()

    var notifyNew: Boolean
        get() = sp.getBoolean("notify_new", true)
        set(v) = sp.edit().putBoolean("notify_new", v).apply()

    var notifyDown: Boolean
        get() = sp.getBoolean("notify_down", true)
        set(v) = sp.edit().putBoolean("notify_down", v).apply()

    /** Minutos entre comprobaciones en segundo plano (mínimo 15 por WorkManager). */
    var intervalMin: Int
        get() = sp.getInt("interval", 15)
        set(v) = sp.edit().putInt("interval", v).apply()

    val configured: Boolean get() = baseUrl.isNotBlank() && token.isNotBlank()

    // Estado de la última comprobación (para detectar cambios)
    var baselineDone: Boolean
        get() = sp.getBoolean("baseline", false)
        set(v) = sp.edit().putBoolean("baseline", v).apply()

    var knownMacs: Set<String>
        get() = (sp.getStringSet("known", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("known", v.toSet()).apply()

    var downMacs: Set<String>
        get() = (sp.getStringSet("down", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("down", v.toSet()).apply()

    fun resetBaseline() {
        sp.edit().putBoolean("baseline", false).remove("known").remove("down").apply()
    }
}
