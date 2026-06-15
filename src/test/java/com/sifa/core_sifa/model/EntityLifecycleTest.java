package com.sifa.core_sifa.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EntityLifecycleTest {

    @Test
    void deviceToken_onCreate_seteaCreatedAtYUpdatedAt() {
        DeviceToken token = new DeviceToken();
        token.onCreate();

        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getUpdatedAt()).isNotNull();
    }

    @Test
    void deviceToken_onUpdate_seteaUpdatedAt() {
        DeviceToken token = new DeviceToken();
        LocalDateTime original = LocalDateTime.of(2020, 1, 1, 0, 0);
        token.setUpdatedAt(original);

        token.onUpdate();

        assertThat(token.getUpdatedAt()).isAfter(original);
    }

    @Test
    void deviceToken_builder_defaultStatusIsUnknown() {
        DeviceToken token = DeviceToken.builder()
                .emailUsuario("test@test.cl")
                .token("fcm-token")
                .platform("ANDROID")
                .appVersion("1.0")
                .build();

        assertThat(token.getStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    void auditLog_onCreate_seteaFechaHora() {
        AuditLog log = new AuditLog();
        log.onCreate();

        assertThat(log.getFechaHora()).isNotNull();
    }

    @Test
    void auditLog_builder_creaConDetalles() {
        Map<String, Object> detalles = Map.of("accion", "test");
        AuditLog log = AuditLog.builder()
                .emailUsuario("user@test.cl")
                .accion("LOGIN")
                .detalles(detalles)
                .build();

        assertThat(log.getEmailUsuario()).isEqualTo("user@test.cl");
        assertThat(log.getAccion()).isEqualTo("LOGIN");
        assertThat(log.getDetalles()).containsEntry("accion", "test");
    }

    @Test
    void notificationLog_onCreate_seteaSentAt() {
        NotificationLog log = new NotificationLog();
        log.onCreate();

        assertThat(log.getSentAt()).isNotNull();
    }

    @Test
    void notificationLog_builder_creaCorrectamente() {
        NotificationLog log = NotificationLog.builder()
                .targetType("TOKEN")
                .title("Notificación")
                .body("Cuerpo del mensaje")
                .devicesCount(5)
                .sentBy("admin@test.cl")
                .build();

        assertThat(log.getTargetType()).isEqualTo("TOKEN");
        assertThat(log.getTitle()).isEqualTo("Notificación");
        assertThat(log.getDevicesCount()).isEqualTo(5);
    }

    @Test
    void evidenciaFotografica_builder_creaCorrectamente() {
        Infraccion infraccion = Infraccion.builder()
                .lugar("Test")
                .latitud(-33.0f)
                .longitud(-71.0f)
                .fecha(LocalDateTime.now())
                .estado("EN PROCESO")
                .build();

        EvidenciaFotografica evidencia = EvidenciaFotografica.builder()
                .url("https://storage.example.com/foto.jpg")
                .infraccion(infraccion)
                .build();

        assertThat(evidencia.getUrl()).isEqualTo("https://storage.example.com/foto.jpg");
        assertThat(evidencia.getInfraccion()).isSameAs(infraccion);
    }

    @Test
    void fiscalizadorPresenciaId_constructorAndEquals() {
        FiscalizadorPresenciaId id1 = new FiscalizadorPresenciaId("user@test.cl", LocalDateTime.of(2024, 1, 1, 10, 0));
        FiscalizadorPresenciaId id2 = new FiscalizadorPresenciaId("user@test.cl", LocalDateTime.of(2024, 1, 1, 10, 0));

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
