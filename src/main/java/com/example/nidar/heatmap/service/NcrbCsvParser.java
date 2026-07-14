package com.example.nidar.heatmap.service;

import com.example.nidar.heatmap.service.NcrbDataService.NcrbRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class NcrbCsvParser {

    /**
     * Parses a CSV file containing NCRB data.
     * Expected columns: District, CrimeType, Year, Count
     * Example row: Delhi, Rape, 2022, 1340
     */
    public List<NcrbRecord> parseCsv(InputStream csvStream) throws IOException {
        List<NcrbRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstRow = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Skip header row
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                // Simple split by comma, handling potential surrounding spaces
                String[] columns = line.split(",");
                if (columns.length >= 4) {
                    try {
                        String district = columns[0].trim().replace("\"", "");
                        String crimeType = columns[1].trim().replace("\"", "");
                        int year = Integer.parseInt(columns[2].trim().replace("\"", ""));
                        int count = Integer.parseInt(columns[3].trim().replace("\"", ""));

                        if (!district.isBlank() && !crimeType.isBlank() && count > 0) {
                            records.add(new NcrbRecord(district, crimeType, year, count));
                        }
                    } catch (NumberFormatException e) {
                        log.debug("Skipping invalid row (number format issue): {}", line);
                    }
                } else {
                    log.debug("Skipping row with insufficient columns: {}", line);
                }
            }
        }

        log.info("Parsed {} NCRB records from CSV", records.size());
        return records;
    }
}
