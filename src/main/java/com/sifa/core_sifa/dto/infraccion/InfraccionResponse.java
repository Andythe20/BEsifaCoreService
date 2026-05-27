package com.sifa.core_sifa.dto.infraccion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sifa.core_sifa.model.EvidenciaFotografica;
import com.sifa.core_sifa.model.Infraccion;

/**
 * DTO que representa la respuesta estructurada de una infracción para el
 * frontend (Dashboard).
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfraccionResponse {

  // columnas generales de la tabla infraccion
  @Schema(description = "Identificador único de la infracción", example = "1")
  private String id;
  @Schema(description = "Identificador del usuario Fiscalizador al emitir la infraccion", example = "1")
  private String idFiscalizador;
  @Schema(description = "Identificador del usuario JPL al procesar la infraccion", example = "1")
  private String idUsuarioJpl;
  @Schema(description = "Fecha exacta cuando se emite la infraccion", example = "2024-05-20T14:30:00")
  private LocalDateTime fecha;
  @Schema(description = "Estado de la infraccion, depende de como lo procese el JPL", example = "EN PROCESO")
  private String status;
  @Schema(description = "En caso de ser rechazada, se llena este campo com un motivo", example = "Evidencias no válidas")
  private String motivoRechazo;
  @Schema(description = "Fecha en la cual el usuario JPL procesa la infraccion", example = "2024-05-20T14:30:00")
  private LocalDateTime fechaResolucion;
  @Schema(description = "Campo que puede ser llenado cuando se esta registrando la infraccion", example = "El vehiculo estaba estacionado en medio de un paso cebra")
  private String observaciones;

  // nodos anidados
  @Schema(
          description = "Detalles de la infracción cometida (gravedad, nombre y normativa legal)",
          implementation = TipoInfraccionDTO.class
  )
  private TipoInfraccionDTO tipoInfraccion;
  @Schema(
          description = "Datos de geolocalización y dirección del lugar exacto del suceso",
          implementation = LocationDTO.class
  )
  private LocationDTO location;

  @Schema(
          description = "Información técnica y de registro del vehículo que cometió la falta",
          implementation = VehicleDTO.class
  )
  private VehicleDTO vehicle;

  @Schema(
          description = "Datos personales e informativos del propietario del vehículo fiscalizado",
          implementation = PropietarioDTO.class
  )
  private PropietarioDTO propietario;

  @Schema(
          description = "Información sobre el agendamiento y citación al Juzgado de Policía Local. Puede ser null si aún no se procesa.",
          implementation = CitacionDTO.class,
          nullable = true
  )
  private CitacionDTO citacion;

  // 3. Evidencias
  @Schema(
          description = "Lista de URLs públicas de las fotografías capturadas en terreno por el fiscalizador como respaldo de la infracción",
          example = "[\"https://sifa-storage.s3.amazonaws.com/evidencias/2026/05/inf_1024_frontal.jpg\", \"https://sifa-storage.s3.amazonaws.com/evidencias/2026/05/inf_1024_patente.jpg\"]"
  )
  private List<String> evidenceUrls;


  @Data
  @Builder
  @Schema(description = "Coordenadas geográficas y dirección formateada donde se cursó la multa")
  public static class LocationDTO {
    @Schema(description = "Dirección legible obtenida por geocodificación inversa o ingresada manualmente", example = "Av. Borgoño 21300, Caleta Higuerillas, Concón")
    private String address;

    @Schema(description = "Latitud precisa del dispositivo GPS", example = "-32.927145")
    private Float lat;

    @Schema(description = "Longitud precisa del dispositivo GPS", example = "-71.521245")
    private Float lng;
  }

  @Data
  @Builder
  @Schema(description = "Información del tipo y gravedad de la infracción cometida")
  public static class TipoInfraccionDTO {
    @Schema(description = "ID único del tipo de infracción", example = "1")
    private Integer id;

    @Schema(description = "Nombre corto de la infracción", example = "Estacionar en sitio prohibido o señalizado")
    private String nombre;

    @Schema(description = "Artículo o normativa legal infringida", example = "Art. 154 Ley de Tránsito")
    private String disposicionInfringida;
  }

  @Data
  @Builder
  @Schema(description = "Datos técnicos del vehículo fiscalizado extraídos del registro")
  public static class VehicleDTO {
    @Schema(description = "Patente única del vehículo (Sin guiones)", example = "BBCC11")
    private String plate;

    @Schema(description = "Marca de fabricante", example = "Toyota")
    private String brand;

    @Schema(description = "Modelo comercial", example = "Yaris")
    private String model;

    @Schema(description = "Año de fabricación", example = "2018")
    private Integer year;

    @Schema(description = "Color principal registrado", example = "Blanco")
    private String color;

    @Schema(description = "Tipo de carrocería o vehículo", example = "Sedán")
    private String type;

    @Schema(description = "Número de motor asociado", example = "1NZ-FE-1234567")
    private String nroMotor;

    @Schema(description = "Número de serie o chasis (VIN)", example = "JTDBT123X4567890")
    private String nroSerie;
  }

  @Data
  @Builder
  @Schema(description = "Datos personales del propietario registrado del vehículo")
  public static class PropietarioDTO {
    @Schema(description = "RUT del propietario con dígito verificador", example = "12345678-9")
    private String rut;

    @Schema(description = "Nombres y apellidos unidos", example = "Juan Pérez González")
    private String nombreCompleto;

    @Schema(description = "Dirección particular registrada", example = "Calle Ficticia 123, Depto 4")
    private String direccion;

    @Schema(description = "Comuna de residencia", example = "Viña del Mar")
    private String comuna;

    @Schema(description = "Correo electrónico de contacto", example = "juan.perez@email.com")
    private String correo;

    @Schema(description = "Teléfono móvil o fijo", example = "+56912345678")
    private String telefono;

    @Schema(description = "Profesión u oficio", example = "Ingeniero")
    private String profesion;

    @Schema(description = "Estado civil legal", example = "Casado")
    private String estadoCivil;

    @Schema(description = "Edad calculada", example = "45")
    private Integer edad;
  }

  @Data
  @Builder
  @Schema(description = "Información sobre la citación al Juzgado de Policía Local (JPL)")
  public static class CitacionDTO {
    @Schema(description = "Fecha y hora en la que el infractor debe presentarse a declarar o pagar", example = "2026-06-15T09:00:00")
    private LocalDateTime fechaCitacion;
  }

  /**
   * Mapea los estados internos de la BD (español) a los estándares del frontend
   * (inglés).
   */
  private static String mapEstadoToFrontend(String estado) {
    if (estado == null) return "pending";
    return switch (estado.toUpperCase()) {
      case "PENDING", "EN PROCESO", "PENDIENTE" -> "pending";
      case "ACCEPTED", "APROBADA", "ACEPTADA" -> "accepted";
      case "REJECTED", "RECHAZADA" -> "rejected";
      case "EXPORTED", "EXPORTADA" -> "exported";
      default -> "pending";
    };
  }

  /**
   * Construye el DTO aplicando formatos y valores seguros por defecto (evita
   * NullPointer).
   */
  public static InfraccionResponse fromEntity(Infraccion entity) {
    return InfraccionResponse.builder()
            .id(String.valueOf(entity.getIdInfraccion()))
            .idFiscalizador(entity.getIdFiscalizador())
            .idUsuarioJpl(entity.getIdUsuarioJpl())
            .fecha(entity.getFecha())
            .status(mapEstadoToFrontend(entity.getEstado()))
            .motivoRechazo(entity.getMotivoRechazo())
            .fechaResolucion(entity.getFechaResolucion())
            .observaciones(entity.getObservaciones())
            .tipoInfraccion(buildTipoInfraccion(entity))
            .location(buildLocation(entity))
            .vehicle(buildVehicle(entity))
            .propietario(buildPropietario(entity))
            .citacion(buildCitacion(entity))
            .evidenceUrls(buildEvidenceUrls(entity))
            .build();
  }

  private static TipoInfraccionDTO buildTipoInfraccion(Infraccion entity) {
    if (entity.getTipoInfraccion() == null) return null;
    return TipoInfraccionDTO.builder()
            .id(entity.getTipoInfraccion().getIdTipoInfraccion())
            .nombre(entity.getTipoInfraccion().getNombre())
            .disposicionInfringida(entity.getTipoInfraccion().getDisposicionInfringida())
            .build();
  }

  private static LocationDTO buildLocation(Infraccion entity) {
    return LocationDTO.builder()
            .address(entity.getLugar())
            .lat(entity.getLatitud())
            .lng(entity.getLongitud())
            .build();
  }

  private static VehicleDTO buildVehicle(Infraccion entity) {
    if (entity.getVehiculo() == null) return null;
    return VehicleDTO.builder()
            .plate(entity.getVehiculo().getPatente())
            .brand(entity.getVehiculo().getMarca())
            .model(entity.getVehiculo().getModelo())
            .year(entity.getVehiculo().getAnioFabricacion())
            .color(entity.getVehiculo().getColor())
            .type(entity.getVehiculo().getTipo())
            .nroMotor(entity.getVehiculo().getNroMotor())
            .nroSerie(entity.getVehiculo().getNroSerie())
            .build();
  }

  private static PropietarioDTO buildPropietario(Infraccion entity) {
    if (entity.getVehiculo() == null || entity.getVehiculo().getPropietarioVehiculo() == null) return null;
    var prop = entity.getVehiculo().getPropietarioVehiculo();
    return PropietarioDTO.builder()
            .rut(prop.getRut())
            .nombreCompleto(prop.getNombres() + " " + prop.getApellidos())
            .direccion(prop.getDireccion())
            .comuna(prop.getComuna())
            .correo(prop.getCorreo())
            .telefono(prop.getTelefono())
            .profesion(prop.getProfesion())
            .estadoCivil(prop.getEstadoCivil())
            .edad(prop.getEdad())
            .build();
  }

  private static CitacionDTO buildCitacion(Infraccion entity) {
    if (entity.getCitacion() == null) return null;
    return CitacionDTO.builder()
            .fechaCitacion(entity.getCitacion().getFecha())
            .build();
  }

  private static List<String> buildEvidenceUrls(Infraccion entity) {
    if (entity.getEvidenciasFotograficas() == null) return Collections.emptyList();
    return entity.getEvidenciasFotograficas().stream()
            .map(EvidenciaFotografica::getUrl)
            .collect(Collectors.toList());
  }
}
