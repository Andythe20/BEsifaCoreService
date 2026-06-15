# core-sifa — Testing Guide

## Stack de Testing

| Herramienta | Propósito |
|---|---|
| **JUnit 5 (Jupiter)** | Framework de tests |
| **Mockito** | Mocking de dependencias |
| **AssertJ** | Assertions fluidas y legibles |
| **Spring Boot Test** | Contexto de aplicación para tests de integración |
| **Spring Security Test** | @WithMockUser, seguridad en tests |
| **H2** | Base de datos en memoria para tests (perfil `test`) |
| **Testcontainers** | Contenedores Docker (MySQL) para integración realista |
| **JaCoCo** | Cobertura de código |
| **Maven Surefire** | Ejecutor de tests |

## Estructura de Tests

```
src/test/java/com/sifa/core_sifa/
  CoreSifaApplicationTests.java        # Smoke test de contexto
  config/
    AbstractIntegrationTest.java       # Clase base para tests de integración
    TestSecurityConfig.java            # Config de seguridad para tests
  controller/
    InfraccionControllerTest.java      # Tests de controlador (MockMvc)
    CitacionControllerTest.java
    TipoInfraccionControllerTest.java
  integration/
    InfraccionFlowIntegrationTest.java  # Test de flujo completo
  repository/
    IInfraccionRepositoryTest.java     # Tests de repositorios (H2)
    ICitacionRepositoryTest.java
  service/
    CitacionServiceTest.java           # Tests unitarios de servicios
    FiscalizadorPresenciaServiceTest.java
    TipoInfraccionServiceImplTest.java
    infraccion/
      InfraccionServiceImplTest.java
  util/
    TestDataFactory.java               # Fábrica de entidades para tests

src/test/resources/
  application-test.properties           # Config para perfil test (H2)
```

## Convenciones y Buenas Prácticas

### 1. Nomenclatura
- Clases de test: `{NombreClase}Test.java`
- Métodos de test: `{accion}_{contexto}_{resultado}`
- Paquetes: mismo package que la clase bajo test

### 2. Capas de Testing

#### Repository (Integración con H2)
- Extienden `AbstractIntegrationTest`
- Usan `@Autowired` para repositorios reales
- H2 en modo MySQL para compatibilidad de queries
- `@BeforeEach` limpia datos y siembra estado inicial

```java
@BeforeEach
void setUp() {
    repository.deleteAll();
    // seed data...
}
```

#### Service (Unitarios con Mockito)
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` para dependencias, `@InjectMocks` para el servicio
- Validar excepciones con `assertThatThrownBy`
- Verificar interacciones con `verify`

```java
@ExtendWith(MockitoExtension.class)
class MiServicioTest {
    @Mock private Repositorio repositorio;
    @InjectMocks private MiServicio servicio;

    @Test
    void metodo_cuandoAlgo_retornaEsperado() {
        ...
    }
}
```

#### Controller (MockMvc)
- `@WebMvcTest(Controlador.class)` + `@Import(TestSecurityConfig.class)`
- `@MockitoBean` para servicios
- Testear status HTTP, estructura JSON, headers

```java
@WebMvcTest(MiController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class MiControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private MiServicio servicio;
}
```

#### Integración (Flujo completo)
- Extienden `AbstractIntegrationTest`
- Usar `TestContainers` solo si se necesita MySQL real
- Probar flujo completo: request → BD → response

### 3. Test Data
- Usar `TestDataFactory` para crear entidades
- Evitar duplicación de construcción de objetos
- Siempre limpiar datos en `@BeforeEach`

### 4. Assertions
- Usar AssertJ (`.isEqualTo()`, `.hasSize()`, `.contains()`)
- NO usar JUnit assertions (`assertEquals`)
- Mensajes descriptivos en assertions clave

## Comandos

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar un test específico
./mvnw test -Dtest=InfraccionServiceImplTest

# Ejecutar tests de una clase con método específico
./mvnw test -Dtest=InfraccionServiceImplTest#crearInfraccion_withValidData_createsSuccessfully

# Generar reporte de cobertura (JaCoCo)
./mvnw verify

# Ver cobertura: abrir target/site/jacoco/index.html

# Ejecutar sin cache
./mvnw clean test

# Modo verbose
./mvnw test -X
```

## Reglas de Calidad

- Cobertura mínima: 0% (inicial), aumentar progresivamente
- Los tests nunca deben depender del orden de ejecución
- NO usar `Thread.sleep()` o timing en tests
- Cada test debe ser independiente y repetible
- Usar perfiles de Spring: `@ActiveProfiles("test")` activa H2 automáticamente

## Troubleshooting

**Error: `No qualifying bean of type`**  
→ Verificar que el perfil activo es `test` y que `TestSecurityConfig` está importado en `@WebMvcTest`

**Error: H2 no compatible con query**  
→ Usar `MODE=MySQL` en la URL de H2: `jdbc:h2:mem:testdb;MODE=MySQL`

**Error: `@MockitoBean` no resuelto**  
→ Usar `@MockitoBean` (Spring Boot 3.4+) o `@MockBean` (versiones anteriores)

**Tests lentos**  
→ Los tests de integración con `@SpringBootTest` cargan todo el contexto. Para cambios rápidos, usar `@WebMvcTest` o `@ExtendWith(MockitoExtension.class)`.
