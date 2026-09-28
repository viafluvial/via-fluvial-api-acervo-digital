package br.com.viafluvial.acervodigital.application.media;

import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.AcervoFileResponse;
import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ImageDerivativeService {

    private static final Map<AcervoFileResponse.VariantEnum, int[]> VARIANT_DIMENSIONS = Map.of(
        AcervoFileResponse.VariantEnum.THUMB, new int[] {200, 200},
        AcervoFileResponse.VariantEnum.CARD, new int[] {640, 360},
        AcervoFileResponse.VariantEnum.GALLERY, new int[] {1280, 720},
        AcervoFileResponse.VariantEnum.BANNER, new int[] {1920, 1080},
        AcervoFileResponse.VariantEnum.SLIDE, new int[] {1600, 900}
    );

    public List<ImageDerivative> createDerivatives(byte[] originalContent, String originalMimeType) {
        BufferedImage sourceImage = readImage(originalContent);
        String outputFormat = outputFormatForMimeType(originalMimeType);
        String outputMime = "image/" + outputFormat;

        List<ImageDerivative> derivatives = new ArrayList<>();
        derivatives.add(createOriginalDerivative(sourceImage, outputFormat, outputMime));

        for (Map.Entry<AcervoFileResponse.VariantEnum, int[]> entry : VARIANT_DIMENSIONS.entrySet()) {
            int[] target = entry.getValue();
            BufferedImage resized = resizeKeepAspect(sourceImage, target[0], target[1]);
            byte[] encoded = encode(resized, outputFormat);
            derivatives.add(new ImageDerivative(
                entry.getKey(),
                encoded,
                outputMime,
                resized.getWidth(),
                resized.getHeight(),
                encoded.length,
                sha256(encoded)
            ));
        }

        return derivatives;
    }

    private ImageDerivative createOriginalDerivative(BufferedImage sourceImage, String outputFormat, String outputMime) {
        byte[] encoded = encode(sourceImage, outputFormat);
        return new ImageDerivative(
            AcervoFileResponse.VariantEnum.ORIGINAL,
            encoded,
            outputMime,
            sourceImage.getWidth(),
            sourceImage.getHeight(),
            encoded.length,
            sha256(encoded)
        );
    }

    private BufferedImage readImage(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new DomainException("INVALID_IMAGE_CONTENT", "Arquivo informado não é uma imagem válida.", 422);
            }
            return image;
        } catch (IOException ex) {
            throw new DomainException("INVALID_IMAGE_CONTENT", "Não foi possível interpretar a imagem enviada.", 422);
        }
    }

    private BufferedImage resizeKeepAspect(BufferedImage source, int targetWidth, int targetHeight) {
        double sourceAspect = (double) source.getWidth() / source.getHeight();
        double targetAspect = (double) targetWidth / targetHeight;

        int width;
        int height;
        if (sourceAspect > targetAspect) {
            width = targetWidth;
            height = (int) Math.round(targetWidth / sourceAspect);
        } else {
            height = targetHeight;
            width = (int) Math.round(targetHeight * sourceAspect);
        }

        Image tmp = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private byte[] encode(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            boolean wrote = ImageIO.write(image, format, out);
            if (!wrote) {
                throw new DomainException("UNSUPPORTED_IMAGE_FORMAT", "Formato de imagem não suportado para processamento.", 422);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new DomainException("IMAGE_PROCESSING_ERROR", "Falha ao processar imagem.", 500);
        }
    }

    private String outputFormatForMimeType(String mimeType) {
        if (mimeType == null) {
            return "jpeg";
        }
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/webp" -> "jpeg";
            case "image/tiff" -> "jpeg";
            default -> "jpeg";
        };
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new DomainException("HASH_ERROR", "Falha ao calcular checksum do arquivo.", 500);
        }
    }
}
