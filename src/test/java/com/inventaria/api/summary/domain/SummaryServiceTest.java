package com.inventaria.api.summary.domain;

import com.inventaria.api.shared.config.IaProperties;
import com.inventaria.api.shared.error.AiUnavailableException;
import com.inventaria.api.shared.security.CurrentUser;
import com.inventaria.api.summary.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock SummaryQueryRepository queryRepository;
    @Mock AlertService alertService;
    @Mock AiTextClient aiTextClient;
    @Mock LocalSummaryGenerator localGenerator;
    @Mock IaProperties iaProperties;
    @Mock SummaryIaJpaRepository iaRepository;
    @Mock CurrentUser currentUser;

    @InjectMocks SummaryService service;

    private static final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        TodaySalesRow salesRow = mock(TodaySalesRow.class);
        when(salesRow.getTotalVentas()).thenReturn(3L);
        when(salesRow.getMontoTotal()).thenReturn(new BigDecimal("150.00"));

        when(currentUser.getId()).thenReturn(USER_ID);
        when(queryRepository.findLowStockProducts(USER_ID)).thenReturn(List.of());
        when(queryRepository.findExpiringProducts(USER_ID)).thenReturn(List.of());
        when(queryRepository.findTodaySales(USER_ID)).thenReturn(List.of(salesRow));
        when(queryRepository.findTopSellersToday(USER_ID)).thenReturn(List.of());
        when(alertService.generateAlerts(any(), any())).thenReturn(List.of());
    }

    @Test
    void generateSummary_aiConfiguredAndSucceeds_returnsIaOrigin() {
        when(iaProperties.isConfigured()).thenReturn(true);
        when(aiTextClient.generateText(anyString(), anyString())).thenReturn("Resumen generado por IA");
        when(localGenerator.generate(any(), any())).thenReturn("Resumen local");

        SummaryResponse response = service.generateSummary();

        assertEquals("IA", response.origin());
        assertEquals("Resumen generado por IA", response.text());
        verify(localGenerator, never()).generate(any(), any());
        // El resumen se guarda a nombre del usuario autenticado.
        verify(iaRepository).save(argThat((AiSummaryRecord r) -> USER_ID.equals(r.getUserId())));
    }

    @Test
    void generateSummary_aiConfiguredButFails_fallsBackToLocal() {
        when(iaProperties.isConfigured()).thenReturn(true);
        when(aiTextClient.generateText(anyString(), anyString()))
                .thenThrow(new AiUnavailableException("timeout"));
        when(localGenerator.generate(any(), any())).thenReturn("Resumen local");

        SummaryResponse response = service.generateSummary();

        assertEquals("LOCAL", response.origin());
        assertEquals("Resumen local", response.text());
    }

    @Test
    void generateSummary_aiNotConfigured_usesLocalGenerator() {
        when(iaProperties.isConfigured()).thenReturn(false);
        when(localGenerator.generate(any(), any())).thenReturn("Resumen local sin IA");

        SummaryResponse response = service.generateSummary();

        assertEquals("LOCAL", response.origin());
        assertEquals("Resumen local sin IA", response.text());
        verify(aiTextClient, never()).generateText(anyString(), anyString());
    }
}
