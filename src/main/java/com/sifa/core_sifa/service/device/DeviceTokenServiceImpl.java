package com.sifa.core_sifa.service.device;

import com.sifa.core_sifa.dto.device.DeviceRegisterRequest;
import com.sifa.core_sifa.dto.device.DeviceTokenResponse;
import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.DeviceToken;
import com.sifa.core_sifa.repository.IDeviceTokenRepository;
import com.sifa.core_sifa.service.push.IPushService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        String appVersion = request.getAppVersion() != null ? request.getAppVersion() : "unknown";
        log.info("Registrando dispositivo para usuario: {}, platform: {}, appVersion: {}",
                emailUsuario, request.getPlatform(), appVersion);

        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.getToken())
                .map(existing -> {
                    existing.setEmailUsuario(emailUsuario);
                    existing.setPlatform(request.getPlatform());
                    existing.setAppVersion(appVersion);
                    if (request.getDeviceId() != null) existing.setDeviceId(request.getDeviceId());
                    if (request.getDeviceModel() != null) existing.setDeviceModel(request.getDeviceModel());
                    if (request.getManufacturer() != null) existing.setManufacturer(request.getManufacturer());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .emailUsuario(emailUsuario)
                        .token(request.getToken())
                        .platform(request.getPlatform())
                        .appVersion(appVersion)
                        .deviceId(request.getDeviceId())
                        .deviceModel(request.getDeviceModel())
                        .manufacturer(request.getManufacturer())
                        .build());

        deviceTokenRepository.save(deviceToken);
        log.info("Dispositivo registrado/actualizado exitosamente para: {}", emailUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceToken> getAllDevices() {
        return deviceTokenRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getAllDeviceResponses() {
        return deviceTokenRepository.findAll().stream()
                .map(DeviceTokenResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void processHeartbeat(String emailUsuario, FiscalizadorHeartbeatRequest request) {
        Optional<DeviceToken> existing = deviceTokenRepository.findByDeviceId(request.getDeviceId());

        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            token.setLastHeartbeatAt(LocalDateTime.now());
            token.setStatus("ACTIVE");
            token.setDeviceModel(request.getModelo());
            token.setManufacturer(request.getMarca());
            if (token.getDeviceId() == null) token.setDeviceId(request.getDeviceId());
            if (request.getFcmToken() != null) token.setToken(request.getFcmToken());
            deviceTokenRepository.save(token);
            log.debug("Device token {} updated from heartbeat (user: {})", token.getId(), emailUsuario);
            return;
        }

        // Buscar por email
        List<DeviceToken> byEmail = deviceTokenRepository.findByEmailUsuario(emailUsuario);
        DeviceToken match = byEmail.stream()
                .filter(t -> t.getDeviceId() == null)
                .findFirst()
                .orElseGet(() -> byEmail.isEmpty() ? null : byEmail.get(0));

        if (match != null) {
            match.setDeviceId(request.getDeviceId());
            match.setDeviceModel(request.getModelo());
            match.setManufacturer(request.getMarca());
            match.setLastHeartbeatAt(LocalDateTime.now());
            match.setStatus("ACTIVE");
            if (request.getFcmToken() != null) match.setToken(request.getFcmToken());
            deviceTokenRepository.save(match);
            log.info("Linked deviceId {} to token {} (user: {})", request.getDeviceId(), match.getId(), emailUsuario);
            return;
        }

        // No existe ningún DeviceToken para este dispositivo o email.
        // Si el heartbeat trae un fcmToken, auto-registramos el dispositivo.
        if (request.getFcmToken() != null) {
            DeviceToken nuevo = DeviceToken.builder()
                    .emailUsuario(emailUsuario)
                    .token(request.getFcmToken())
                    .platform("ANDROID")
                    .appVersion("unknown")
                    .deviceId(request.getDeviceId())
                    .deviceModel(request.getModelo())
                    .manufacturer(request.getMarca())
                    .status("ACTIVE")
                    .lastHeartbeatAt(LocalDateTime.now())
                    .build();
            deviceTokenRepository.save(nuevo);
            log.warn("Auto-registrado nuevo DeviceToken {} para usuario {} desde heartbeat (fcmToken provisto)", nuevo.getId(), emailUsuario);
        } else {
            log.warn("Heartbeat recibido para usuario {} (deviceId {}) pero NO existe DeviceToken ni se puede crear (falta fcmToken). " +
                     "El dispositivo NO recibirá notificaciones push hasta que la app móvil registre su FCM token.", emailUsuario, request.getDeviceId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDeviceStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", deviceTokenRepository.count());
        stats.put("active", deviceTokenRepository.countByStatus("ACTIVE"));
        stats.put("unknown", deviceTokenRepository.countByStatus("UNKNOWN"));
        stats.put("inactive", deviceTokenRepository.countByStatus("INACTIVE"));
        return stats;
    }

    @Override
    @Transactional
    public int cleanupStaleDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        int updated = deviceTokenRepository.markInactiveSince("INACTIVE", threshold);

        log.info("Cleanup: marked {} devices as INACTIVE", updated);
        return updated;
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

        return sendToAll(outdated, title, body);
    }

    @Override
    @Transactional(readOnly = true)
    public int notifyAllDevices(String title, String body) {
        List<DeviceToken> all = deviceTokenRepository.findAll();

        if (all.isEmpty()) {
            log.info("No registered devices found");
            return 0;
        }

        log.info("Sending notification to all {} registered devices", all.size());

        return sendToAll(all, title, body);
    }

    @Override
    @Transactional(readOnly = true)
    public int notifyByPlatform(String platform, String title, String body) {
        List<DeviceToken> byPlatform = deviceTokenRepository.findByPlatform(platform);

        if (byPlatform.isEmpty()) {
            log.info("No registered devices found for platform: {}", platform);
            return 0;
        }

        log.info("Sending notification to {} devices on platform: {}", byPlatform.size(), platform);

        return sendToAll(byPlatform, title, body);
    }

    @Override
    @Transactional(readOnly = true)
    public int notifyDevicesByIds(List<Long> deviceIds, String title, String body) {
        List<DeviceToken> selected = deviceTokenRepository.findAllById(deviceIds);

        if (selected.isEmpty()) {
            log.info("No devices found for the given IDs");
            return 0;
        }

        log.info("Sending notification to {} selected devices", selected.size());

        return sendToAll(selected, title, body);
    }

    private int sendToAll(List<DeviceToken> devices, String title, String body) {
        int successCount = 0;
        for (DeviceToken device : devices) {
            try {
                pushService.send(device.getToken(), title, body);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send notification to device {} (user: {}): {}",
                        device.getToken(), device.getEmailUsuario(), e.getMessage());
            }
        }

        log.info("Notification sent to {}/{} devices", successCount, devices.size());
        return successCount;
    }
}
