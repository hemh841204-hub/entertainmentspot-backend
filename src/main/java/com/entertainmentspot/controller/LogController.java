package com.entertainmentspot.controller;

import com.entertainmentspot.entity.OperationLog;
import com.entertainmentspot.repository.OperationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final OperationLogRepository repo;
    private final ObjectMapper objectMapper;

    public LogController(OperationLogRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody OperationLog log, HttpServletRequest request) {
        return saveLog(log, request);
    }

    // sendBeacon sometimes sends as text/plain
    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> createFromText(@RequestBody String body, HttpServletRequest request) {
        try {
            OperationLog log = objectMapper.readValue(body, OperationLog.class);
            return saveLog(log, request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid JSON");
        }
    }

    private ResponseEntity<?> saveLog(OperationLog log, HttpServletRequest request) {
        try {
            if (log.getUserIp() == null || log.getUserIp().isBlank()) {
                String ip = request.getHeader("X-Real-IP");
                if (ip == null) ip = request.getRemoteAddr();
                log.setUserIp(ip);
            }
            return ResponseEntity.ok(repo.save(log));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving log: " + e.getMessage());
        }
    }
}
