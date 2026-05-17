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

  private String id; // ID formateado a String para compatibilidad con el frontend
  private String status;
  private LocalDateTime timestamp;
  private String infractionDescription;
  private String numeroBoleta;
  private String numeroParte;
  private String agentId;
  private String infractionCode;
  private String disposicionInfringida;

  private LocationDTO location;
  private VehicleDTO vehicle;
  private DenunciadoDTO denunciado;
  private TramitacionDTO tramitacion;

  private String photoUrl;
  private List<String> evidenceUrls;
  private String denunciante;
  private Float amount;
  private String observaciones;

  @Data
  @Builder
  public static class LocationDTO {
    private String address;
    private Float lat;
    private Float lng;
  }

  @Data
  @Builder
  public static class VehicleDTO {
    private String plate;
    private String brand;
    private String model;
    private String color;
    private String type;
  }

  @Data
  @Builder
  public static class DenunciadoDTO {
    private String rut;
    private String nombre;
    private String direccion;
    private String comuna;
    private String profesion;
    private String estadoCivil;
    private String edad;
  }

  @Data
  @Builder
  public static class TramitacionDTO {
    private String fechaCitacion;
    private Boolean listadoCorte;
    private String fechaFallo;
    private String fechaArchivo;
  }

  /**
   * Mapea los estados internos de la BD (español) a los estándares del frontend
   * (inglés).
   */
  private static String mapEstadoToFrontend(String estado) {
    if (estado == null)
      return "pending";
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
        .status(mapEstadoToFrontend(entity.getEstado()))
        .timestamp(entity.getFecha())
        .infractionDescription(
            entity.getTipoInfraccion() != null ? entity.getTipoInfraccion().getNombre() : "Infracción de Tránsito")
        .numeroBoleta(entity.getNumeroBoleta() != null ? entity.getNumeroBoleta() : "B-" + entity.getIdInfraccion())
        .numeroParte(entity.getNumeroParte() != null ? entity.getNumeroParte() : "P-" + entity.getIdInfraccion())
        .agentId(entity.getIdFiscalizador() != null ? entity.getIdFiscalizador() : "app@sifa.cl")
        .infractionCode(getInfractionCode(entity))
        .disposicionInfringida(getDisposicion(entity))
        .denunciante(entity.getDenunciante() != null ? entity.getDenunciante() : "Seguridad Pública")
        .amount(entity.getMonto() != null ? entity.getMonto() : 0.0f)
        .location(buildLocation(entity))
        .vehicle(buildVehicle(entity))
        .denunciado(buildDenunciado(entity))
        .tramitacion(buildTramitacion(entity))
        .evidenceUrls(buildEvidenceUrls(entity))
        .photoUrl(buildPhotoUrl(entity))
        .observaciones(entity.getObservaciones())
        .build();
  }

  private static String getInfractionCode(Infraccion entity) {
    return entity.getTipoInfraccion() != null ? String.valueOf(entity.getTipoInfraccion().getIdTipoInfraccion())
        : "S/C";
  }

  private static String getDisposicion(Infraccion entity) {
    if (entity.getTipoInfraccion() != null && entity.getTipoInfraccion().getDisposicionInfringida() != null) {
      return entity.getTipoInfraccion().getDisposicionInfringida();
    }
    return "Ley de Tránsito 18.290";
  }

  private static LocationDTO buildLocation(Infraccion entity) {
    return LocationDTO.builder()
        .address(entity.getLugar())
        .lat(entity.getLatitud())
        .lng(entity.getLongitud())
        .build();
  }

  private static VehicleDTO buildVehicle(Infraccion entity) {
    if (entity.getVehiculo() == null)
      return null;
    return VehicleDTO.builder()
        .plate(entity.getVehiculo().getPatente())
        .brand(entity.getVehiculo().getMarca())
        .model(entity.getVehiculo().getModelo())
        .color(entity.getVehiculo().getColor())
        .type(entity.getVehiculo().getTipo() != null ? entity.getVehiculo().getTipo() : "Vehículo Motorizado")
        .build();
  }

  private static DenunciadoDTO buildDenunciado(Infraccion entity) {
    if (entity.getVehiculo() == null || entity.getVehiculo().getPropietarioVehiculo() == null)
      return null;
    var prop = entity.getVehiculo().getPropietarioVehiculo();
    return DenunciadoDTO.builder()
        .rut(prop.getRut())
        .nombre(prop.getNombres() + " " + prop.getApellidos())
        .direccion(prop.getDireccion() != null ? prop.getDireccion() : "No registrada")
        .comuna(prop.getComuna())
        .profesion(prop.getProfesion())
        .estadoCivil(prop.getEstadoCivil())
        .edad(prop.getEdad() != null ? String.valueOf(prop.getEdad()) : "N/A")
        .build();
  }

  /**
   * Resuelve la fecha de citación: usa la citación judicial asignada, o la fecha
   * de la infracción como fallback.
   */
  private static TramitacionDTO buildTramitacion(Infraccion entity) {
    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    String fechaCitacionStr = "No definida";

    if (entity.getCitacion() != null && entity.getCitacion().getFecha() != null) {
      fechaCitacionStr = entity.getCitacion().getFecha().format(formatter);
    } else if (entity.getFecha() != null) {
      fechaCitacionStr = entity.getFecha().format(formatter);
    }

    if (entity.getCitacion() == null) {
      return TramitacionDTO.builder()
          .fechaCitacion(fechaCitacionStr)
          .listadoCorte(false)
          .build();
    }

    var c = entity.getCitacion();
    return TramitacionDTO.builder()
        .fechaCitacion(fechaCitacionStr)
        .listadoCorte(c.getListadoCorte() != null ? c.getListadoCorte() : false)
        .fechaFallo(c.getFechaFallo() != null ? c.getFechaFallo().format(formatter) : null)
        .fechaArchivo(c.getFechaArchivo() != null ? c.getFechaArchivo().format(formatter) : null)
        .build();
  }

  private static List<String> buildEvidenceUrls(Infraccion entity) {
    if (entity.getEvidenciasFotograficas() == null)
      return Collections.emptyList();
    return entity.getEvidenciasFotograficas().stream()
        .map(EvidenciaFotografica::getUrl)
        .collect(Collectors.toList());
  }

  private static String buildPhotoUrl(Infraccion entity) {
    if (entity.getEvidenciasFotograficas() == null || entity.getEvidenciasFotograficas().isEmpty())
      return null;
    return entity.getEvidenciasFotograficas().get(0).getUrl();
  }
}
