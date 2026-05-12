package com.example.core_sifa.config;

import com.example.core_sifa.model.PropietarioVehiculo;
import com.example.core_sifa.model.TipoInfraccion;
import com.example.core_sifa.model.Vehiculo;
import com.example.core_sifa.repository.IPropietarioVehiculoRepository;
import com.example.core_sifa.repository.ITipoInfraccionRepository;
import com.example.core_sifa.repository.IVehiculoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/** Clase para poblar datos en las tablas vehiculos, propietarios vehiculos y tipo infracciones */
@Configuration
@Slf4j
public class DataInitializerConfig {

    @Bean
    @Transactional
    public CommandLineRunner initData(
            ITipoInfraccionRepository tipoInfraccionRepo,
            IPropietarioVehiculoRepository propietarioRepo,
            IVehiculoRepository vehiculoRepo
    ) {

        return args -> {
            log.info("========================================================");
            log.info("Verificando e inicializando Dataset de prueba en MySQL...");

            // 1. Población de Tipo de Infracción
            if (tipoInfraccionRepo.count() == 0) {
                TipoInfraccion tipoMalEstacionado = TipoInfraccion.builder()
                        .nombre("Mal Estacionado")
                        .disposicionInfringida("Art. 154, Ley de Tránsito 18.290")
                        .build();
                tipoInfraccionRepo.save(tipoMalEstacionado);
                log.info("[+] Tipo de Infracción 'Mal Estacionado' registrado.");
            } else {
                log.info("[-] Tipos de Infracción ya existen. Se omite inserción.");
            }

            // 2. Población del Propietario
            String rutPropietario = "9330521-3";
            PropietarioVehiculo propietario = propietarioRepo.findById(rutPropietario).orElseGet(() -> {
                PropietarioVehiculo nuevoPropietario = PropietarioVehiculo.builder()
                        .rut(rutPropietario)
                        .nombres("MARCELO ALEJANDRO")
                        .apellidos("SEITUN ACUNA")
                        .direccion("Avenida Libertad 1250, Depto 402")
                        .comuna("Viña del Mar")
                        .correo("marcelo.seitun@correo.cl")
                        .telefono("+56987654321")
                        .profesion("Particular")
                        .estadoCivil("Soltero/a")
                        .edad(42)
                        .build();

                PropietarioVehiculo guardado = propietarioRepo.save(nuevoPropietario);
                log.info("[+] Propietario RUT {} registrado exitosamente.", rutPropietario);
                return guardado;
            });

            // 3. Población del Vehículo
            String patente = "TZPW11";
            if (!vehiculoRepo.existsById(patente)) {
                Vehiculo vehiculo = Vehiculo.builder()
                        .patente(patente)
                        .marca("SUBARU")
                        .modelo("ALL NEW OUTBACK 2.4T FIEL")
                        .anioFabricacion(2025)
                        .color("BLANCO CRISTAL PERLADO")
                        .tipo("SUV / Station Wagon")
                        .nroMotor("CS55459")
                        .nroSerie("JF1BTAL83RG063261")
                        .propietarioVehiculo(propietario)
                        .build();

                vehiculoRepo.save(vehiculo);
                log.info("[+] Vehículo patente {} (Subaru Outback) registrado y enlazado al RUT {}.", patente, rutPropietario);
            } else {
                log.info("[-] Vehículo patente {} ya existe. Se omite inserción.", patente);
            }

            log.info("Dataset inicial revisado y/o cargado correctamente.");
            log.info("========================================================");
        };
    }
}