package com.example.nidar.sos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.example.nidar.sos.dto.LocationUpdateRequest;
import com.example.nidar.sos.dto.SosTriggerRequest;
import com.example.nidar.sos.dto.SosTriggerResponse;
import com.example.nidar.sos.service.SosService;


// sos/controller/SosController.java
@RestController
@RequestMapping("/api/v1/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;

    @PostMapping("/trigger")
    public ResponseEntity<SosTriggerResponse> trigger(
        @Valid @RequestBody SosTriggerRequest request
    ) {
        return ResponseEntity.ok(sosService.triggerSos(request));
    }

    @PostMapping("/{sessionId}/location")
    public ResponseEntity<Void> updateLocation(
        @PathVariable String sessionId,
        @Valid @RequestBody LocationUpdateRequest request
    ) {
        sosService.updateLocation(sessionId, request);
        return ResponseEntity.ok().build();
    }
}
