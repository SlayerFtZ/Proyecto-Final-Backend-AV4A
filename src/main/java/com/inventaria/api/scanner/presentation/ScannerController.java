package com.inventaria.api.scanner.presentation;

import com.inventaria.api.scanner.domain.ProductSuggestionResponse;
import com.inventaria.api.scanner.domain.VisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/scanner")
@Tag(name = "Scanner", description = "Análisis de fotos de productos con IA de visión")
public class ScannerController {

    private final VisionService service;

    public ScannerController(VisionService service) {
        this.service = service;
    }

    @PostMapping(value = "/analizar-foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analiza una foto de producto con IA de visión y devuelve nombre, categoría y tipo sugeridos")
    public ProductSuggestionResponse analyzePhoto(@RequestPart("foto") MultipartFile foto) {
        return service.analyzePhoto(foto);
    }
}
