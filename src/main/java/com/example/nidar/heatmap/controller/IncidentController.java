package com.example.nidar.heatmap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.nidar.heatmap.dto.IncidentReportRequest;
import com.example.nidar.heatmap.service.IncidentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping("/report")
    public ResponseEntity<String> report(
        @Valid @RequestBody IncidentReportRequest request,
        @RequestHeader("Authorization") String authHeader
    ) {
        // Extract userId from JWT — done by SecurityContext
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        incidentService.report(request, userId);
        return ResponseEntity.ok("Incident reported successfully");
    }
}
