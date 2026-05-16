package org.example.ais_sst.controller;

import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.service.redisService.RedisService;
import org.example.ais_sst.service.sectorService.SectorCacheService;
import org.example.ais_sst.service.sectorService.SectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;  // ← Правильный импорт
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private SectorService sectorService;

    @Autowired
    private SectorCacheService sectorCacheService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment env;

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("db.url", env.getProperty("spring.datasource.url"));
        config.put("active.profiles", Arrays.toString(env.getActiveProfiles()));
        return config;
    }

    @GetMapping("/enum")
    public List<Map<String, Object>> checkEnum() {
        String query = """
            SELECT typname, enumlabel 
            FROM pg_enum 
            JOIN pg_type ON pg_enum.enumtypid = pg_type.oid 
            WHERE typname = 'genders'
        """;
        return jdbcTemplate.queryForList(query);
    }

    @GetMapping("/sectors")
    public ResponseEntity<?> debugSectors() {
        Map<String, Object> debug = new HashMap<>();

        // Проверяем, что возвращает метод getAllSectors()
        List<SectorDTO> sectors = sectorService.getAllSectorsWithoutCache();
        debug.put("database_sectors_count", sectors.size());
        debug.put("database_sectors", sectors.stream()
                .map(s -> Map.of("id", s.getId(), "title", s.getTitle()))
                .collect(Collectors.toList()));

        // Проверяем, что в Redis
        debug.put("redis_sectors_count", sectorCacheService.getCacheSize());
        debug.put("redis_all_sectors_exists", redisService.exists("cache:sectors:all"));
        debug.put("redis_active_sectors_exists", redisService.exists("cache:sectors:active"));

        return ResponseEntity.ok(debug);
    }
}