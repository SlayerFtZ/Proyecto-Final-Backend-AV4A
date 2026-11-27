package com.inventaria.api.products.presentation;

import com.inventaria.api.products.domain.ProductService;
import com.inventaria.api.shared.error.DuplicateBarcodeException;
import com.inventaria.api.shared.error.InsufficientStockException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
// Prueba solo el controller y GlobalExceptionHandler; la seguridad (JWT) no aplica aquí.
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean ProductService service;

    @Test
    void post_invalidBody_returns400WithValidationCode() throws Exception {
        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void post_duplicateBarcode_returns409() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateBarcodeException("12345678901"));

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CODIGO_BARRAS_DUPLICADO"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(service.findById(99)).thenThrow(new ResourceNotFoundException("Producto", 99));

        mvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void patchStock_insufficientStock_returns409() throws Exception {
        when(service.adjustStock(eq(1), eq(-10)))
                .thenThrow(new InsufficientStockException("Leche", 2, 10));

        mvc.perform(patch("/api/productos/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\": -10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private String validProductJson() {
        return """
                {
                  "barcode": "12345678901",
                  "name": "Leche entera",
                  "price": 10.00,
                  "stock": 10,
                  "minimumStock": 5,
                  "type": "Perecedero",
                  "expirationDate": "%s",
                  "categoryId": 1,
                  "supplierId": 1
                }
                """.formatted(LocalDate.now().plusDays(30));
    }
}
