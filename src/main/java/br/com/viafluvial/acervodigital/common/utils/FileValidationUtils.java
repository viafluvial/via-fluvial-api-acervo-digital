package br.com.viafluvial.acervodigital.common.utils;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.regex.Pattern;

public class FileValidationUtils {

    private static final Pattern UNSAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final Pattern PATH_TRAVERSAL = Pattern.compile(".*(\\\\|/|\\.\\.|\0).*");

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Remove path separators and null bytes
        String sanitized = filename.replace(File.separator, "-")
            .replace("/", "-")
            .replace("\\", "-")
            .replace("\0", "");

        // Remove non-alphanumeric except dots, underscores, and hyphens
        sanitized = UNSAFE_FILENAME.matcher(sanitized).replaceAll("-");

        // Remove consecutive hyphens
        sanitized = sanitized.replaceAll("-+", "-");

        // Remove leading/trailing hyphens/dots
        sanitized = sanitized.replaceAll("^[-._]+", "")
            .replaceAll("[-._]+$", "");

        // Ensure not empty after sanitization
        if (sanitized.isBlank()) {
            sanitized = "file-" + System.nanoTime();
        }

        // Limit length to 255 (common filesystem limit)
        if (sanitized.length() > 255) {
            String extension = getExtension(sanitized);
            int maxLength = 255 - extension.length() - 1;
            sanitized = sanitized.substring(0, maxLength) + "." + extension;
        }

        return sanitized;
    }

    public static boolean isPathTraversalAttempt(String path) {
        if (path == null) return false;
        return PATH_TRAVERSAL.matcher(path).matches();
    }

    public static String getExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') <= 0) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    public static String removeExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') <= 0) {
            return filename;
        }
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
