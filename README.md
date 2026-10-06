# Core API SIFA (Spring Boot)

Microservicio central del ecosistema SIFA encargado de la gestión de **Infracciones**, **Evidencias Fotográficas** y **Citaciones** del Juzgado de Policía Local (JPL).

Esta aplicación está diseñada para funcionar detrás de un **API Gateway**, el cual delega la identidad del usuario mediante cabeceras HTTP. Está conectada a una base de datos **MySQL** independiente y su configuración principal se realiza mediante variables de entorno.

## Tecnologías

- Java 21
- Spring Boot 3
- Spring Data JPA
- Spring Web (REST)
- Lombok
- Maven
- MySQL 8+
- Docker & Docker Compose

## Requisitos

- Java 21 instalado
- Maven 3.9+ (o usar `./mvnw`)
- MySQL 8+ en ejecución (si se ejecuta en local)
- Docker Desktop (opcional, para despliegue en contenedores)

## Variables de entorno

Debes definir las siguientes variables antes de ejecutar la aplicación. Puedes crear un archivo `.env` en la raíz del proyecto:

| Variable | Requerida | Descripción | Ejemplo |
|---|-----------|---|---|
| `DB_HOST` | Sí | Host de MySQL | `mysql` (Docker) o `localhost` |
| `DB_PORT` | Sí | Puerto interno de MySQL | `3306` |
| `DB_NAME` | Sí | Nombre de la base de datos del Core | `core_db` |
| `DB_USER` | Sí | Usuario de MySQL | `core_app` |
| `DB_PASSWORD` | Sí | Contraseña de MySQL | `password123` |
| `SERVER_PORT` | No | Puerto interno de la API (por defecto 8080) | `8080` |
| `EXTERNAL_PORT` | No | Puerto expuesto en Docker (para pruebas) | `8083` |

## Configuración de base de datos

La aplicación usa esta URL JDBC (definida en `application.properties`):

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=America/Santiago&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true&defaultAuthenticationPlugin=caching_sha2_password
```

Notas importantes:

- `createDatabaseIfNotExist=true` permite crear la base de datos si no existe.
- `spring.jpa.hibernate.ddl-auto=update` actualiza el esquema automáticamente.
- `defaultAuthenticationPlugin=caching_sha2_password` exige el estándar de encriptación moderno de MySQL 8.0+.


## Arquitectura y Seguridad (API Gateway)
Este microservicio no gestiona tokens JWT directamente. Asume que toda petición ha sido filtrada previamente por el API Gateway y que
viaja por el **canal interno** (cabecera `X-Internal-Key`).

Para consumir los endpoints de creación o búsqueda, el Gateway se encarga de:

1. Validar el JWT de sesión y regenerar `X-Auth-Identity` (token interno firmado, 60s) con los roles verificados — nunca confía en `X-Auth-User`/`X-Auth-Roles` planos del cliente.
2. Inyectar la cabecera `X-Internal-Key` del canal interno (`INTERNAL_CHANNEL_KEY`).

Cualquier petición que llegue **directa** a este microservicio sin pasar por el Gateway es rechazada:

- Sin `X-Internal-Key` válida → `403` JSON (lo descarta `ChannelKeyFilter` antes de cualquier lógica).
- Con `X-Internal-Key` válida pero sin `X-Auth-Identity` → `401` (no autenticada).

No existe la opción de "consumir desde Postman inyectando `X-Auth-User`": esas cabeceras planas ya no otorgan acceso. La única vía de
consumo es a través del Gateway; el `INTERNAL_CHANNEL_KEY` compartido es un secreto de despliegue y nunca debe distribuirse a clientes.


## Cómo ejecutar

### 1. Despliegue con Docker (Recomendado)

El proyecto está preparado para levantarse junto con su propia base de datos aislada en una red de Docker (`sifa-network`).

```bash
# Levantar el microservicio y su base de datos
docker-compose up -d --build

# Ver los logs
docker-compose logs -f core-sifa
```

La API quedará expuesta externamente en `http://localhost:8083`.

### 2. Ejecución Local (VS Code / Maven)

Asegúrate de tener un archivo `launch.json` configurado en tu carpeta `.vscode` que apunte a tu archivo `.env`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot-App",
      "request": "launch",
      "mainClass": "com.example.core_sifa.CoreSifaApplication",
      "envFile": "${workspaceFolder}/..env"
    }
  ]
}
```

Con Maven Wrapper:
```bash
./mvnw spring-boot:run
```


## Logs

Los logs se generan en:

- `logs/core-sifa.log`

## Solución rápida de problemas

- Error 400 (`MethodArgumentTypeMismatchException`): Asegúrate de enviar un texto (correo) en lugar de un UUID si consumes rutas dinámicas.
- Error 401 (`Unauthorized`): Si usas Spring Security en el Core, verifica que el archivo SecurityConfig esté permitiendo el tráfico (`permitAll()`), ya que el Gateway hace la autenticación.
- Error de Base de Datos (`Communications link failure`): Si estás en Docker, asegúrate de que el Gateway y el Core estén en la misma red (`sifa-network`) y que `DB_HOST` sea el nombre del contenedor de MySQL (`mysql`).

## Creación de usuario mysql (Si no usas Docker)

```sql
CREATE DATABASE IF NOT EXISTS core_db;
CREATE USER 'core_app'@'%' IDENTIFIED WITH caching_sha2_password BY '<password>';
GRANT ALL PRIVILEGES ON core_db.* TO 'core_app'@'%';
FLUSH PRIVILEGES;
```
