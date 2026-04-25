package com.example.nidar.heatmap.service;

import com.example.nidar.heatmap.service.NcrbDataService.NcrbRecord;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class NcrbExcelParser {

    // Parse the actual NCRB Excel file
    // Download from: https://ncrb.gov.in/crime-in-india-table-addl.html
    public List<NcrbRecord> parseExcel(InputStream excelStream)
        throws IOException {

        List<NcrbRecord> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(excelStream)) {
            // NCRB Excel has multiple sheets — find "Crime Against Women"
            Sheet sheet = workbook.getSheet("Table 3A.1");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            // Skip header rows (usually first 3 rows)
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String district  = getCellValue(row, 0);
                    String crimeType = getCellValue(row, 2);
                    int    count     = (int) row.getCell(3).getNumericCellValue();
                    int    year      = 2022; // hardcode year from filename

                    if (!district.isBlank() && !crimeType.isBlank() && count > 0) {
                        records.add(new NcrbRecord(
                            district, crimeType, year, count
                        ));
                    }
                } catch (Exception e) {
                    // Skip malformed rows
                    log.debug("Skipping row {}: {}", i, e.getMessage());
                }
            }
        }

        log.info("Parsed {} NCRB records from Excel", records.size());
        return records;
    }

    private String getCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default      -> "";
        };
    }
}
