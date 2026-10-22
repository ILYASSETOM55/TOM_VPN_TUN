package com.tom.tunnel.config

import org.json.JSONObject
import java.net.URI
import java.util.UUID

object ProfileCodec {
    fun parse(raw: String): Profile {
        val text = raw.trim()
        require(text.isNotEmpty()) { "contenu vide" }
        if (text.startsWith("{")) return parseJson(JSONObject(text))
        if (text.startsWith("vmess://", ignoreCase = true)) return parseVmess(text)
        val uri = URI(text)
        val scheme = uri.scheme?.lowercase() ?: error("schéma absent")
        val protocol = when (scheme) {
            "vless" -> "VLESS"
            "vmess" -> "VMESS"
            "trojan" -> "TROJAN"
            "socks", "socks5" -> "SOCKS"
            else -> error("protocole non pris en charge : $scheme")
        }
        val query = uri.rawQuery.orEmpty().split("&").mapNotNull {
            val p = it.split("=", limit = 2)
            if (p.size == 2) p[0] to java.net.URLDecoder.decode(p[1], "UTF-8") else null
        }.toMap()
        val userInfo = uri.userInfo.orEmpty()
        val user = userInfo.substringBefore(":")
        val pass = userInfo.substringAfter(":", "")
        val port = if (uri.port in 1..65535) uri.port else 443
        val security = query["security"] ?: if (query["type"] == "ws") "tls" else "none"
        val transport = query["type"] ?: "tcp"
        val profile = Profile(
            id = UUID.randomUUID().toString(),
            name = uri.fragment?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "$protocol ${uri.host}",
            protocol = protocol,
            server = uri.host ?: error("serveur absent"),
            port = port,
            uuid = if (protocol == "VLESS" || protocol == "VMESS") user else "",
            password = if (protocol == "TROJAN" || protocol == "SOCKS") user else pass,
            transport = transport,
            path = query["path"].orEmpty(),
            host = query["host"].orEmpty(),
            security = security,
            sni = query["sni"].orEmpty(),
            publicKey = query["pbk"].orEmpty(),
            shortId = query["sid"].orEmpty(),
            serviceName = query["serviceName"].orEmpty()
        )
        validate(profile)
        return profile
    }


    private fun parseVmess(text: String): Profile {
        val encoded = text.substringAfter("vmess://", "")
        require(encoded.isNotBlank()) { "Lien VMess vide" }
        val decoded = try {
            String(java.util.Base64.getMimeDecoder().decode(encoded), Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Lien VMess Base64 invalide", e)
        }
        val obj = JSONObject(decoded)
        val p = Profile(
            id = UUID.randomUUID().toString(),
            name = obj.optString("ps", "VMess ${obj.optString("add")}"),
            protocol = "VMESS",
            server = obj.optString("add"),
            port = obj.optString("port", "443").toIntOrNull() ?: 443,
            uuid = obj.optString("id"),
            transport = when (obj.optString("net", "tcp").lowercase()) { "http" -> "http"; else -> obj.optString("net", "tcp").lowercase() },
            path = obj.optString("path"),
            host = obj.optString("host"),
            security = if (obj.optString("tls").equals("tls", true)) "tls" else "none",
            sni = obj.optString("sni")
        )
        validate(p)
        return p
    }

    private fun parseJson(obj: JSONObject): Profile {
        val source = if (obj.has("profile")) obj.getJSONObject("profile") else obj
        val p = Profile(
            id = source.optString("id", UUID.randomUUID().toString()),
            name = source.optString("name", "Configuration importée"),
            protocol = source.optString("protocol", "VLESS").uppercase(),
            server = source.optString("server"),
            port = source.optInt("port", 443),
            uuid = source.optString("uuid"),
            password = source.optString("password"),
            transport = source.optString("transport", "tcp"),
            path = source.optString("path"),
            host = source.optString("host"),
            security = source.optString("security", "none"),
            sni = source.optString("sni"),
            publicKey = source.optString("publicKey"),
            shortId = source.optString("shortId"),
            serviceName = source.optString("serviceName"),
            dns = source.optString("dns", "1.1.1.1"),
            createdAt = source.optLong("createdAt", System.currentTimeMillis()),
            expiresAt = if (source.has("expiresAt") && !source.isNull("expiresAt")) source.optLong("expiresAt") else null
        )
        validate(p)
        return p
    }

    fun validate(p: Profile) {
        require(p.protocol in setOf("VLESS", "VMESS", "TROJAN", "SOCKS")) { "protocole inconnu" }
        require(p.server.isNotBlank()) { "serveur obligatoire" }
        require(p.port in 1..65535) { "port invalide" }
        require(p.transport.lowercase() in setOf("tcp", "ws", "grpc", "http", "h2", "kcp", "quic", "domainsocket")) { "transport non reconnu" }
        if (p.protocol == "VLESS" || p.protocol == "VMESS") require(p.uuid.isNotBlank()) { "UUID obligatoire" }
        if (p.protocol == "TROJAN") require(p.password.isNotBlank()) { "mot de passe obligatoire" }
        if (p.security == "reality") {
            require(p.publicKey.isNotBlank()) { "clé publique REALITY obligatoire" }
        }
    }

    fun export(p: Profile, deviceId: String): Pair<String, String> {
        validate(p)
        val obj = JSONObject()
            .put("format", "tom-tunnel-profile")
            .put("formatVersion", 1)
            .put("deviceId", deviceId)
            .put("profile", JSONObject()
                .put("id", p.id).put("name", p.name).put("protocol", p.protocol)
                .put("server", p.server).put("port", p.port).put("uuid", p.uuid)
                .put("password", p.password)
                .put("transport", p.transport).put("path", p.path).put("host", p.host)
                .put("security", p.security).put("sni", p.sni).put("publicKey", p.publicKey)
                .put("shortId", p.shortId).put("serviceName", p.serviceName).put("dns", p.dns)
                .put("createdAt", p.createdAt).put("expiresAt", p.expiresAt ?: JSONObject.NULL))
        val suffix = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        return "TOM_TUNNEL${suffix}_GFF.json" to obj.toString(2)
    }
}
