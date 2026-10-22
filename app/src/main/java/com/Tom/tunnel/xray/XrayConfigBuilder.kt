package com.tom.tunnel.xray

import com.tom.tunnel.config.Profile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds an Xray outbound for common VMess/VLESS/Trojan configurations.
 * Advanced options should be retained in raw Xray JSON rather than guessed.
 */
object XrayConfigBuilder {
    fun build(profile: Profile, tunFd: Int): String {
        require(tunFd >= 0) { "Descripteur TUN invalide" }
        val stream = JSONObject()
        val security = profile.security.lowercase()
        val network = when (profile.transport.lowercase()) {
            "h2" -> "http"
            else -> profile.transport.lowercase()
        }
        stream.put("network", network)

        when (security) {
            "tls" -> {
                val tls = JSONObject().put("allowInsecure", false)
                if (profile.sni.isNotBlank()) tls.put("serverName", profile.sni)
                stream.put("security", "tls").put("tlsSettings", tls)
            }
            "reality" -> {
                val reality = JSONObject()
                    .put("serverName", profile.sni.ifBlank { profile.server })
                    .put("publicKey", profile.publicKey)
                    .put("shortId", profile.shortId)
                stream.put("security", "reality").put("realitySettings", reality)
            }
            else -> stream.put("security", "none")
        }

        when (network) {
            "ws" -> {
                val headers = JSONObject()
                if (profile.host.isNotBlank()) headers.put("Host", profile.host)
                stream.put("wsSettings", JSONObject()
                    .put("path", profile.path.ifBlank { "/" })
                    .put("headers", headers))
            }
            "grpc" -> stream.put("grpcSettings", JSONObject()
                .put("serviceName", profile.serviceName)
                .put("multiMode", false))
            "http" -> stream.put("httpSettings", JSONObject()
                .put("path", profile.path.ifBlank { "/" })
                .put("host", JSONArray().put(profile.host.ifBlank { profile.server })))
        }

        val user = when (profile.protocol.uppercase()) {
            "VLESS" -> JSONObject()
                .put("id", profile.uuid)
                .put("encryption", "none")
                .also { if (profile.security == "reality" && profile.uuid.isNotBlank()) it.put("flow", "") }
            "VMESS" -> JSONObject().put("id", profile.uuid).put("alterId", 0).put("security", "auto")
            "TROJAN" -> JSONObject().put("password", profile.password)
            else -> throw IllegalArgumentException("Protocole non pris en charge par ce générateur : ${profile.protocol}")
        }
        val protocol = profile.protocol.lowercase()
        val settings = when (protocol) {
            "vless", "vmess" -> JSONObject().put("vnext", JSONArray().put(
                JSONObject().put("address", profile.server).put("port", profile.port)
                    .put("users", JSONArray().put(user))
            ))
            "trojan" -> JSONObject().put("servers", JSONArray().put(
                JSONObject().put("address", profile.server).put("port", profile.port)
                    .put("password", profile.password)
            ))
            else -> error("Protocole invalide")
        }

        val root = JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("env", JSONObject().put("xray.tun.fd", tunFd))
            .put("inbounds", JSONArray().put(
                JSONObject().put("tag", "tun-in").put("port", 0).put("protocol", "tun")
                    .put("settings", JSONObject().put("name", "tom_tunnel").put("mtu", 1500))
            ))
            .put("outbounds", JSONArray()
                .put(JSONObject().put("tag", "proxy").put("protocol", protocol).put("settings", settings)
                    .put("streamSettings", stream))
                .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
                .put(JSONObject().put("tag", "block").put("protocol", "blackhole")))
            .put("routing", JSONObject().put("domainStrategy", "AsIs").put("rules", JSONArray().put(
                JSONObject().put("type", "field").put("inboundTag", JSONArray().put("tun-in"))
                    .put("outboundTag", "proxy")
            )))
        return root.toString()
    }
}
