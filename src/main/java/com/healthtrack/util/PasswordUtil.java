package com.healthtrack.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (looksLikeBcrypt(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        return Objects.equals(rawPassword, storedPassword);
    }

    private static boolean looksLikeBcrypt(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}