package com.inventaria.api.sales.presentation;

import com.inventaria.api.sales.domain.SaleService;
import com.inventaria.api.shared.error.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SaleController.class)
// Prueba solo el controller y GlobalExceptionHandler; la seguridad (JWT) no aplica aquí.
@AutoConfigureMockMvc(addFilters = false)
class SaleControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean SaleService service;

    @Test
    void post_emptyItems_returns400() throws Exception {
        mvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void post_insufficientStock_returns409() throws Exception {
        when(service.create(any())).thenThrow(new InsufficientStockException("Leche", 2, 5));

        mvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 5}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
    }

    @Test
    void post_validRequest_returns201WithLocation() throws Exception {
        SaleResponse response = new SaleResponse(42, LocalDateTime.now(),
                new BigDecimal("10.00"), List.of());
        when(service.create(any())).thenReturn(response);

        mvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 2}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/ventas/42")))
                .andExpect(jsonPath("$.id").value(42));
    }
}
