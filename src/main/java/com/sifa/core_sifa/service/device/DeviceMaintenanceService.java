package com.sifa.core_sifa.service.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceMaintenanceService {

    private final IDeviceTokenService deviceTokenService;

    @Scheduled(cron = "0 */5 * * * *")
    public void markInactiveDevices() {
        log.info("Running device status cleanup...");
        int affected = deviceTokenService.cleanupStaleDevices();
        if (affected > 0) {
            log.info("Device cleanup completed: {} devices affected", affected);
        }
    }
}
