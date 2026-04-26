package com.example.core_sifa.repository;

import com.example.core_sifa.model.Citacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICitacionRepository extends JpaRepository<Citacion, String> {
}
