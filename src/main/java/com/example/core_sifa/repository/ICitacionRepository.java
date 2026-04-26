package com.example.core_sifa.repository;

import com.example.core_sifa.model.Citacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICitacionRepository extends JpaRepository<Citacion, Integer> {
}
