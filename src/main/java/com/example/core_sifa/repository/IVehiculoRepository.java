package com.example.core_sifa.repository;

import com.example.core_sifa.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IVehiculoRepository extends JpaRepository<Vehiculo, String> {

    boolean existsByNroMotor(String nroMotor);

    Optional<Vehiculo> findByNroMotor(String nroMotor);

    boolean existsByNroSerie(String nroSerie);

    Optional<Vehiculo> findByNroSerie(String nroSerie);
}