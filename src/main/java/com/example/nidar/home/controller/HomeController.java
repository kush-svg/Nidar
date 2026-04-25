package com.example.nidar.home.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.example.nidar.home.service.HomeService;
import com.example.nidar.home.dto.HomeSummaryDto;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/summary")
    public ResponseEntity<HomeSummaryDto> getSummary(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(homeService.getSummary(lat, lng));
    }
}
