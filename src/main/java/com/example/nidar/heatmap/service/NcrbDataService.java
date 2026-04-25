package com.example.nidar.heatmap.service;

import com.example.nidar.common.util.H3SnapUtil;
import com.example.nidar.heatmap.model.Incident;
import com.example.nidar.heatmap.model.IncidentType;
import com.example.nidar.heatmap.repository.IncidentRepository;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NcrbDataService {

    private final IncidentRepository incidentRepository;
    private final H3SnapUtil         h3SnapUtil;

    // H3Core is thread-safe after initialisation — create once, reuse
    private static final H3Core H3;
    static {
        try {
            H3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Could not initialise H3Core: " + e.getMessage());
        }
    }

    // District centroids for NCR — NCRB reports at district level
    // We map each district to its centroid coordinates
    private static final Map<String, double[]> DISTRICT_CENTROIDS = Map.of(
        "Faridabad",  new double[]{28.4089, 77.3178},
        "Gurugram",   new double[]{28.4595, 77.0266},
        "Delhi",      new double[]{28.6139, 77.2090},
        "Noida",      new double[]{28.5355, 77.3910},
        "Ghaziabad",  new double[]{28.6692, 77.4538},
        "Meerut",     new double[]{28.9845, 77.7064},
        "Agra",       new double[]{27.1767, 78.0081},
        "Lucknow",    new double[]{26.8467, 80.9462}
    );

    // NCRB crime type → our IncidentType mapping
    private static final Map<String, IncidentType> NCRB_CRIME_MAP = Map.of(
        "Rape",                    IncidentType.SOS_TRIGGER,
        "Assault on women",        IncidentType.PHYSICAL_HARASSMENT,
        "Kidnapping of women",     IncidentType.SOS_TRIGGER,
        "Stalking",                IncidentType.STALKING,
        "Eve-teasing",             IncidentType.PHYSICAL_HARASSMENT,
        "Cruelty by husband",      IncidentType.PHYSICAL_HARASSMENT,
        "Insult to modesty",       IncidentType.STALKING
    );

    // Scheduled to run every year on Jan 1 when NCRB releases new data
    @Scheduled(cron = "0 0 0 1 1 *")
    public void syncNcrbData() {
        log.info("Starting annual NCRB data sync...");
        ingestNcrbData();
    }

    // Removes all NCRB-seeded incidents — called by admin clear endpoint
    public void clearNcrbData() {
        incidentRepository.deleteByUserId("ncrb-system");
        log.info("NCRB data cleared from DB.");
    }

    // Triggered manually via admin endpoint (uses pre-parsed dataset)
    public void ingestNcrbData() {
        ingestParsedRecords(fetchNcrbRecords());
    }

    // Triggered via admin upload endpoint (uses caller-supplied records)
    public void ingestParsedRecords(List<NcrbRecord> records) {
        try {
            int inserted = 0;
            for (NcrbRecord record : records) {
                double[] centroid = DISTRICT_CENTROIDS.get(record.district());
                if (centroid == null) {
                    log.debug("No centroid mapping for district '{}' — skipping", record.district());
                    continue;
                }

                // Distribute incidents across H3 cells in the district
                // instead of a single point — more realistic spatial spread
                List<Incident> incidents = distributeAcrossDistrict(
                    record, centroid[0], centroid[1]
                );

                incidentRepository.saveAll(incidents);
                inserted += incidents.size();
            }

            log.info("NCRB sync complete. Inserted {} incidents.", inserted);

        } catch (Exception e) {
            log.error("NCRB sync failed: {}", e.getMessage(), e);
        }
    }

    // Distributes district-level crime count into multiple H3 cells.
    // Gives spatial spread rather than one big spike at the centroid.
    private List<Incident> distributeAcrossDistrict(
        NcrbRecord record,
        double     centerLat,
        double     centerLng
    ) {
        List<Incident> incidents = new ArrayList<>();
        IncidentType   type      = NCRB_CRIME_MAP.getOrDefault(
            record.crimeType(), IncidentType.SUSPICIOUS_ACTIVITY
        );

        // Get center cell + 2 rings of neighbors
        String       centerCell = h3SnapUtil.cellIndex(centerLat, centerLng);
        List<String> cells      = new ArrayList<>(h3SnapUtil.getNeighborCells(centerCell, 2));
        cells.add(centerCell);

        // Distribute crime count evenly across cells
        int perCell = Math.max(1, record.crimeCount() / cells.size());

        for (String cell : cells) {
            try {
                LatLng center = H3.cellToLatLng(cell);

                // Add spatial jitter within the cell for natural spread
                for (int i = 0; i < Math.min(perCell, 5); i++) {
                    double jitterLat = center.lat + (Math.random() - 0.5) * 0.01;
                    double jitterLng = center.lng + (Math.random() - 0.5) * 0.01;

                    incidents.add(Incident.builder()
                        .userId("ncrb-system")
                        .latitude(jitterLat)
                        .longitude(jitterLng)
                        .h3Index(cell)
                        .incidentType(type)
                        .baseWeight(type.getBaseWeight())
                        .trustScore(0.8)  // NCRB data is considered trusted
                        .timestamp(ncrbYearToEpoch(record.year()))
                        .build()
                    );
                }
            } catch (Exception e) {
                log.warn("H3 cell error for cell {}: {}", cell, e.getMessage());
            }
        }

        return incidents;
    }

    private long ncrbYearToEpoch(int year) {
        // Map NCRB year to mid-year epoch so time decay is applied naturally
        return LocalDate.of(year, 6, 15)
            .atStartOfDay(ZoneOffset.UTC)
            .toEpochSecond();
    }

    // Fetch NCRB records — parse from Excel in production
    private List<NcrbRecord> fetchNcrbRecords() {
        // Pre-parsed 2022 NCRB data for NCR districts
        // In production: use NcrbExcelParser to parse the real Excel file
        return List.of(
            new NcrbRecord("Faridabad", "Assault on women",     2022, 423),
            new NcrbRecord("Faridabad", "Stalking",             2022, 187),
            new NcrbRecord("Faridabad", "Eve-teasing",          2022, 312),
            new NcrbRecord("Faridabad", "Rape",                 2022,  89),
            new NcrbRecord("Gurugram",  "Assault on women",     2022, 512),
            new NcrbRecord("Gurugram",  "Stalking",             2022, 234),
            new NcrbRecord("Gurugram",  "Rape",                 2022, 104),
            new NcrbRecord("Delhi",     "Assault on women",     2022, 4921),
            new NcrbRecord("Delhi",     "Stalking",             2022, 1823),
            new NcrbRecord("Delhi",     "Rape",                 2022, 1340),
            new NcrbRecord("Delhi",     "Kidnapping of women",  2022,  892),
            new NcrbRecord("Noida",     "Assault on women",     2022, 389),
            new NcrbRecord("Noida",     "Eve-teasing",          2022, 201),
            new NcrbRecord("Ghaziabad", "Assault on women",     2022, 445),
            new NcrbRecord("Ghaziabad", "Stalking",             2022, 198)
        );
    }

    public record NcrbRecord(
        String district,
        String crimeType,
        int    year,
        int    crimeCount
    ) {}
}