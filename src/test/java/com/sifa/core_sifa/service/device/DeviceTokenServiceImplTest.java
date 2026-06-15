package com.sifa.core_sifa.service.device;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.sifa.core_sifa.dto.device.DeviceRegisterRequest;
import com.sifa.core_sifa.dto.fiscalizador.FiscalizadorHeartbeatRequest;
import com.sifa.core_sifa.model.DeviceToken;
import com.sifa.core_sifa.repository.IDeviceTokenRepository;
import com.sifa.core_sifa.service.push.IPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceImplTest {

    @Mock
    private IDeviceTokenRepository deviceTokenRepository;

    @Mock
    private IPushService pushService;

    @InjectMocks
    private DeviceTokenServiceImpl deviceTokenService;

    @Captor
    private ArgumentCaptor<DeviceToken> tokenCaptor;

    @Test
    void register_cuandoTokenExistente_actualiza() {
        var existing = DeviceToken.builder().id(1L).token("existing-token").emailUsuario("old@test.cl").build();
        given(deviceTokenRepository.findByToken("existing-token")).willReturn(Optional.of(existing));

        var request = DeviceRegisterRequest.builder()
                .token("existing-token")
                .platform("ANDROID")
                .appVersion("2.0.0")
                .deviceId("device-123")
                .deviceModel("Pixel 7")
                .manufacturer("Google")
                .build();

        deviceTokenService.register("user@test.cl", request);

        verify(deviceTokenRepository).save(tokenCaptor.capture());
        var saved = tokenCaptor.getValue();
        assertThat(saved.getEmailUsuario()).isEqualTo("user@test.cl");
        assertThat(saved.getPlatform()).isEqualTo("ANDROID");
        assertThat(saved.getAppVersion()).isEqualTo("2.0.0");
    }

    @Test
    void register_cuandoTokenNuevo_conOldTokens_eliminaViejosYCrea() {
        given(deviceTokenRepository.findByToken("new-token")).willReturn(Optional.empty());
        var oldTokens = List.of(
                DeviceToken.builder().id(1L).token("old-token").emailUsuario("user@test.cl").build()
        );
        given(deviceTokenRepository.findByEmailUsuario("user@test.cl")).willReturn(oldTokens);

        var request = DeviceRegisterRequest.builder()
                .token("new-token")
                .platform("IOS")
                .build();

        deviceTokenService.register("user@test.cl", request);

        verify(deviceTokenRepository).deleteAll(oldTokens);
        verify(deviceTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("new-token");
    }

    @Test
    void register_cuandoTokenNuevo_sinOldTokens_creaDirectamente() {
        given(deviceTokenRepository.findByToken("new-token")).willReturn(Optional.empty());
        given(deviceTokenRepository.findByEmailUsuario("user@test.cl")).willReturn(List.of());

        var request = DeviceRegisterRequest.builder()
                .token("new-token")
                .platform("ANDROID")
                .build();

        deviceTokenService.register("user@test.cl", request);

        verify(deviceTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("new-token");
        assertThat(tokenCaptor.getValue().getEmailUsuario()).isEqualTo("user@test.cl");
    }

    @Test
    void getAllDevices_returnsAll() {
        var devices = List.of(DeviceToken.builder().id(1L).token("t1").build());
        given(deviceTokenRepository.findAll()).willReturn(devices);

        var result = deviceTokenService.getAllDevices();

        assertThat(result).hasSize(1);
    }

    @Test
    void getDeviceStats_returnsCounts() {
        given(deviceTokenRepository.count()).willReturn(10L);
        given(deviceTokenRepository.countByStatus("ACTIVE")).willReturn(5L);
        given(deviceTokenRepository.countByStatus("UNKNOWN")).willReturn(3L);
        given(deviceTokenRepository.countByStatus("INACTIVE")).willReturn(2L);

        var stats = deviceTokenService.getDeviceStats();

        assertThat(stats).containsAllEntriesOf(Map.of(
                "total", 10L, "active", 5L, "unknown", 3L, "inactive", 2L
        ));
    }

    @Test
    void cleanupStaleDevices_returnsUpdatedCount() {
        given(deviceTokenRepository.markInactiveSince(anyString(), any())).willReturn(3);

        var result = deviceTokenService.cleanupStaleDevices();

        assertThat(result).isEqualTo(3);
    }

    @Test
    void processHeartbeat_cuandoExisteDeviceId_actualiza() {
        var token = DeviceToken.builder()
                .id(1L).token("fcm-token").emailUsuario("user@test.cl")
                .deviceId("device-123").platform("ANDROID")
                .build();
        given(deviceTokenRepository.findByDeviceId("device-123")).willReturn(Optional.of(token));

        var request = new FiscalizadorHeartbeatRequest();
        request.setDeviceId("device-123");
        request.setMarca("Google");
        request.setModelo("Pixel 7");

        deviceTokenService.processHeartbeat("user@test.cl", request);

        verify(deviceTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(tokenCaptor.getValue().getDeviceModel()).isEqualTo("Pixel 7");
    }

    @Test
    void processHeartbeat_cuandoNoExisteDeviceId_peroExisteEmail_vincula() {
        given(deviceTokenRepository.findByDeviceId("device-456")).willReturn(Optional.empty());
        var tokenByEmail = DeviceToken.builder()
                .id(2L).token("fcm-token").emailUsuario("user@test.cl")
                .platform("ANDROID").deviceId(null)
                .build();
        given(deviceTokenRepository.findByEmailUsuario("user@test.cl")).willReturn(List.of(tokenByEmail));

        var request = new FiscalizadorHeartbeatRequest();
        request.setDeviceId("device-456");
        request.setMarca("Samsung");
        request.setModelo("Galaxy S24");

        deviceTokenService.processHeartbeat("user@test.cl", request);

        verify(deviceTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getDeviceId()).isEqualTo("device-456");
        assertThat(tokenCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void processHeartbeat_cuandoNoExiste_conFcmToken_autoregistra() {
        given(deviceTokenRepository.findByDeviceId("device-789")).willReturn(Optional.empty());
        given(deviceTokenRepository.findByEmailUsuario("user@test.cl")).willReturn(List.of());

        var request = new FiscalizadorHeartbeatRequest();
        request.setDeviceId("device-789");
        request.setMarca("Apple");
        request.setModelo("iPhone 15");
        request.setFcmToken("new-fcm-token");

        deviceTokenService.processHeartbeat("user@test.cl", request);

        verify(deviceTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("new-fcm-token");
        assertThat(tokenCaptor.getValue().getPlatform()).isEqualTo("ANDROID");
        assertThat(tokenCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void processHeartbeat_cuandoNoExiste_sinFcmToken_noHaceNada() {
        given(deviceTokenRepository.findByDeviceId("device-000")).willReturn(Optional.empty());
        given(deviceTokenRepository.findByEmailUsuario("user@test.cl")).willReturn(List.of());

        var request = new FiscalizadorHeartbeatRequest();
        request.setDeviceId("device-000");
        request.setMarca("Test");
        request.setModelo("Test");

        deviceTokenService.processHeartbeat("user@test.cl", request);

        verifyNoInteractions(pushService);
    }

    @Test
    void notifyOutdatedDevices_cuandoHayDesactualizados_envia() {
        var outdated = List.of(
                DeviceToken.builder().id(1L).token("t1").emailUsuario("u1@test.cl").build(),
                DeviceToken.builder().id(2L).token("t2").emailUsuario("u2@test.cl").build()
        );
        given(deviceTokenRepository.findByAppVersionNot("2.0.0")).willReturn(outdated);

        var result = deviceTokenService.notifyOutdatedDevices("2.0.0", "Update", "Please update");

        assertThat(result).isEqualTo(2);
    }

    @Test
    void notifyOutdatedDevices_cuandoNoHay_retornaCero() {
        given(deviceTokenRepository.findByAppVersionNot("2.0.0")).willReturn(List.of());

        var result = deviceTokenService.notifyOutdatedDevices("2.0.0", "Update", "Please update");

        assertThat(result).isZero();
        verifyNoInteractions(pushService);
    }

    @Test
    void notifyAllDevices_enviaATodos() {
        var all = List.of(
                DeviceToken.builder().id(1L).token("t1").emailUsuario("u1@test.cl").build()
        );
        given(deviceTokenRepository.findAll()).willReturn(all);

        var result = deviceTokenService.notifyAllDevices("Title", "Body");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void notifyAllDevices_cuandoNoHay_retornaCero() {
        given(deviceTokenRepository.findAll()).willReturn(List.of());

        var result = deviceTokenService.notifyAllDevices("Title", "Body");

        assertThat(result).isZero();
    }

    @Test
    void notifyByPlatform_enviaPorPlataforma() {
        var devices = List.of(
                DeviceToken.builder().id(1L).token("t1").emailUsuario("u1@test.cl").build()
        );
        given(deviceTokenRepository.findByPlatform("ANDROID")).willReturn(devices);

        var result = deviceTokenService.notifyByPlatform("ANDROID", "Title", "Body");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void notifyByPlatform_cuandoNoHay_retornaCero() {
        given(deviceTokenRepository.findByPlatform("IOS")).willReturn(List.of());

        var result = deviceTokenService.notifyByPlatform("IOS", "Title", "Body");

        assertThat(result).isZero();
    }

    @Test
    void notifyDevicesByIds_enviaASeleccionados() {
        var devices = List.of(
                DeviceToken.builder().id(1L).token("t1").emailUsuario("u1@test.cl").build()
        );
        given(deviceTokenRepository.findAllById(List.of(1L))).willReturn(devices);

        var result = deviceTokenService.notifyDevicesByIds(List.of(1L), "Title", "Body");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void notifyDevicesByIds_cuandoNoHay_retornaCero() {
        given(deviceTokenRepository.findAllById(List.of(99L))).willReturn(List.of());

        var result = deviceTokenService.notifyDevicesByIds(List.of(99L), "Title", "Body");

        assertThat(result).isZero();
    }

    @Test
    void sendToAll_cuandoFallaPush_noInterrumpeProceso() throws FirebaseMessagingException {
        var devices = List.of(
                DeviceToken.builder().id(1L).token("bad-token").emailUsuario("u1@test.cl").build()
        );
        given(deviceTokenRepository.findAll()).willReturn(devices);
        willThrow(new RuntimeException("Firebase error"))
                .given(pushService).send(anyString(), anyString(), anyString());

        var result = deviceTokenService.notifyAllDevices("Title", "Body");

        assertThat(result).isZero();
    }
}
