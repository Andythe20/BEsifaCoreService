package com.sifa.core_sifa.service.device;

import com.sifa.core_sifa.dto.device.DeviceRegisterRequest;

public interface IDeviceTokenService {

    void register(String emailUsuario, DeviceRegisterRequest request);

    int notifyOutdatedDevices(String currentVersion, String title, String body);
}
