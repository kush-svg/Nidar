package com.example.nidar.common.messaging;

import com.example.nidar.heatmap.service.HeatmapCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentAlertHandler {

    private final HeatmapCacheService heatmapCacheService;

    public void handle(Map<String, String> data) {
        String h3Index = data.get("h3Index");
        if (h3Index == null) return;

        heatmapCacheService.invalidateArea(h3Index);
        log.debug("Heatmap cache invalidated for h3: {}", h3Index);
    }
}