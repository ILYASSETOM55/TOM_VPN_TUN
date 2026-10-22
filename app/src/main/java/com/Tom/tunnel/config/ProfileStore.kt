package com.tom.tunnel.config

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local profile storage. Credentials remain on-device in app-private preferences. */
class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun save(profile: Profile) {
        val all = JSONArray(prefs.getString("items", "[]"))
        val next = JSONArray()
        for (i in 0 until all.length()) {
            val item = all.getJSONObject(i)
            if (item.optString("id") != profile.id) next.put(item)
        }
        next.put(profile.toJson())
        prefs.edit().putString("items", next.toString()).apply()
    }

    fun get(id: String): Profile? = listProfiles().firstOrNull { it.id == id }

    fun listProfiles(): List<Profile> {
        val all = JSONArray(prefs.getString("items", "[]"))
        return (0 until all.length()).mapNotNull { i ->
            try { ProfileCodec.parse(all.getJSONObject(i).toString()) } catch (_: Exception) { null }
        }
    }

    /** Backwards-compatible metadata list for simple UI/debugging. */
    fun list(): List<JSONObject> {
        val all = JSONArray(prefs.getString("items", "[]"))
        return (0 until all.length()).map { all.getJSONObject(it) }
    }

    private fun Profile.toJson() = JSONObject()
        .put("id", id).put("name", name).put("protocol", protocol).put("server", server)
        .put("port", port).put("uuid", uuid).put("password", password)
        .put("transport", transport).put("path", path).put("host", host)
        .put("security", security).put("sni", sni).put("publicKey", publicKey)
        .put("shortId", shortId).put("serviceName", serviceName).put("dns", dns)
        .put("createdAt", createdAt).put("expiresAt", expiresAt ?: JSONObject.NULL)
}
