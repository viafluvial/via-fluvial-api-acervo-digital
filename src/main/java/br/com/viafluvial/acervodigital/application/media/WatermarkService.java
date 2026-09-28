package br.com.viafluvial.acervodigital.application.media;

import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class WatermarkService {

    private final ResourceLoader resourceLoader;

    @Value("${acervo.watermark.enabled:true}")
    private boolean enabled;

    @Value("${acervo.watermark.logo-path:classpath:watermark/viafluvial-watermark.png}")
    private String logoPath;

    @Value("${acervo.watermark.opacity:0.08}")
    private float opacity;

    @Value("${acervo.watermark.scale-ratio:0.04}")
    private double scaleRatio;

    @Value("${acervo.watermark.margin-ratio:0.02}")
    private double marginRatio;

    public WatermarkService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public byte[] applyBottomRightWatermark(byte[] imageContent, String sourceMimeType) {
        if (!enabled) {
            return imageContent;
        }

        BufferedImage source = readImage(imageContent, "INVALID_IMAGE_CONTENT", "Arquivo de imagem invalido para aplicacao de marca d'agua.");
        BufferedImage logo = readImage(loadLogoBytes(), "WATERMARK_LOGO_INVALID", "Arquivo de logo da marca d'agua invalido.");

        String outputFormat = outputFormatForMimeType(sourceMimeType);
        BufferedImage canvas = createCanvas(source, outputFormat);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, null);

        int watermarkWidth = Math.max(1, (int) Math.round(source.getWidth() * clampScale(scaleRatio)));
        int watermarkHeight = Math.max(1,
            (int) Math.round(((double) logo.getHeight() / Math.max(1, logo.getWidth())) * watermarkWidth));

        Image scaledLogo = logo.getScaledInstance(watermarkWidth, watermarkHeight, Image.SCALE_SMOOTH);
        int margin = Math.max(2, (int) Math.round(source.getWidth() * clampMargin(marginRatio)));
        int x = Math.max(0, source.getWidth() - watermarkWidth - margin);
        int y = Math.max(0, source.getHeight() - watermarkHeight - margin);

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clampOpacity(opacity)));
        graphics.drawImage(scaledLogo, x, y, null);
        graphics.dispose();

        return encode(canvas, outputFormat);
    }

    private byte[] loadLogoBytes() {
        try {
            Resource resource = resourceLoader.getResource(logoPath);
            if (!resource.exists()) {
                throw new DomainException("WATERMARK_LOGO_NOT_FOUND", "Logo da marca d'agua nao encontrado em " + logoPath + ".", 500);
            }
            return resource.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw new DomainException("WATERMARK_LOGO_READ_ERROR", "Falha ao ler o logo da marca d'agua.", 500);
        }
    }

    private BufferedImage readImage(byte[] content, String code, String message) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new DomainException(code, message, 422);
            }
            return image;
        } catch (IOException ex) {
            throw new DomainException(code, message, 422);
        }
    }

    private BufferedImage createCanvas(BufferedImage source, String format) {
        int type = "png".equalsIgnoreCase(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        return new BufferedImage(source.getWidth(), source.getHeight(), type);
    }

    private byte[] encode(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            boolean wrote = ImageIO.write(image, format, out);
            if (!wrote) {
                throw new DomainException("UNSUPPORTED_IMAGE_FORMAT", "Formato de imagem nao suportado para watermark.", 422);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new DomainException("IMAGE_PROCESSING_ERROR", "Falha ao aplicar watermark na imagem.", 500);
        }
    }

    private String outputFormatForMimeType(String mimeType) {
        if (mimeType == null) {
            return "jpeg";
        }
        return switch (mimeType) {
            case "image/png" -> "png";
            default -> "jpeg";
        };
    }

    private float clampOpacity(float value) {
        if (value < 0.01f) {
            return 0.01f;
        }
        return Math.min(value, 1.0f);
    }

    private double clampScale(double value) {
        if (value < 0.01d) {
            return 0.01d;
        }
        return Math.min(value, 0.25d);
    }

    private double clampMargin(double value) {
        if (value < 0d) {
            return 0d;
        }
        return Math.min(value, 0.2d);
    }
}