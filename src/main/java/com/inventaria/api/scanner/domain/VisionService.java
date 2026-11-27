package com.inventaria.api.scanner.domain;

import com.inventaria.api.categories.domain.Category;
import com.inventaria.api.categories.infrastructure.CategoryJpaRepository;
import com.inventaria.api.shared.error.InvalidFileException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;

@Service
public class VisionService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final AiVisionClient aiVisionClient;
    private final CategoryJpaRepository categoryRepository;

    public VisionService(AiVisionClient aiVisionClient, CategoryJpaRepository categoryRepository) {
        this.aiVisionClient     = aiVisionClient;
        this.categoryRepository = categoryRepository;
    }

    public ProductSuggestionResponse analyzePhoto(MultipartFile file) {
        byte[] imageBytes = validateAndRead(file);

        List<Category> categories   = categoryRepository.findAll();
        List<String>   categoryNames = categories.stream().map(Category::getName).toList();

        ProductSuggestionResponse suggestion = aiVisionClient.analyze(imageBytes, file.getContentType(), categoryNames);

        Integer categoryId = findCategoryId(suggestion.suggestedCategory(), categories);

        return new ProductSuggestionResponse(
                suggestion.suggestedName(),
                suggestion.suggestedCategory(),
                categoryId,
                suggestion.productType(),
                suggestion.description()
        );
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("La imagen no puede estar vacía");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidFileException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamaño máximo de 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileException("Tipo de imagen no soportado. Use JPEG, PNG o WebP");
        }
        try {
            byte[] bytes = file.getBytes();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new InvalidFileException("El archivo no es una imagen válida o está corrupta");
            }
            return bytes;
        } catch (InvalidFileException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidFileException("No se pudo leer la imagen: " + e.getMessage());
        }
    }

    private Integer findCategoryId(String suggestedCategory, List<Category> categories) {
        if (suggestedCategory == null || suggestedCategory.isBlank()) return null;
        String normalized = normalize(suggestedCategory);
        return categories.stream()
                .filter(c -> normalize(c.getName()).equals(normalized))
                .findFirst()
                .map(Category::getId)
                .orElse(null);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }
}
