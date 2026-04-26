package com.example.core_sifa.repository;

import com.example.core_sifa.model.EvidenciaFotografica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IEvidenciaFotograficaRepository extends JpaRepository<EvidenciaFotografica, Integer> {

    boolean existsByUrl(String url);

    Optional<EvidenciaFotografica> findByUrl (String url);
}
