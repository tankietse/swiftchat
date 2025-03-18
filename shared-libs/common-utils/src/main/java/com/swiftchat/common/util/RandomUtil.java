package com.swiftchat.common.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating random tokens and keys.
 */
public class RandomUtil {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Generates a secure activation key.
     * 
     * @return A base64 URL-encoded activation key
     */
    public static String generateActivationKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    /**
     * Generates a secure reset key for password resets.
     * 
     * @return A base64 URL-encoded reset key
     */
    public static String generateResetKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    /**
     * Generates a secure random token with the specified length.
     * 
     * @param length The length of random bytes to generate
     * @return A base64 URL-encoded random token
     */
    public static String generateSecureToken(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
}
