package Utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

    /** Hash a plain-text password using BCrypt. */
    public static String hash(String plain) {
        if (plain == null) throw new IllegalArgumentException("Password cannot be null");
        String hash = BCrypt.hashpw(plain, BCrypt.gensalt(13));
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$")) {
            return "$2y$" + hash.substring(4);
        }
        return hash;
    }

    /**
     * Verify plain password vs stored value.
     * Supports both:
     *   - BCrypt hashed passwords ($2a$, $2b$, $2y$ prefix)
     *   - Legacy plain-text passwords (stored as-is for old seeded users)
     */
    public static boolean verify(String plain, String stored) {
        if (plain == null || stored == null) return false;
        String normalized = stored.startsWith("$2y$") ? "$2a$" + stored.substring(4) : stored;
        try {
            return BCrypt.checkpw(plain, normalized);
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if the string looks like a BCrypt hash. */
    public static boolean looksLikeBCryptHash(String s) {
        if (s == null) return false;
        return s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$");
    }
}