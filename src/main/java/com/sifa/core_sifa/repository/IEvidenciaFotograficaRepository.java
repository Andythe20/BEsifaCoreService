package com.sifa.core_sifa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.EvidenciaFotografica;

import java.util.Optional;

@Repository
public interface IEvidenciaFotograficaRepository extends JpaRepository<EvidenciaFotografica, Integer> {

    boolean existsByUrl(String url);

    Optional<EvidenciaFotografica> findByUrl (String url);
}
