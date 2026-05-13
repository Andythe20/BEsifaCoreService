package com.sifa.core_sifa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.TipoInfraccion;

@Repository
public interface ITipoInfraccionRepository extends JpaRepository<TipoInfraccion, Integer> {
}
