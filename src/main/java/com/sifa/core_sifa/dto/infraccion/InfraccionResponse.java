package com.sifa.core_sifa.dto.infraccion;

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
  private String id;
  private String idFiscalizador;
  private String idUsuarioJpl;
  private LocalDateTime fecha;
  private String status;
  private String motivoRechazo;
  private LocalDateTime fechaResolucion;
  private String observaciones;

  // nodos anidados
  private TipoInfraccionDTO tipoInfraccion;
  private LocationDTO location;
  private VehicleDTO vehicle;
  private PropietarioDTO propietario;
  private CitacionDTO citacion;

  // 3. Evidencias
  private List<String> evidenceUrls;


  @Data
  @Builder
  public static class LocationDTO {
    private String address;
    private Float lat;
    private Float lng;
  }

  @Data @Builder
  public static class TipoInfraccionDTO {
    private Integer id;
    private String nombre;
    private String disposicionInfringida;
  }

  @Data @Builder
  public static class VehicleDTO {
    private String plate;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private String type;
    private String nroMotor;
    private String nroSerie;
  }

  @Data @Builder
  public static class PropietarioDTO {
    private String rut;
    private String nombreCompleto;
    private String direccion;
    private String comuna;
    private String correo;
    private String telefono;
    private String profesion;
    private String estadoCivil;
    private Integer edad;
  }

  @Data @Builder
  public static class CitacionDTO {
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
