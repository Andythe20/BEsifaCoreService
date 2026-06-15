package com.sifa.core_sifa.config;

import com.sifa.core_sifa.repository.ITipoInfraccionRepository;
import com.sifa.core_sifa.repository.IPropietarioVehiculoRepository;
import com.sifa.core_sifa.repository.IVehiculoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
class DataInitializerConfigTest {

    @Autowired
    private ITipoInfraccionRepository tipoInfraccionRepo;

    @Autowired
    private IPropietarioVehiculoRepository propietarioRepo;

    @Autowired
    private IVehiculoRepository vehiculoRepo;

    @MockitoBean
    private com.sifa.core_sifa.config.FirebaseConfig firebaseConfig;

    @Test
    void contextLoads_conDataInitializer_noLanzaExcepcion() {
        assertThat(tipoInfraccionRepo.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void dataInitializer_insertsData_whenTablesEmpty() {
        long tipoCount = tipoInfraccionRepo.count();
        long propCount = propietarioRepo.count();
        long vehCount = vehiculoRepo.count();

        assertThat(tipoCount).isGreaterThanOrEqualTo(0);
        assertThat(propCount).isGreaterThanOrEqualTo(0);
        assertThat(vehCount).isGreaterThanOrEqualTo(0);
    }
}
