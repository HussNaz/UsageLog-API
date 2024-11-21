package com.example.UsageTrackAPI.service;

import com.example.UsageTrackAPI.exceptions.LicenseValidationFailedException;
import com.example.UsageTrackAPI.model.UsageLog;
import com.example.UsageTrackAPI.repository.UsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.example.UsageTrackAPI.model.Status;

import java.time.LocalDateTime;
import java.util.Base64;
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

    private HttpStatus httpStatus;

//    private final WebClient.Builder webClientBuilder;

    private final UsageLogRepository usageLogRepository;
    private final RestTemplate restTemplate;

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

        if (!isValidBinNumber(binNumber)) {
            throw new IllegalArgumentException("Bin Number is not valid: " + binNumber);
        }
        if (!isValidLicenseCode(licenseCode)) {
            throw new IllegalArgumentException("License Code is not valid: " + licenseCode);
        }
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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = username+":"+password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            String url = license_API_url + "/validateByBinAndLicenseCode/{licenseCode}?binNumber={binNumber}";

            Map<String, String> uriVariables = Map.of(
                    "licenseCode", licenseCode,
                    "binNumber", binNumber
            );


            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class,
                    uriVariables
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map responseBody = response.getBody();
                assert responseBody != null;
                return "true".equalsIgnoreCase((String) responseBody.get("isValid"));
            } else {
                throw new LicenseValidationFailedException("License validation failed: HTTP status " + response.getStatusCode());
            }
        } catch (HttpClientErrorException ex) {
            System.err.println("HTTP error during license validation: " + ex.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error during license validation: " + e.getMessage());
            return false;
        }
    }



    private void deactivateLicenseAfterOneTimeUse(String licenseCode) {
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);


            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);

            String url = license_API_url + "/deactivate/{licenseCode}";

            Map<String, String> uriVariables = Map.of("licenseCode", licenseCode);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);


            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Void.class,
                    uriVariables
            );


            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("License successfully deactivated.");
            } else {
                throw new RuntimeException("Failed to deactivate the license. HTTP Status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException ex) {
            System.err.println("HTTP error during license deactivation: " + ex.getResponseBodyAsString());
            throw new RuntimeException("Error while deactivating one-time use license: " + ex.getMessage(), ex);
        } catch (Exception e) {
            System.err.println("General error during license deactivation: " + e.getMessage());
            throw new RuntimeException("Error while deactivating one-time use license: " + e.getMessage(), e);
        }
    }

    private boolean isValidBinNumber(String binNumber) {
        return binNumber.matches("^\\d{13}$");
    }

    private boolean isValidLicenseCode(String licenseCode) {
        return licenseCode.matches("\\d{4}");
    }

}
