package com.example.UsageTrackAPI.repository;

import com.example.UsageTrackAPI.model.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {
    List<UsageLog> findByLicenseCode(String licenseCode);
}
