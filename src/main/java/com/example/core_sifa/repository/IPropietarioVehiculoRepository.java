package com.example.core_sifa.repository;

import com.example.core_sifa.model.PropietarioVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPropietarioVehiculoRepository extends JpaRepository<PropietarioVehiculo, String> {
}
