package com.example.UsageTrackAPI.service;

import com.example.UsageTrackAPI.errorResponse.ErrorResponse;
import com.example.UsageTrackAPI.exceptions.LicenseValidationFailedException;
import com.example.UsageTrackAPI.model.UsageLog;
import com.example.UsageTrackAPI.repository.UsageLogRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.UsageTrackAPI.model.Status;
import reactor.core.publisher.Mono;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsageLogService {

    @Value("${spring.security.user.name}")
    private String username;
    @Value("${spring.security.user.password}")
    private String password;

    @Value("${license_API}")
    private String license_API_url;

    private final WebClient.Builder webClientBuilder;

    private final UsageLogRepository usageLogRepository;

    public List<UsageLog> findAllUsageLogs() {
        return usageLogRepository.findAll();
    }

    public List<UsageLog> findUsageLogsByLicenseCode(String licenseCode) {
        return usageLogRepository.findByLicenseCode(licenseCode);
    }

    public List<UsageLog> getUsageLogsByStatus(Status status) {
        return usageLogRepository.findAll()
                .stream()
                .filter(usageLog -> usageLog.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    public UsageLog saveUsageLog(String licenseCode, String binNumber, String sadNumber) {

        boolean isValid = validateLicense(licenseCode, binNumber);
        if (!isValid) {
            throw new RuntimeException("License validation failed for License Code: " + licenseCode);
        }

        UsageLog usageLog = new UsageLog();
        usageLog.setBinNumber(binNumber);
        usageLog.setSadNumber(sadNumber);
        usageLog.setLicenseCode(licenseCode);
        usageLog.setStatus(Status.USED);
        usageLog.setUsageDate(LocalDateTime.now());

        UsageLog savedLog = usageLogRepository.save(usageLog);

        deactivateLicenseAfterOneTimeUse(licenseCode);

        return savedLog;
    }

    private boolean validateLicense(String licenseCode, String binNumber) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(license_API_url)
                    .defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username, password))
                    .build();

            Map<String, String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/validateByBinAndLicenseCode/{licenseCode}")
                            .queryParam("binNumber", binNumber)
                            .build(licenseCode))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                    })
                    .block();

            if (response != null && "true".equalsIgnoreCase(response.get("isValid"))) {
                return true;
            } else {
                throw new LicenseValidationFailedException("License validation failed.");
            }
        } catch (Exception e) {
            System.err.println("Error during license validation: " + e.getMessage());
            return false;
        }
    }

    private void deactivateLicenseAfterOneTimeUse(String licenseCode) {
        try {

            WebClient webClient = webClientBuilder.baseUrl(license_API_url).build();

            String endpoint = String.format("/deactivate/%s", licenseCode);

            webClient.put()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("Error while deactivating one-time use license: " + e.getMessage(), e);
        }
    }

}
