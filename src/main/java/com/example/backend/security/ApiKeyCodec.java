package com.example.backend.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

/**
 * API Key 的生成、解析与哈希。
 * 格式 pp_<kid 10 位>_<secret 43 位>：pp 是系统前缀，kid 用来查行，secret 是 32 字节随机数的 base64url。
 * secret 的字母表含 _，所以明文只按前两个 _ 切，不能整串 split。
 * 库里只存 SHA-256(secret)，明文只在发放时返回一次。
 */
public final class ApiKeyCodec {

    public static final String PREFIX = "pp";

    private static final int KID_LENGTH = 10;
    private static final String KID_PATTERN = "[A-Za-z0-9]{10}";
    private static final String SECRET_PATTERN = "[A-Za-z0-9_-]{43}";
    private static final int SECRET_BYTES = 32;
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeyCodec() {
    }

    /** raw 是完整 key 明文，secretHash 是 secret 段的 SHA-256 十六进制 */
    public record Generated(String raw, String kid, String secretHash) {
    }

    public record Parsed(String kid, String secret) {
    }

    public static Generated generate() {
        String kid = randomBase62(KID_LENGTH);
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Generated(PREFIX + "_" + kid + "_" + secret, kid, hash(secret));
    }

    /** 拆 key 明文。格式不对返回 null */
    public static Parsed parse(String raw) {
        if (raw == null) {
            return null;
        }
        // limit=3：第三段是完整 secret，里面可能还有 _
        String[] parts = raw.split("_", 3);
        if (parts.length != 3 || !PREFIX.equals(parts[0])
                || !parts[1].matches(KID_PATTERN) || !parts[2].matches(SECRET_PATTERN)) {
            return null;
        }
        return new Parsed(parts[1], parts[2]);
    }

    public static String hash(String secret) {
        return HexFormat.of().formatHex(digest(secret));
    }

    /** 常量时间比较 */
    public static boolean matches(String secret, String expectedHexHash) {
        return MessageDigest.isEqual(HexFormat.of().parseHex(expectedHexHash), digest(secret));
    }

    public static String randomBase62(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static byte[] digest(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
