package br.com.viafluvial.acervodigital.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "acervo.upload")
public class AcervoUploadProperties {

    private long maxImageSizeBytes = 10 * 1024 * 1024; // 10 MB default
    private long maxVideoSizeBytes = 500 * 1024 * 1024; // 500 MB default
    private long maxDocumentSizeBytes = 50 * 1024 * 1024; // 50 MB default

    private List<String> allowedImageMimes = List.of("image/jpeg", "image/png", "image/webp", "image/tiff");
    private List<String> allowedVideoMimes = List.of("video/mp4", "video/quicktime", "video/x-msvideo", "video/x-matroska");
    private List<String> allowedDocumentMimes = List.of("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private Map<String, Long> customSizeLimits = new HashMap<>();

    public long getMaxImageSizeBytes() {
        return maxImageSizeBytes;
    }

    public void setMaxImageSizeBytes(long maxImageSizeBytes) {
        this.maxImageSizeBytes = maxImageSizeBytes;
    }

    public long getMaxVideoSizeBytes() {
        return maxVideoSizeBytes;
    }

    public void setMaxVideoSizeBytes(long maxVideoSizeBytes) {
        this.maxVideoSizeBytes = maxVideoSizeBytes;
    }

    public long getMaxDocumentSizeBytes() {
        return maxDocumentSizeBytes;
    }

    public void setMaxDocumentSizeBytes(long maxDocumentSizeBytes) {
        this.maxDocumentSizeBytes = maxDocumentSizeBytes;
    }

    public List<String> getAllowedImageMimes() {
        return allowedImageMimes;
    }

    public void setAllowedImageMimes(List<String> allowedImageMimes) {
        this.allowedImageMimes = allowedImageMimes;
    }

    public List<String> getAllowedVideoMimes() {
        return allowedVideoMimes;
    }

    public void setAllowedVideoMimes(List<String> allowedVideoMimes) {
        this.allowedVideoMimes = allowedVideoMimes;
    }

    public List<String> getAllowedDocumentMimes() {
        return allowedDocumentMimes;
    }

    public void setAllowedDocumentMimes(List<String> allowedDocumentMimes) {
        this.allowedDocumentMimes = allowedDocumentMimes;
    }

    public Map<String, Long> getCustomSizeLimits() {
        return customSizeLimits;
    }

    public void setCustomSizeLimits(Map<String, Long> customSizeLimits) {
        this.customSizeLimits = customSizeLimits;
    }
}
