package com.example.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyCodecTest {

    @Test
    @DisplayName("生成的 key 能被解析，secret 哈希一致")
    void generatedKeyRoundTrips() {
        ApiKeyCodec.Generated generated = ApiKeyCodec.generate();

        assertTrue(generated.raw().matches("pp_[A-Za-z0-9]{10}_[A-Za-z0-9_-]{43}"), generated.raw());
        assertEquals(64, generated.secretHash().length());

        ApiKeyCodec.Parsed parsed = ApiKeyCodec.parse(generated.raw());
        assertEquals(generated.kid(), parsed.kid());
        assertTrue(ApiKeyCodec.matches(parsed.secret(), generated.secretHash()));
    }

    @Test
    @DisplayName("secret 不对则哈希不匹配")
    void wrongSecretDoesNotMatch() {
        ApiKeyCodec.Generated generated = ApiKeyCodec.generate();
        ApiKeyCodec.Parsed parsed = ApiKeyCodec.parse(generated.raw());

        String last = parsed.secret().substring(parsed.secret().length() - 1);
        String tampered = parsed.secret().substring(0, parsed.secret().length() - 1)
                + (last.equals("A") ? "B" : "A");

        assertFalse(ApiKeyCodec.matches(tampered, generated.secretHash()));
    }

    @Test
    @DisplayName("secret 是 base64url，里面带 _ 也必须完整解析")
    void secretWithUnderscoreParsesWhole() {
        String secret = "A_".repeat(21) + "B";

        ApiKeyCodec.Parsed parsed = ApiKeyCodec.parse("pp_0123456789_" + secret);

        assertEquals("0123456789", parsed.kid());
        assertEquals(secret, parsed.secret());
    }

    @Test
    @DisplayName("格式不对返回 null")
    void malformedKeyReturnsNull() {
        assertNull(ApiKeyCodec.parse(null));
        assertNull(ApiKeyCodec.parse(""));
        assertNull(ApiKeyCodec.parse("pp_abc_x"));
        assertNull(ApiKeyCodec.parse("xx_0123456789_x"));
        assertNull(ApiKeyCodec.parse("pp_0123456789_"));
        assertNull(ApiKeyCodec.parse("pp_0123456789_x_y"));
        assertNull(ApiKeyCodec.parse("Bearer pp_0123456789_x"));
    }

    @Test
    @DisplayName("两次生成的 kid 和 secret 都不同")
    void generationIsUnique() {
        ApiKeyCodec.Generated first = ApiKeyCodec.generate();
        ApiKeyCodec.Generated second = ApiKeyCodec.generate();

        assertNotEquals(first.kid(), second.kid());
        assertNotEquals(first.secretHash(), second.secretHash());
    }
}
