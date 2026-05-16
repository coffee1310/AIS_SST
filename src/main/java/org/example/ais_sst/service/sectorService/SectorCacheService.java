package org.example.ais_sst.service.sectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.service.redisService.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorCacheService {

    private final RedisService redisService;

    @Value("${app.redis.cache.sector.ttl:3600}")
    private long sectorCacheTtlSeconds;

    private static final String SECTOR_PREFIX = "cache:sector:";
    private static final String ALL_SECTORS_KEY = "cache:sectors:all";
    private static final String ACTIVE_SECTORS_KEY = "cache:sectors:active";

    @SuppressWarnings("unchecked")
    public Optional<SectorDTO> getSectorById(Long id) {
        String key = SECTOR_PREFIX + id;
        Object cached = redisService.get(key);

        if (cached != null) {
            log.debug("Cache HIT for sector id: {}", id);
            return Optional.of((SectorDTO) cached);
        }

        log.debug("Cache MISS for sector id: {}", id);
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Optional<List<SectorDTO>> getAllSectors() {
        Object cached = redisService.get(ALL_SECTORS_KEY);

        if (cached != null) {
            log.debug("Cache HIT for all sectors");
            return Optional.of((List<SectorDTO>) cached);
        }

        log.debug("Cache MISS for all sectors");
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Optional<List<SectorDTO>> getActiveSectors() {
        Object cached = redisService.get(ACTIVE_SECTORS_KEY);

        if (cached != null) {
            log.debug("Cache HIT for active sectors");
            return Optional.of((List<SectorDTO>) cached);
        }

        log.debug("Cache MISS for active sectors");
        return Optional.empty();
    }

    public void cacheSector(SectorDTO sector) {
        if (sector == null || sector.getId() == null) return;

        String key = SECTOR_PREFIX + sector.getId();
        redisService.set(key, sector, sectorCacheTtlSeconds);

        log.debug("Sector {} cached, TTL: {}s", sector.getId(), sectorCacheTtlSeconds);
        invalidateSectorLists();
    }

    public void cacheAllSectors(List<SectorDTO> sectors) {
        if (sectors == null) return;

        redisService.set(ALL_SECTORS_KEY, sectors, sectorCacheTtlSeconds);
        log.debug("All sectors cached, count: {}, TTL: {}s", sectors.size(), sectorCacheTtlSeconds);
    }

    public void cacheActiveSectors(List<SectorDTO> sectors) {
        if (sectors == null) return;

        redisService.set(ACTIVE_SECTORS_KEY, sectors, sectorCacheTtlSeconds);
        log.debug("Active sectors cached, count: {}, TTL: {}s", sectors.size(), sectorCacheTtlSeconds);
    }

    public void invalidateSector(Long id) {
        String key = SECTOR_PREFIX + id;
        redisService.delete(key);
        log.debug("Sector {} cache invalidated", id);
        invalidateSectorLists();
    }

    public void invalidateAllSectorCache() {
        redisService.deleteByPattern(SECTOR_PREFIX + "*");
        invalidateSectorLists();
        log.debug("All sector caches invalidated");
    }

    private void invalidateSectorLists() {
        redisService.delete(ALL_SECTORS_KEY);
        redisService.delete(ACTIVE_SECTORS_KEY);
        log.debug("Sector lists cache invalidated");
    }
}