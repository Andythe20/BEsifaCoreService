package com.sifa.core_sifa.service.device;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeviceMaintenanceServiceTest {

    @Mock
    private IDeviceTokenService deviceTokenService;

    @InjectMocks
    private DeviceMaintenanceService deviceMaintenanceService;

    @Test
    void markInactiveDevices_cuandoHayAfectados_limpia() {
        given(deviceTokenService.cleanupStaleDevices()).willReturn(5);

        deviceMaintenanceService.markInactiveDevices();

        verify(deviceTokenService).cleanupStaleDevices();
    }

    @Test
    void markInactiveDevices_cuandoNoHayAfectados_noLogueaDetalle() {
        given(deviceTokenService.cleanupStaleDevices()).willReturn(0);

        deviceMaintenanceService.markInactiveDevices();

        verify(deviceTokenService).cleanupStaleDevices();
    }
}
