package com.tom.tunnel.identity

import android.content.Context
import java.security.SecureRandom

object DeviceIdentity {
    private const val PREFS = "tom_tunnel_private"
    private const val KEY = "device_identity"

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val bytes = ByteArray(6)
        SecureRandom().nextBytes(bytes)
        val id = "TT-" + bytes.joinToString("") { "%02X".format(it) }
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}
