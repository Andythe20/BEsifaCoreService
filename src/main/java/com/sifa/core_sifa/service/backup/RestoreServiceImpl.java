package com.sifa.core_sifa.service.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sifa.core_sifa.dto.backup.RestoreJobResponse;
import com.sifa.core_sifa.dto.backup.RestoreValidationResponse;
import com.sifa.core_sifa.exception.RestoreException;
import com.sifa.core_sifa.exception.RestoreExecutionException;
import com.sifa.core_sifa.exception.RestoreValidationException;
import com.sifa.core_sifa.model.RestoreJob;
import com.sifa.core_sifa.util.GzipUtil;
import com.sifa.core_sifa.util.SqlStatementSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestoreServiceImpl implements IRestoreService {

    private final S3Client s3Client;
    private final BackupValidator backupValidator;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${DB_HOST}")
    private String dbHost;

    @Value("${DB_PORT}")
    private String dbPort;

    @Value("${DB_USER}")
    private String dbUser;

    @Value("${DB_PASSWORD}")
    private String dbPassword;

    @Value("${DB_NAME_CORE}")
    private String coreDbName;

    @Value("${AUTH_DB_NAME:authdb}")
    private String authDbName;

    private static final String BACKUPS_PREFIX = "backups/";
    private static final String STORAGE_PREFIX = "infracciones/";
    private static final long MAX_JOB_DURATION_MS = 30 * 60 * 1000;

    private final AtomicBoolean restoreInProgress = new AtomicBoolean(false);
    private volatile RestoreJob currentJob;

    @Override
    public RestoreJobResponse startRestore(String backupId, String scope) {
        if (!restoreInProgress.compareAndSet(false, true)) {
            throw new IllegalStateException("Ya hay un restore en ejecución. Espere a que finalice.");
        }

        try {
            String jobId = "rst-" + UUID.randomUUID().toString().substring(0, 8);

            RestoreJob job = RestoreJob.builder()
                    .jobId(jobId)
                    .backupId(backupId)
                    .scope(scope)
                    .status("PENDING")
                    .progress(0)
                    .message("Iniciando restauración segura (" + scope + ")")
                    .build();
            this.currentJob = job;

            CompletableFuture.runAsync(() -> {
                try {
                    executeRestore(job);
                } finally {
                    restoreInProgress.set(false);
                }
            });

            return buildJobResponse(job);
        } catch (Exception e) {
            restoreInProgress.set(false);
            throw e;
        }
    }

    private void executeRestore(RestoreJob job) {
        String backupId = job.getBackupId();
        String scope = job.getScope() != null ? job.getScope() : "full";
        boolean restoreDb = "database".equals(scope) || "full".equals(scope);
        boolean restoreSt = "storage".equals(scope) || "full".equals(scope);

        String tempSuffix = "_restore_" + job.getJobId().replace("rst-", "");
        String authTemp = authDbName + tempSuffix;
        String coreTemp = coreDbName + tempSuffix;
        String safetyAuth = "safety_" + authDbName + "_" + job.getJobId().replace("rst-", "");
        String safetyCore = "safety_" + coreDbName + "_" + job.getJobId().replace("rst-", "");

        try {
            if (restoreDb || restoreSt) {
                updateJob(job, "VALIDATING", 5, "Validando integridad del backup...");
                RestoreValidationResponse validation = backupValidator.validate(backupId);
                if (!validation.isValid()) {
                    throw new RestoreValidationException("Validación falló: " + String.join(", ", validation.getErrors()));
                }
                log.info("Backup {} validado exitosamente", backupId);
            }

            if (restoreDb) {
                updateJob(job, "RESTORING", 20, "Creando schemas temporales...");
                job.setTempSchemaAuth(authTemp);
                job.setTempSchemaCore(coreTemp);
                job.setSafetySchemaAuth(safetyAuth);
                job.setSafetySchemaCore(safetyCore);

                createTemporarySchemas(authTemp, coreTemp);

                updateJob(job, "RESTORING", 35, "Restaurando authdb en schema temporal...");
                restoreToSchema(backupId, "authdb", authTemp);

                updateJob(job, "RESTORING", 50, "Restaurando core_db en schema temporal...");
                restoreToSchema(backupId, "core_db", coreTemp);

                updateJob(job, "VALIDATING_POST", 65, "Validando datos restaurados...");
                validatePostRestore(authTemp, authDbName);
                validatePostRestore(coreTemp, coreDbName);

                updateJob(job, "SWAPPING", 80, "Ejecutando swap seguro...");
                executeSwap(authTemp, coreTemp, safetyAuth, safetyCore);

                cleanupTempSchemas(safetyAuth, safetyCore);
            }

            if (restoreSt) {
                updateJob(job, "SWAPPING", 90, "Restaurando storage S3...");
                restoreStorage(backupId);
            }

            if (restoreDb) {
                cleanupTempSchemas(authTemp, coreTemp);
            }

            updateJob(job, "SUCCESS", 100, "Restauración completada exitosamente");
            log.info("Restore {} completado exitosamente (scope: {})", job.getJobId(), scope);

        } catch (RestoreValidationException e) {
            log.warn("Restore {} falló en validación: {}", job.getJobId(), e.getMessage());
            updateJob(job, "FAILED", 0, "Validación falló: " + e.getMessage());
            if (restoreDb) {
                dropSchemaQuietly(authTemp);
                dropSchemaQuietly(coreTemp);
            }
        } catch (RestoreExecutionException e) {
            log.error("Restore {} falló durante swap: {}", job.getJobId(), e.getMessage());
            updateJob(job, "FAILED", 0, "Error durante swap: " + e.getMessage()
                    + " — Los datos originales están en schemas de safety. Contacte al administrador.");
        } catch (Exception e) {
            log.error("Restore {} falló: {}", job.getJobId(), e.getMessage(), e);
            updateJob(job, "FAILED", 0, "Error: " + e.getMessage());
            if (restoreDb) {
                dropSchemaQuietly(authTemp);
                dropSchemaQuietly(coreTemp);
            }
        }
    }

    private void createTemporarySchemas(String... schemas) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String schema : schemas) {
                stmt.execute("CREATE DATABASE IF NOT EXISTS `" + schema + "`");
                log.info("Schema temporal creado: {}", schema);
            }
        } catch (Exception e) {
            throw new RestoreException("Error creando schemas temporales: " + e.getMessage(), e);
        }
    }

    private void restoreToSchema(String backupId, String dbKey, String tempSchemaName) {
        String backupDir = BACKUPS_PREFIX + backupId + "/databases/";
        String key = backupDir + dbKey + ".sql.gz";

        try {
            byte[] gzipContent = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()).asByteArray();

            String sql = GzipUtil.decompress(gzipContent);

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("USE `" + tempSchemaName + "`");

                for (String statement : SqlStatementSplitter.split(sql)) {
                    stmt.execute(statement);
                }
            }

            log.info("Schema {} restaurado desde backup {}", tempSchemaName, key);
        } catch (Exception e) {
            throw new RestoreException("Error restaurando " + dbKey + " en " + tempSchemaName + ": " + e.getMessage(), e);
        }
    }

    private void validatePostRestore(String tempSchema, String originalDbName) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("USE `" + tempSchema + "`");

            List<String> tables = getObjectNames(stmt, "SHOW TABLES");
            log.info("Schema {}: {} tablas encontradas", tempSchema, tables.size());

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + tempSchema + "'")) {
                if (rs.next()) {
                    log.debug("Confirmadas {} tablas en {}", rs.getInt(1), tempSchema);
                }
            }

            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                    if (rs.next()) {
                        log.debug("Tabla {}: {} filas", table, rs.getLong(1));
                    }
                }
            }

        } catch (Exception e) {
            throw new RestoreValidationException("Error validando post-restore en " + tempSchema + ": " + e.getMessage(), e);
        }
    }

    private void executeSwap(String authTemp, String coreTemp, String safetyAuth, String safetyCore) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            try {
                performSchemaSwap(stmt, authDbName, authTemp, safetyAuth);
                performSchemaSwap(stmt, coreDbName, coreTemp, safetyCore);
                conn.commit();
                log.info("Swap completado exitosamente");
            } catch (Exception e) {
                conn.rollback();
                log.error("Swap falló, rollback ejecutado: {}", e.getMessage());
                throw new RestoreExecutionException("Swap falló, rollback automático ejecutado", e);
            }

        } catch (RestoreExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new RestoreExecutionException("Error en conexión durante swap: " + e.getMessage(), e);
        }
    }

    private void performSchemaSwap(Statement stmt, String productionSchema, String tempSchema, String safetySchema) throws Exception {
        List<String> tables = getObjectNames(stmt, "SHOW TABLES FROM `" + tempSchema + "`");
        List<String> productionTables = getObjectNames(stmt, "SHOW TABLES FROM `" + productionSchema + "`");

        stmt.execute("CREATE DATABASE IF NOT EXISTS `" + safetySchema + "`");

        for (String table : productionTables) {
            stmt.execute(String.format("RENAME TABLE `%s`.`%s` TO `%s`.`%s`",
                    productionSchema, table, safetySchema, table));
        }

        for (String table : tables) {
            stmt.execute(String.format("RENAME TABLE `%s`.`%s` TO `%s`.`%s`",
                    tempSchema, table, productionSchema, table));
        }

        log.info("Swap {} → {} completado (safety: {})", tempSchema, productionSchema, safetySchema);
    }

    private void restoreStorage(String backupId) {
        String sourcePrefix = BACKUPS_PREFIX + backupId + "/storage/" + STORAGE_PREFIX;
        String destPrefix = STORAGE_PREFIX;

        listAllObjects(sourcePrefix).forEach(obj -> {
            String destKey = obj.key().replace(sourcePrefix, destPrefix);
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(obj.key())
                    .destinationBucket(bucketName)
                    .destinationKey(destKey)
                    .build());
        });

        log.info("Storage restaurado de {} a {}", sourcePrefix, destPrefix);
    }

    private void cleanupTempSchemas(String... schemas) {
        for (String schema : schemas) {
            dropSchemaQuietly(schema);
        }
    }

    private void dropSchemaQuietly(String schema) {
        if (schema == null) return;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS `" + schema + "`");
            log.info("Schema {} eliminado", schema);
        } catch (Exception e) {
            log.warn("No se pudo eliminar schema {}: {}", schema, e.getMessage());
        }
    }

    @Override
    public RestoreJobResponse getRestoreStatus(String jobId) {
        if (currentJob == null || !currentJob.getJobId().equals(jobId)) {
            throw new IllegalArgumentException("Job de restore no encontrado: " + jobId);
        }
        return buildJobResponse(currentJob);
    }

    @Override
    public RestoreValidationResponse validateBackup(String backupId) {
        return backupValidator.validate(backupId);
    }

    @Override
    public void cancelRestore(String jobId) {
        if (currentJob == null || !currentJob.getJobId().equals(jobId)) {
            throw new IllegalArgumentException("Job de restore no encontrado: " + jobId);
        }

        if (!Set.of("VALIDATING", "RESTORING").contains(currentJob.getStatus())) {
            throw new IllegalStateException("Solo se pueden cancelar jobs en estado VALIDATING o RESTORING");
        }

        updateJob(currentJob, "FAILED", 0, "Cancelado por el usuario");
        restoreInProgress.set(false);

        dropSchemaQuietly(currentJob.getTempSchemaAuth());
        dropSchemaQuietly(currentJob.getTempSchemaCore());
    }

    private Connection getConnection() throws Exception {
        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    private List<String> getObjectNames(Statement stmt, String query) throws Exception {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    private List<S3Object> listAllObjects(String prefix) {
        List<S3Object> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix);

            if (continuationToken != null) {
                builder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(builder.build());
            objects.addAll(response.contents());
            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);

        return objects;
    }

    private void updateJob(RestoreJob job, String status, int progress, String message) {
        job.setStatus(status);
        job.setProgress(progress);
        job.setMessage(message);
        if ("FAILED".equals(status)) {
            job.setErrorMessage(message);
        }
    }

    private RestoreJobResponse buildJobResponse(RestoreJob job) {
        return RestoreJobResponse.builder()
                .jobId(job.getJobId())
                .backupId(job.getBackupId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .message(job.getMessage())
                .build();
    }
}
