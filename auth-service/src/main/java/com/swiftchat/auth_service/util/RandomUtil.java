package com.swiftchat.auth_service.util;

import java.security.SecureRandom;
import java.util.Random;

public class RandomUtil {

    private static final int ACTIVATION_KEY_LENGTH = 20;
    private static final int RESET_KEY_LENGTH = 20;
    private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random RANDOM = new SecureRandom();

    private RandomUtil() {
        // Utility class, should not be instantiated
    }

    public static String generateActivationKey() {
        return generateRandomString(ACTIVATION_KEY_LENGTH);
    }

    public static String generateResetKey() {
        return generateRandomString(RESET_KEY_LENGTH);
    }

    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length())));
        }
        return sb.toString();
    }
}
