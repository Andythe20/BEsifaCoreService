package com.sifa.core_sifa.service.device;

import com.sifa.core_sifa.dto.device.DeviceRegisterRequest;
import com.sifa.core_sifa.model.DeviceToken;
import com.sifa.core_sifa.repository.IDeviceTokenRepository;
import com.sifa.core_sifa.service.push.IPushService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements IDeviceTokenService {

    private final IDeviceTokenRepository deviceTokenRepository;
    private final IPushService pushService;

    @Override
    @Transactional
    public void register(String emailUsuario, DeviceRegisterRequest request) {
        log.info("Registrando dispositivo para usuario: {}, platform: {}, appVersion: {}", emailUsuario, request.getPlatform(), request.getAppVersion());

        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.getToken())
                .map(existing -> {
                    existing.setEmailUsuario(emailUsuario);
                    existing.setPlatform(request.getPlatform());
                    existing.setAppVersion(request.getAppVersion());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .emailUsuario(emailUsuario)
                        .token(request.getToken())
                        .platform(request.getPlatform())
                        .appVersion(request.getAppVersion())
                        .build());

        deviceTokenRepository.save(deviceToken);
        log.info("Dispositivo registrado/actualizado exitosamente para: {}", emailUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public int notifyOutdatedDevices(String currentVersion, String title, String body) {
        List<DeviceToken> outdated = deviceTokenRepository.findByAppVersionNot(currentVersion);

        if (outdated.isEmpty()) {
            log.info("No outdated devices found (current version: {})", currentVersion);
            return 0;
        }

        log.info("Sending notification to {} outdated devices (current version: {})",
                outdated.size(), currentVersion);

        int successCount = 0;
        for (DeviceToken device : outdated) {
            try {
                pushService.send(device.getToken(), title, body);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send notification to device {} (user: {}): {}",
                        device.getToken(), device.getEmailUsuario(), e.getMessage());
            }
        }

        log.info("Notification sent to {}/{} outdated devices", successCount, outdated.size());
        return successCount;
    }
}
