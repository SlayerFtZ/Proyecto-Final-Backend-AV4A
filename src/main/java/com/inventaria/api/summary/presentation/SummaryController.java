package com.inventaria.api.summary.presentation;

import com.inventaria.api.summary.domain.SummaryHistoryItem;
import com.inventaria.api.summary.domain.SummaryResponse;
import com.inventaria.api.summary.domain.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumen")
@Tag(name = "Summary", description = "Resumen del día con IA de texto")
public class SummaryController {

    private final SummaryService service;

    public SummaryController(SummaryService service) {
        this.service = service;
    }

    @GetMapping("/datos")
    @Operation(summary = "Datos estructurados del día: stock bajo, por caducar, ventas hoy y top vendidos (sin IA)")
    public SummaryResponse getDayData() {
        return service.getDayData();
    }

    @PostMapping("/generar")
    @Operation(summary = "Genera resumen del día con IA de texto (fallback a plantilla local si IA no está disponible)")
    public SummaryResponse generate() {
        return service.generateSummary();
    }

    @GetMapping("/historial")
    @Operation(summary = "Últimos 10 resúmenes guardados en ResumenesIA")
    public List<SummaryHistoryItem> history() {
        return service.getHistory();
    }
}
