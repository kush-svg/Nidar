package com.example.nidar.heatmap.controller;

import com.example.nidar.heatmap.service.HeatmapCacheService;
import com.example.nidar.heatmap.service.NcrbDataService;
import com.example.nidar.heatmap.service.NcrbExcelParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/heatmap")
@RequiredArgsConstructor
@Slf4j
public class AdminHeatmapController {

    private final NcrbDataService    ncrbDataService;
    private final NcrbExcelParser    ncrbExcelParser;
    private final HeatmapCacheService cacheService;

    // Trigger manual NCRB sync
    @PostMapping("/sync-ncrb")
    public ResponseEntity<String> syncNcrb() {
        ncrbDataService.ingestNcrbData();
        cacheService.invalidateAll();
        return ResponseEntity.ok("NCRB sync completed successfully");
    }

    // Upload NCRB Excel file directly
    @PostMapping("/upload-ncrb-excel")
    public ResponseEntity<String> uploadNcrbExcel(
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        List<NcrbDataService.NcrbRecord> records =
            ncrbExcelParser.parseExcel(file.getInputStream());

        ncrbDataService.ingestParsedRecords(records);
        cacheService.invalidateAll();

        return ResponseEntity.ok(
            "Ingested " + records.size() + " NCRB records"
        );
    }

    // Clear all NCRB data and re-sync
    @DeleteMapping("/clear-ncrb")
    public ResponseEntity<String> clearNcrbData() {
        ncrbDataService.clearNcrbData();
        cacheService.invalidateAll();
        return ResponseEntity.ok("NCRB data cleared");
    }
}
