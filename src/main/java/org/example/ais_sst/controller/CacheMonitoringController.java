package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.service.redisService.RedisService;
import org.example.ais_sst.service.sectorService.SectorCacheService;
import org.example.ais_sst.service.sectorService.SectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheMonitoringController {

    private final SectorCacheService sectorCacheService;
    private final SectorService sectorService;
    private final RedisService redisService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("hit_rate", String.format("%.2f%%", sectorCacheService.getHitRate()));
        stats.put("hits", sectorCacheService.getHits());
        stats.put("misses", sectorCacheService.getMisses());
        stats.put("total_requests", sectorCacheService.getHits() + sectorCacheService.getMisses());
        stats.put("cache_size", sectorCacheService.getCacheSize());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getCacheDetails() {
        Map<String, Object> details = new HashMap<>();

        details.put("cache_keys_count", sectorCacheService.getCacheSize());
        details.put("all_sectors_cached", redisService.exists("cache:sectors:all"));
        details.put("active_sectors_cached", redisService.exists("cache:sectors:active"));

        return ResponseEntity.ok(details);
    }
}