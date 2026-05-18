package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.TipoInfraccionDTO;

public interface ITipoInfraccionService {

    TipoInfraccionDTO create(TipoInfraccionDTO dto);
    TipoInfraccionDTO update(Integer id, TipoInfraccionDTO dto);
    void delete(Integer id);
}
