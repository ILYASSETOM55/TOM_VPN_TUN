package com.tom.tunnel.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileCodecTest {
    @Test fun rejectsUnsupportedScheme() {
        assertThrows(IllegalStateException::class.java) { ProfileCodec.parse("openvpn://example.com") }
    }

    @Test fun rejectsInvalidPort() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfileCodec.validate(Profile("x", "x", "VLESS", "example.com", 70000, uuid = "123"))
        }
    }

    @Test fun validatesVlessBasics() {
        val p = Profile("x", "test", "VLESS", "example.com", 443, uuid = "123e4567-e89b-12d3-a456-426614174000")
        ProfileCodec.validate(p)
        assertEquals("VLESS", p.protocol)
    }
}
