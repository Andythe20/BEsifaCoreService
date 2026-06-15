package com.sifa.core_sifa.dto;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.dto.citacion.CitacionCreateRequest;
import com.sifa.core_sifa.dto.citacion.CitacionUpdateRequest;
import com.sifa.core_sifa.dto.device.DeviceRegisterRequest;
import com.sifa.core_sifa.dto.device.DeviceTokenResponse;
import com.sifa.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.sifa.core_sifa.dto.infraccion.InfraccionUpdateRequest;
import com.sifa.core_sifa.dto.push.SinglePushRequest;
import com.sifa.core_sifa.model.*;
import com.sifa.core_sifa.util.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DTOTest {

    @Test
    void errorResponse_builder_createsCorrectly() {
        var now = LocalDateTime.now();
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(now)
                .status(404)
                .error("Not Found")
                .message("Recurso no encontrado")
                .path("/api/v1/test")
                .build();

        assertThat(response.getTimestamp()).isEqualTo(now);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getMessage()).isEqualTo("Recurso no encontrado");
        assertThat(response.getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void infraccionCreateRequest_builder_createsCorrectly() {
        var fecha = LocalDateTime.of(2024, 5, 20, 14, 30);
        InfraccionCreateRequest request = InfraccionCreateRequest.builder()
                .lugar("Av. Maroto 410")
                .fecha(fecha)
                .latitud(-32.9271f)
                .longitud(-71.5212f)
                .patenteVehiculo("ABCD12")
                .idTipoInfraccion(1)
                .observaciones("Test obs")
                .fechaCitacion(fecha)
                .build();

        assertThat(request.getLugar()).isEqualTo("Av. Maroto 410");
        assertThat(request.getPatenteVehiculo()).isEqualTo("ABCD12");
        assertThat(request.getIdTipoInfraccion()).isEqualTo(1);
        assertThat(request.getLatitud()).isEqualTo(-32.9271f);
    }

    @Test
    void infraccionUpdateRequest_builder_createsCorrectly() {
        InfraccionUpdateRequest request = InfraccionUpdateRequest.builder()
                .estado("ACEPTADO")
                .motivoRechazo("Todo correcto")
                .build();

        assertThat(request.getEstado()).isEqualTo("ACEPTADO");
        assertThat(request.getMotivoRechazo()).isEqualTo("Todo correcto");
    }

    @Test
    void infraccionUpdateRequest_conCamposNull_permiteValoresNull() {
        InfraccionUpdateRequest request = InfraccionUpdateRequest.builder().build();
        assertThat(request.getEstado()).isNull();
        assertThat(request.getMotivoRechazo()).isNull();
    }

    @Test
    void citacionCreateRequest_builder_createsCorrectly() {
        var fecha = LocalDateTime.now();
        CitacionCreateRequest request = CitacionCreateRequest.builder()
                .fecha(fecha)
                .idInfraccion(1)
                .build();

        assertThat(request.getFecha()).isEqualTo(fecha);
        assertThat(request.getIdInfraccion()).isEqualTo(1);
    }

    @Test
    void citacionUpdateRequest_builder_createsCorrectly() {
        var fecha = LocalDateTime.now();
        CitacionUpdateRequest request = CitacionUpdateRequest.builder()
                .fecha(fecha)
                .build();

        assertThat(request.getFecha()).isEqualTo(fecha);
    }

    @Test
    void vehiculoDTO_fromEntity_mapsCorrectly() {
        PropietarioVehiculo prop = TestDataFactory.createPropietario();
        Vehiculo vehiculo = TestDataFactory.createVehiculo(prop);

        VehiculoDTO dto = VehiculoDTO.fromEntity(vehiculo);

        assertThat(dto.getPatente()).isEqualTo("ABCD12");
        assertThat(dto.getMarca()).isEqualTo("TOYOTA");
        assertThat(dto.getModelo()).isEqualTo("YARIS");
        assertThat(dto.getAnio_fabricacion()).isEqualTo(2020);
        assertThat(dto.getColor()).isEqualTo("BLANCO");
        assertThat(dto.getNro_motor()).isEqualTo("MOTOR123");
        assertThat(dto.getNro_serie()).isEqualTo("SERIE456");
        assertThat(dto.getPropietario()).contains("JUAN", "PEREZ GONZALEZ");
        assertThat(dto.getRut()).isEqualTo("12345678-9");
    }

    @Test
    void deviceRegisterRequest_builder_createsCorrectly() {
        DeviceRegisterRequest request = DeviceRegisterRequest.builder()
                .token("fcm-token-123")
                .platform("ANDROID")
                .appVersion("1.0.0")
                .deviceId("device-123")
                .deviceModel("Pixel 8")
                .manufacturer("Google")
                .build();

        assertThat(request.getToken()).isEqualTo("fcm-token-123");
        assertThat(request.getPlatform()).isEqualTo("ANDROID");
        assertThat(request.getAppVersion()).isEqualTo("1.0.0");
    }

    @Test
    void deviceRegisterRequest_sinOpcionales_creaCorrectamente() {
        DeviceRegisterRequest request = DeviceRegisterRequest.builder()
                .token("token")
                .platform("IOS")
                .build();

        assertThat(request.getToken()).isEqualTo("token");
        assertThat(request.getDeviceId()).isNull();
    }

    @Test
    void deviceTokenResponse_fromEntity_mapsCorrectly() {
        var now = LocalDateTime.now();
        DeviceToken entity = DeviceToken.builder()
                .id(1L)
                .emailUsuario("test@test.cl")
                .token("fcm-token")
                .platform("ANDROID")
                .appVersion("2.0")
                .deviceId("dev-1")
                .deviceModel("S24")
                .manufacturer("Samsung")
                .lastHeartbeatAt(now)
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        DeviceTokenResponse response = DeviceTokenResponse.fromEntity(entity);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmailUsuario()).isEqualTo("test@test.cl");
        assertThat(response.getPlatform()).isEqualTo("ANDROID");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getLastHeartbeatAt()).isEqualTo(now);
    }

    @Test
    void singlePushRequest_builder_createsCorrectly() {
        SinglePushRequest request = SinglePushRequest.builder()
                .token("fcm-token")
                .title("Título")
                .body("Mensaje")
                .build();

        assertThat(request.getToken()).isEqualTo("fcm-token");
        assertThat(request.getTitle()).isEqualTo("Título");
        assertThat(request.getBody()).isEqualTo("Mensaje");
    }

    @Test
    void auditLogRequestDTO_data_worksCorrectly() {
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("key1", "value1");

        AuditLogRequestDTO dto = AuditLogRequestDTO.builder()
                .emailUsuario("admin@test.cl")
                .accion("CREATE")
                .tablaAfectada("INFRACCIONES")
                .idRegistroAfectado("1")
                .detalles(detalles)
                .build();

        assertThat(dto.getEmailUsuario()).isEqualTo("admin@test.cl");
        assertThat(dto.getAccion()).isEqualTo("CREATE");
        assertThat(dto.getDetalles()).containsEntry("key1", "value1");

        AuditLogRequestDTO noArgs = new AuditLogRequestDTO();
        assertThat(noArgs.getEmailUsuario()).isNull();
    }
}
