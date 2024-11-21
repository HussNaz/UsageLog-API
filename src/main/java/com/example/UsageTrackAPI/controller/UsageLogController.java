package com.example.UsageTrackAPI.controller;

import com.example.UsageTrackAPI.exceptions.UsageLogNotFoundException;
import com.example.UsageTrackAPI.model.Status;
import com.example.UsageTrackAPI.model.UsageLog;
import com.example.UsageTrackAPI.service.UsageLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UsageLogController {

    private final UsageLogService usageLogService;

    @GetMapping("/getAllUsageLogs")
    public ResponseEntity<List<UsageLog>> getAllUsageLogs() {
        List<UsageLog> usageLogs = usageLogService.findAllUsageLogs();
        return ResponseEntity.ok(usageLogs);
    }

    @GetMapping("/license/{licenseCode}")
    public ResponseEntity<List<UsageLog>> getUsageLogsByLicenseCode(String licenseCode) {
        List<UsageLog> usageLogs = usageLogService.findUsageLogsByLicenseCode(licenseCode);
        if (usageLogs.isEmpty()) {
            throw new UsageLogNotFoundException("No usage logs found for license code: " + licenseCode);
        }
        return ResponseEntity.ok(usageLogs);
    }

    @GetMapping("/status")
    public ResponseEntity<List<UsageLog>> getUsageLogsByStatus(@RequestParam Status status) {
        List<UsageLog> usageLogs = usageLogService.getUsageLogsByStatus(status);
        return ResponseEntity.ok(usageLogs);
    }

    @PostMapping("/usageLogsRegistration")
    public ResponseEntity<UsageLog> saveUsageLog(@Valid @RequestBody UsageLog usageLog) {
        try {
            UsageLog savedUsageLog = usageLogService.saveUsageLog(usageLog.getLicenseCode(), usageLog.getBinNumber(), usageLog.getSadNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUsageLog);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}

