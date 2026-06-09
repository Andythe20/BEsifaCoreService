package com.sifa.core_sifa.service.device;

import com.sifa.core_sifa.dto.device.DeviceRegisterRequest;
import com.sifa.core_sifa.dto.device.DeviceTokenResponse;
import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.DeviceToken;
import java.util.List;
import java.util.Map;

public interface IDeviceTokenService {

    void register(String emailUsuario, DeviceRegisterRequest request);

    List<DeviceToken> getAllDevices();

    List<DeviceTokenResponse> getAllDeviceResponses();

    void processHeartbeat(String emailUsuario, FiscalizadorHeartbeatRequest request);

    Map<String, Long> getDeviceStats();

    int cleanupStaleDevices();

    int notifyOutdatedDevices(String currentVersion, String title, String body);

    int notifyAllDevices(String title, String body);

    int notifyByPlatform(String platform, String title, String body);

    int notifyDevicesByIds(List<Long> deviceIds, String title, String body);
}
