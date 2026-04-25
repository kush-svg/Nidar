package com.example.nidar.common.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public GeocodingService(StringRedisTemplate redisTemplate) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.redisTemplate = redisTemplate;
    }

    public String reverseGeocode(double lat, double lng) {
        // Round to 3 decimal places to improve cache hit rate (approx 100m accuracy)
        double roundedLat = Math.round(lat * 1000.0) / 1000.0;
        double roundedLng = Math.round(lng * 1000.0) / 1000.0;
        String cacheKey = String.format("nidar:geocode:%.3f:%.3f", roundedLat, roundedLng);

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis cache error for geocoding", e);
        }

        String location = "Unknown Location";
        try {
            String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=14", lat, lng);
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NidarApp/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode address = root.path("address");
                
                String neighborhood = address.path("neighbourhood").asText("");
                String suburb = address.path("suburb").asText("");
                String city = address.path("city").asText(address.path("town").asText(address.path("village").asText("")));
                String county = address.path("county").asText("");

                StringBuilder sb = new StringBuilder();
                if (!neighborhood.isEmpty()) { sb.append(neighborhood).append(", "); }
                else if (!suburb.isEmpty()) { sb.append(suburb).append(", "); }
                
                if (!city.isEmpty()) { sb.append(city); }
                else if (!county.isEmpty()) { sb.append(county); }
                
                String result = sb.toString();
                if (result.endsWith(", ")) {
                    result = result.substring(0, result.length() - 2);
                }
                
                if (!result.isEmpty()) {
                    location = result;
                }
            }
        } catch (Exception e) {
            log.error("Failed to reverse geocode lat: {} lng: {}", lat, lng, e);
        }

        try {
            // Cache for 30 days
            redisTemplate.opsForValue().set(cacheKey, location, Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("Redis cache error for geocoding", e);
        }

        return location;
    }
}
