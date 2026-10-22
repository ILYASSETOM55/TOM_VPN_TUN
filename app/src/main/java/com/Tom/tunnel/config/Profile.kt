package com.tom.tunnel.config

data class Profile(
    val id: String,
    val name: String,
    val protocol: String,
    val server: String,
    val port: Int,
    val uuid: String = "",
    val password: String = "",
    val transport: String = "tcp",
    val path: String = "",
    val host: String = "",
    val security: String = "none",
    val sni: String = "",
    val publicKey: String = "",
    val shortId: String = "",
    val serviceName: String = "",
    val dns: String = "1.1.1.1",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
) {
    companion object {
        fun sample() = Profile("local-default", "Ma configuration", "VLESS", "example.com", 443)
    }
}
