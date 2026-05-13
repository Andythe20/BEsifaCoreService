package com.sifa.core_sifa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.Vehiculo;

import java.util.Optional;

@Repository
public interface IVehiculoRepository extends JpaRepository<Vehiculo, String> {

    boolean existsByNroMotor(String nroMotor);

    Optional<Vehiculo> findByNroMotor(String nroMotor);

    boolean existsByNroSerie(String nroSerie);

    Optional<Vehiculo> findByNroSerie(String nroSerie);
}