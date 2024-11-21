package com.example.UsageTrackAPI;

import com.example.UsageTrackAPI.model.Status;
import com.example.UsageTrackAPI.model.UsageLog;
import com.example.UsageTrackAPI.repository.UsageLogRepository;
import com.example.UsageTrackAPI.service.UsageLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UsageLogServiceTest {
    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UsageLogService usageLogService;

    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    @Value("${license_API}")
    private String license_API_url;

    private UsageLog testUsageLog;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        testUsageLog = new UsageLog();
        testUsageLog.setId(1L);
        testUsageLog.setLicenseCode("1234");
        testUsageLog.setBinNumber("1234567890123");
        testUsageLog.setSadNumber("SAD123");
        testUsageLog.setStatus(Status.USED);
        testUsageLog.setUsageDate(LocalDateTime.now());
    }

    @Test
    public void testFindAllUsageLogs() {
        when(usageLogRepository.findAll()).thenReturn(List.of(testUsageLog));

        List<UsageLog> result = usageLogService.findAllUsageLogs();

        assertEquals(1, result.size());
        assertEquals(testUsageLog, result.get(0));
        verify(usageLogRepository, times(1)).findAll();
    }

    @Test
    public void testFindUsageLogsByLicenseCode() {
        String licenseCode = "1234";
        when(usageLogRepository.findByLicenseCode(licenseCode)).thenReturn(List.of(testUsageLog));

        List<UsageLog> result = usageLogService.findUsageLogsByLicenseCode(licenseCode);

        assertEquals(1, result.size());
        assertEquals(licenseCode, result.get(0).getLicenseCode());
        verify(usageLogRepository, times(1)).findByLicenseCode(licenseCode);
    }

    @Test
    public void testGetUsageLogsByStatus() {
        when(usageLogRepository.findAll()).thenReturn(List.of(testUsageLog));

        List<UsageLog> result = usageLogService.getUsageLogsByStatus(Status.USED);

        assertEquals(1, result.size());
        assertEquals(Status.USED, result.get(0).getStatus());
        verify(usageLogRepository, times(1)).findAll();
    }

    @Test
    public void testSaveUsageLog_Success() {
        String licenseCode = "1234";
        String binNumber = "1234567890123";
        String sadNumber = "456123";

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class), anyMap()))
                .thenReturn(new ResponseEntity<>(Map.of("isValid", "true"), HttpStatus.OK));
        when(usageLogRepository.save(any(UsageLog.class))).thenReturn(testUsageLog);

        UsageLog result = usageLogService.saveUsageLog(licenseCode, binNumber, sadNumber);

        assertNotNull(result);
        assertEquals(Status.USED, result.getStatus());
        verify(usageLogRepository, times(1)).save(any(UsageLog.class));
    }

    @Test
    public void testSaveUsageLog_InvalidLicense() {
        String licenseCode = "1234";
        String binNumber = "1234567890123";
        String sadNumber = "SAD123";

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class), anyMap()))
                .thenThrow(HttpClientErrorException.class);

        assertThrows(RuntimeException.class, () -> usageLogService.saveUsageLog(licenseCode, binNumber, sadNumber));
        verify(usageLogRepository, never()).save(any(UsageLog.class));
    }

    @Test
    public void testValidateLicense_Success() {
        String licenseCode = "1234";
        String binNumber = "1234567890123";

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class), anyMap()))
                .thenReturn(new ResponseEntity<>(Map.of("isValid", "true"), HttpStatus.OK));

        boolean isValid = usageLogService.validateLicense(licenseCode, binNumber);

        assertTrue(isValid);
    }

    @Test
    public void testValidateLicense_HttpClientError() {
        String licenseCode = "1234";
        String binNumber = "1234567890123";

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class), anyMap()))
                .thenThrow(HttpClientErrorException.class);

        boolean isValid = usageLogService.validateLicense(licenseCode, binNumber);

        assertFalse(isValid);
    }

    @Test
    public void testDeactivateLicenseAfterOneTimeUse_Success() {
        String licenseCode = "1234";

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class), anyMap()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertDoesNotThrow(() -> usageLogService.deactivateLicenseAfterOneTimeUse(licenseCode));
    }

    @Test
    public void testDeactivateLicenseAfterOneTimeUse_HttpClientError() {
        String licenseCode = "1234";

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class), anyMap()))
                .thenThrow(HttpClientErrorException.class);

        assertThrows(RuntimeException.class, () -> usageLogService.deactivateLicenseAfterOneTimeUse(licenseCode));
    }

    @Test
    public void testIsValidBinNumber_Valid() {
        String validBinNumber = "1234567890123";

        assertTrue(usageLogService.isValidBinNumber(validBinNumber));
    }

    @Test
    public void testIsValidBinNumber_Invalid() {
        String invalidBinNumber = "12345";

        assertFalse(usageLogService.isValidBinNumber(invalidBinNumber));
    }

    @Test
    public void testIsValidLicenseCode_Valid() {
        String validLicenseCode = "1234";

        assertTrue(usageLogService.isValidLicenseCode(validLicenseCode));
    }

    @Test
    public void testIsValidLicenseCode_Invalid() {
        String invalidLicenseCode = "12";

        assertFalse(usageLogService.isValidLicenseCode(invalidLicenseCode));
    }
}
