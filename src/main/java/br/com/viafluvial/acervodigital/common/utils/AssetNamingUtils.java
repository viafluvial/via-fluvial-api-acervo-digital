package br.com.viafluvial.acervodigital.common.utils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public final class AssetNamingUtils {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private AssetNamingUtils() {
    }

    public static String buildMediaTitle(
        String mediaType,
        String entityType,
        UUID entityId,
        String mediaPublicKey,
        OffsetDateTime timestamp,
        String relatedEntityName
    ) {
        String ts = formatTimestamp(timestamp);
        String base = String.format(
            Locale.ROOT,
            "MIDIA_%s_%s_%s_%s_%s",
            normalizeToken(mediaType),
            normalizeToken(entityType),
            shortUuid(entityId),
            ts,
            shortToken(mediaPublicKey)
        );

        String relatedToken = normalizeToken(relatedEntityName);
        if ("NA".equals(relatedToken)) {
            return base;
        }

        String compactRelatedToken = relatedToken.substring(0, Math.min(32, relatedToken.length()));
        return base + "_" + compactRelatedToken;
    }

    public static String buildPrivateDocumentFileName(
        String entityType,
        UUID entityId,
        String documentTypeCode,
        String documentPublicKey,
        OffsetDateTime timestamp,
        String extension
    ) {
        String ext = normalizeExtension(extension);
        return String.format(
            Locale.ROOT,
            "DOC_%s_%s_%s_%s_%s_%s.%s",
            normalizeToken(entityType),
            shortUuid(entityId),
            normalizeToken(documentTypeCode),
            normalizeToken("PRIVADO"),
            formatTimestamp(timestamp),
            shortToken(documentPublicKey),
            ext
        );
    }

    public static String buildPrivateDocumentObjectPath(
        String documentPublicKey,
        String standardizedFileName
    ) {
        return "private-documents/" + normalizeToken(documentPublicKey).toLowerCase(Locale.ROOT) + "/" + standardizedFileName;
    }

    public static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "NA";
        }

        String normalized = value
            .trim()
            .replaceAll("[^a-zA-Z0-9]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            return "NA";
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    public static String extensionFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot + 1 >= filename.length()) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "bin";
        }
        return extension.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public static String shortUuid(UUID value) {
        if (value == null) {
            return "00000000";
        }
        String token = value.toString().replace("-", "");
        return token.substring(0, Math.min(8, token.length())).toUpperCase(Locale.ROOT);
    }

    public static String shortToken(String value) {
        String normalized = normalizeToken(value).replace("_", "");
        return normalized.substring(0, Math.min(8, normalized.length()));
    }

    private static String formatTimestamp(OffsetDateTime value) {
        OffsetDateTime timestamp = value == null ? OffsetDateTime.now() : value;
        return TS_FORMATTER.format(timestamp);
    }
}
