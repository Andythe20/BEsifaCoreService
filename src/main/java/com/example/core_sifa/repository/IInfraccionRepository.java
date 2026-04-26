package com.example.core_sifa.repository;

import com.example.core_sifa.model.Infraccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IInfraccionRepository extends JpaRepository<Infraccion, Integer> {
}
