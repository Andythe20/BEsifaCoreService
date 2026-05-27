package com.sifa.core_sifa.service.infraccion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.sifa.core_sifa.dto.infraccion.CoordenadaDTO;
import com.sifa.core_sifa.dto.infraccion.ReporteResumenDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.sifa.core_sifa.dto.infraccion.InfraccionCreateRequest;
import com.sifa.core_sifa.dto.infraccion.InfraccionResponse;
import com.sifa.core_sifa.dto.infraccion.InfraccionUpdateRequest;

public interface IInfraccionService {

        List<InfraccionResponse> findAllInfracciones();

        InfraccionResponse findById(Integer idInfraccion);

        Page<InfraccionResponse> findByIdFiscalizador(String idFiscalizador, Pageable pageable);

        List<InfraccionResponse> findByVehiculoPatente(String vehiculoPatente);

        InfraccionResponse crearInfraccion(
                        InfraccionCreateRequest request,
                        List<MultipartFile> fotos,
                        String idFiscalizador);

        InfraccionResponse procesarInfraccionPorJpl(
                        Integer idInfraccion,
                        InfraccionUpdateRequest request,
                        String idAdministrativoJpl);

        List<InfraccionResponse> findByDate(LocalDate date);

        Page<InfraccionResponse> findInfracciones(
                        LocalDate startDate,
                        LocalDate endDate,
                        String user,
                        Pageable pageable);

        InfraccionResponse actualizarEstadoInfraccion(
                        Integer id,
                        String status,
                        String idUsuario);

        InfraccionResponse editarInfraccion(
                        Integer id,
                        Map<String, Object> request);

        List<CoordenadaDTO> findCoordenadas(
                        LocalDate startDate,
                        LocalDate endDate,
                        String user);

        ReporteResumenDTO obtenerResumenReporte(
                        LocalDate startDate,
                        LocalDate endDate,
                        String user);
}