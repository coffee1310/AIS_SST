package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.service.sectorService.SectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final SectorService sectorService;

    @GetMapping("/sectors/without-cache")
    public ResponseEntity<Map<String, Object>> getSectorsWithoutCache() {
        Map<String, Object> result = new HashMap<>();

        long startTime = System.nanoTime();

        // Получаем сектора без кэша (принудительно из БД)
        List<SectorDTO> sectors = sectorService.getAllSectorsWithoutCache();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000; // миллисекунды

        result.put("duration_ms", duration);
        result.put("count", sectors.size());
        result.put("data", sectors);
        result.put("cache_used", false);

        log.info("Without cache: {} ms", duration);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/sectors/with-cache")
    public ResponseEntity<Map<String, Object>> getSectorsWithCache() {
        Map<String, Object> result = new HashMap<>();

        long startTime = System.nanoTime();

        // Получаем сектора с кэшем
        List<SectorDTO> sectors = sectorService.getAllSectors();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;

        result.put("duration_ms", duration);
        result.put("count", sectors.size());
        result.put("data", sectors);
        result.put("cache_used", true);

        log.info("With cache: {} ms", duration);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> comparePerformance() {
        Map<String, Object> result = new HashMap<>();

        // Первый запрос (без кэша)
        long start1 = System.nanoTime();
        List<SectorDTO> sectors1 = sectorService.getAllSectorsWithoutCache();
        long duration1 = (System.nanoTime() - start1) / 1_000_000;

        // Второй запрос (с кэшем)
        long start2 = System.nanoTime();
        List<SectorDTO> sectors2 = sectorService.getAllSectors();
        long duration2 = (System.nanoTime() - start2) / 1_000_000;

        result.put("without_cache_ms", duration1);
        result.put("with_cache_ms", duration2);
        result.put("speedup_factor", String.format("%.2fx", (double) duration1 / duration2));
        result.put("without_cache_count", sectors1.size());
        result.put("with_cache_count", sectors2.size());

        log.info("Performance comparison - Without cache: {} ms, With cache: {} ms, Speedup: {}x",
                duration1, duration2, String.format("%.2f", (double) duration1 / duration2));

        return ResponseEntity.ok(result);
    }
}