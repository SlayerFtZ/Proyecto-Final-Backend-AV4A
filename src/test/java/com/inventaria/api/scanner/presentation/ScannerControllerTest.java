package com.inventaria.api.scanner.presentation;

import com.inventaria.api.scanner.domain.ProductSuggestionResponse;
import com.inventaria.api.scanner.domain.VisionService;
import com.inventaria.api.shared.error.AiUnavailableException;
import com.inventaria.api.shared.error.InvalidFileException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScannerController.class)
class ScannerControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean VisionService service;

    @Test
    void analyzePhoto_emptyFile_returns400() throws Exception {
        when(service.analyzePhoto(any()))
                .thenThrow(new InvalidFileException("La imagen no puede estar vacía"));

        mvc.perform(multipart("/api/scanner/analizar-foto")
                        .file(new MockMultipartFile("foto", "test.jpg", "image/jpeg", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_INVALIDO"));
    }

    @Test
    void analyzePhoto_tooLarge_returns413() throws Exception {
        when(service.analyzePhoto(any()))
                .thenThrow(new InvalidFileException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "La imagen supera el tamaño máximo de 5 MB"));

        mvc.perform(multipart("/api/scanner/analizar-foto")
                        .file(new MockMultipartFile("foto", "big.jpg", "image/jpeg", new byte[100])))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_INVALIDO"));
    }

    @Test
    void analyzePhoto_aiUnavailable_returns503() throws Exception {
        when(service.analyzePhoto(any()))
                .thenThrow(new AiUnavailableException("timeout al contactar el servicio"));

        mvc.perform(multipart("/api/scanner/analizar-foto")
                        .file(new MockMultipartFile("foto", "product.jpg", "image/jpeg", new byte[100])))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("IA_NO_DISPONIBLE"));
    }

    @Test
    void analyzePhoto_validImage_returns200WithSuggestion() throws Exception {
        ProductSuggestionResponse suggestion = new ProductSuggestionResponse(
                "Leche entera", "Lácteos", 3, "Perecedero", "Leche entera de vaca 1L");
        when(service.analyzePhoto(any())).thenReturn(suggestion);

        mvc.perform(multipart("/api/scanner/analizar-foto")
                        .file(new MockMultipartFile("foto", "milk.jpg", "image/jpeg", new byte[500])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedName").value("Leche entera"))
                .andExpect(jsonPath("$.productType").value("Perecedero"))
                .andExpect(jsonPath("$.suggestedCategoryId").value(3));
    }
}
