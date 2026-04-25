package com.example.nidar.heatmap.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.nidar.heatmap.dto.BoundingBox;
import com.example.nidar.heatmap.dto.WeightedClusterDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapCacheService {

    private static final Duration TTL        = Duration.ofMinutes(5);
    private static final String   KEY_PREFIX = "heatmap:";

    // TypeReference captures List<WeightedClusterDto> generic type for Jackson
    private static final TypeReference<List<WeightedClusterDto>> CLUSTER_LIST_TYPE =
        new TypeReference<>() {};

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                  objectMapper;

    // ── Key builder — BoundingBox variant (used by HeatmapController) ────────
    public String buildCacheKey(BoundingBox bbox) {
        return String.format(
            KEY_PREFIX + "%.3f:%.3f:%.3f:%.3f",
            bbox.minLat(), bbox.maxLat(),
            bbox.minLng(), bbox.maxLng()
        );
    }

    // ── Key builder — H3 cell list variant (used by HomeService) ─────────────
    // Sort cells so the key is stable regardless of insertion order
    public String buildCacheKey(List<String> cells) {
        return KEY_PREFIX + "cells:" + cells.stream().sorted().collect(java.util.stream.Collectors.joining(","));
    }

    // ── GET — JSON → List<WeightedClusterDto> ─────────────────────────────────
    public Optional<List<WeightedClusterDto>> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("Heatmap cache MISS  key={}", key);
                return Optional.empty();
            }
            List<WeightedClusterDto> clusters = objectMapper.readValue(json, CLUSTER_LIST_TYPE);
            log.debug("Heatmap cache HIT   key={} clusters={}", key, clusters.size());
            return Optional.of(clusters);
        } catch (Exception e) {
            // Redis down or corrupted value — degrade gracefully, recompute from DB
            log.warn("Heatmap cache GET failed key={} reason={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    // ── PUT — List<WeightedClusterDto> → JSON, stored with TTL ───────────────
    public void put(String key, List<WeightedClusterDto> clusters) {
        try {
            String json = objectMapper.writeValueAsString(clusters);
            redisTemplate.opsForValue().set(key, json, TTL);
            log.debug("Heatmap cache PUT    key={} clusters={} ttl={}", key, clusters.size(), TTL);
        } catch (Exception e) {
            // Redis down or serialisation error — continue without caching
            log.warn("Heatmap cache PUT failed key={} reason={}", key, e.getMessage());
        }
    }

    // ── EVICT — invalidate a key when new incidents arrive in that bbox ───────
    public void evict(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Heatmap cache EVICT  key={} deleted={}", key, deleted);
        } catch (Exception e) {
            log.warn("Heatmap cache EVICT failed key={} reason={}", key, e.getMessage());
        }
    }

    // ── EVICT by H3 cell — called from IncidentService after a new report ────
    // Only evicts keys that contain this specific cell — not the whole cache
    public void invalidateArea(String h3Index) {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*" + h3Index + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Heatmap cache EVICT area h3={} keys={}", h3Index, keys.size());
            }
        } catch (Exception e) {
            log.warn("Heatmap cache EVICT area failed h3={} reason={}", h3Index, e.getMessage());
        }
    }

    // ── EVICT ALL — called by admin after a full sync or clear ───────────────
    public void invalidateAll() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Heatmap cache EVICT ALL — {} keys removed", keys.size());
            }
        } catch (Exception e) {
            log.warn("Heatmap cache EVICT ALL failed reason={}", e.getMessage());
        }
    }
}