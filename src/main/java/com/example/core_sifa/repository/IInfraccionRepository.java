package com.example.core_sifa.repository;

import com.example.core_sifa.model.Infraccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IInfraccionRepository extends JpaRepository<Infraccion, Integer> {

    // Para buscar todas las infracciones que hizo un usuario en terreno
    List<Infraccion> findByIdFiscalizador(String idFiscalizador);

    // Para ver el historial de multas de un vehículo navegando por la relación
    List<Infraccion> findByVehiculoPatente(String vehiculoPatente);
}
