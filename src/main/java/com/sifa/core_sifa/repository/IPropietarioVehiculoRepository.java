package com.sifa.core_sifa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.PropietarioVehiculo;

@Repository
public interface IPropietarioVehiculoRepository extends JpaRepository<PropietarioVehiculo, String> {
}
