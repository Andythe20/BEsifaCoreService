package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.config.AbstractIntegrationTest;
import com.sifa.core_sifa.model.DeviceToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IDeviceTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private IDeviceTokenRepository deviceTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        deviceTokenRepository.deleteAll();
        deviceTokenRepository.save(DeviceToken.builder()
                .emailUsuario("user1@test.cl").token("fcm-token-1")
                .platform("ANDROID").appVersion("1.0.0")
                .deviceId("device-1").deviceModel("Pixel 7").manufacturer("Google")
                .status("ACTIVE").lastHeartbeatAt(LocalDateTime.now().minusMinutes(1))
                .build());
        deviceTokenRepository.save(DeviceToken.builder()
                .emailUsuario("user2@test.cl").token("fcm-token-2")
                .platform("IOS").appVersion("1.0.0")
                .deviceId("device-2").deviceModel("iPhone 15").manufacturer("Apple")
                .status("ACTIVE").lastHeartbeatAt(LocalDateTime.now().minusMinutes(5))
                .build());
        deviceTokenRepository.save(DeviceToken.builder()
                .emailUsuario("user3@test.cl").token("fcm-token-3")
                .platform("ANDROID").appVersion("0.9.0")
                .deviceId("device-3").deviceModel("Galaxy S24").manufacturer("Samsung")
                .status("UNKNOWN").lastHeartbeatAt(LocalDateTime.now().minusMinutes(30))
                .build());
    }

    @Test
    void findByToken_cuandoExiste_retornaToken() {
        var result = deviceTokenRepository.findByToken("fcm-token-1");

        assertThat(result).isPresent();
        assertThat(result.get().getEmailUsuario()).isEqualTo("user1@test.cl");
    }

    @Test
    void findByToken_cuandoNoExiste_retornaEmpty() {
        var result = deviceTokenRepository.findByToken("unknown-token");

        assertThat(result).isEmpty();
    }

    @Test
    void findByAppVersionNot_retornaDispositivosConVersionDiferente() {
        var result = deviceTokenRepository.findByAppVersionNot("1.0.0");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAppVersion()).isEqualTo("0.9.0");
    }

    @Test
    void findByPlatform_retornaDispositivosDePlataforma() {
        var result = deviceTokenRepository.findByPlatform("IOS");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPlatform()).isEqualTo("IOS");
    }

    @Test
    void findByDeviceId_cuandoExiste_retornaToken() {
        var result = deviceTokenRepository.findByDeviceId("device-1");

        assertThat(result).isPresent();
        assertThat(result.get().getEmailUsuario()).isEqualTo("user1@test.cl");
    }

    @Test
    void findByEmailUsuario_retornaTokensDelUsuario() {
        var result = deviceTokenRepository.findByEmailUsuario("user1@test.cl");

        assertThat(result).hasSize(1);
    }

    @Test
    void countByStatus_retornaConteoCorrecto() {
        assertThat(deviceTokenRepository.countByStatus("ACTIVE")).isEqualTo(2);
        assertThat(deviceTokenRepository.countByStatus("UNKNOWN")).isEqualTo(1);
        assertThat(deviceTokenRepository.countByStatus("INACTIVE")).isZero();
    }

    @Test
    @Transactional
    void markInactiveSince_actualizaTokensAntiguos() {
        var threshold = LocalDateTime.now().minusMinutes(10);

        int updated = deviceTokenRepository.markInactiveSince("INACTIVE", threshold);

        assertThat(updated).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        var inactive = deviceTokenRepository.findByEmailUsuario("user3@test.cl").getFirst();
        assertThat(inactive.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @Transactional
    void markInactiveSince_conTodosActivos_noActualizaNinguno() {
        var threshold = LocalDateTime.now().minusMinutes(40);

        int updated = deviceTokenRepository.markInactiveSince("INACTIVE", threshold);

        assertThat(updated).isZero();
    }
}
