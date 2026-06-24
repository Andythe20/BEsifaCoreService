package com.sifa.core_sifa.service.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sifa.core_sifa.dto.backup.BackupJobResponse;
import com.sifa.core_sifa.dto.backup.BackupListResponse;
import com.sifa.core_sifa.dto.backup.DownloadResponse;
import com.sifa.core_sifa.model.BackupJob;
import com.sifa.core_sifa.repository.IBackupJobRepository;
import com.sifa.core_sifa.util.ChecksumUtil;
import com.sifa.core_sifa.util.GzipUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackupServiceImpl implements IBackupService {

    private final IBackupJobRepository jobRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
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
    private static final String METADATA_FILE = "metadata.json";
    private static final List<String> DATABASES = List.of("authdb", "core_db");

    @Override
    public BackupJobResponse createFullBackup(String createdBy) {
        String backupId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String jobId = "bkp-" + UUID.randomUUID().toString().substring(0, 8);

        BackupJob job = BackupJob.builder()
                .jobId(jobId)
                .backupId(backupId)
                .status("PENDING")
                .progress(0)
                .message("Iniciando backup completo")
                .build();
        jobRepository.save(job);

        CompletableFuture.runAsync(() -> executeFullBackup(job, createdBy));

        return buildJobResponse(job);
    }

    protected void executeFullBackup(BackupJob job) {
        executeFullBackup(job, null);
    }

    protected void executeFullBackup(BackupJob job, String createdBy) {
        long startTime = System.currentTimeMillis();
        try {
            updateJob(job, "RUNNING", 5, "Respaldando bases de datos");
            List<SchemaInfo> schemaInfos = backupDatabases(job);
            updateJob(job, "RUNNING", 50, "Copiando archivos de storage a S3");
            S3Info s3Info = backupStorage(job);
            updateJob(job, "RUNNING", 90, "Generando metadatos");
            long totalSizeBytes = schemaInfos.stream().mapToLong(s -> s.sizeBytes).sum() + s3Info.totalSizeBytes;
            long durationMs = System.currentTimeMillis() - startTime;
            uploadMetadata(job, schemaInfos, totalSizeBytes, durationMs, s3Info, "automatic", createdBy, null);
            updateJob(job, "SUCCESS", 100, "Backup completado exitosamente");
            log.info("Backup {} completado exitosamente ({} ms)", job.getBackupId(), durationMs);
        } catch (Exception e) {
            log.error("Error en backup {}: {}", job.getBackupId(), e.getMessage(), e);
            updateJob(job, "FAILED", 0, "Error: " + e.getMessage());
        }
    }

    private List<SchemaInfo> backupDatabases(BackupJob job) {
        String backupDir = BACKUPS_PREFIX + job.getBackupId() + "/databases/";
        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
        List<SchemaInfo> schemaInfos = new ArrayList<>();

        for (String db : DATABASES) {
            String dbName = db.equals("core_db") ? coreDbName : authDbName;
            Path tempSql = null;

            try {
                tempSql = Files.createTempFile("backup-" + db, ".sql");

                Map<String, List<String>> inventory = new LinkedHashMap<>();
                long totalRows = 0;

                try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                     Statement stmt = conn.createStatement();
                     Writer w = Files.newBufferedWriter(tempSql, StandardCharsets.UTF_8)) {

                    stmt.execute("USE `" + dbName + "`");

                    inventory.putAll(buildObjectInventory(stmt, dbName));

                    w.write("-- SIFA Backup - Database: " + dbName + "\n");
                    w.write("-- Generated: " + LocalDateTime.now() + "\n\n");
                    w.write("SET FOREIGN_KEY_CHECKS = 0;\n");
                    w.write("SET @@SESSION.SQL_LOG_BIN = 0;\n\n");

                    for (String table : inventory.get("tables")) {
                        try (ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                            if (rs.next()) {
                                w.write(rs.getString(2) + ";\n\n");
                            }
                        }
                        totalRows += writeTableData(stmt, table, w);
                    }

                    for (String view : inventory.get("views")) {
                        try (ResultSet rs = stmt.executeQuery("SHOW CREATE VIEW `" + view + "`")) {
                            if (rs.next()) {
                                String ddl = rs.getString(2).replaceAll("(?i)DEFINER\\s*=\\s*\\S+", "");
                                w.write(ddl + ";\n\n");
                            }
                        }
                    }

                    for (String trigger : inventory.get("triggers")) {
                        try (ResultSet rs = stmt.executeQuery("SHOW CREATE TRIGGER `" + trigger + "`")) {
                            if (rs.next()) {
                                String ddl = rs.getString(3).replaceAll("(?i)DEFINER\\s*=\\s*\\S+", "");
                                w.write(ddl + "\n\n");
                            }
                        }
                    }

                    for (String proc : inventory.get("procedures")) {
                        String ddl = getCreateRoutine(stmt, "PROCEDURE", proc);
                        if (ddl != null) {
                            w.write(ddl + "\n\n");
                        }
                    }

                    for (String func : inventory.get("functions")) {
                        String ddl = getCreateRoutine(stmt, "FUNCTION", func);
                        if (ddl != null) {
                            w.write(ddl + "\n\n");
                        }
                    }

                    for (String event : inventory.get("events")) {
                        try (ResultSet rs = stmt.executeQuery("SHOW CREATE EVENT `" + event + "`")) {
                            if (rs.next()) {
                                String ddl = rs.getString(3).replaceAll("(?i)DEFINER\\s*=\\s*\\S+", "");
                                w.write(ddl + "\n\n");
                            }
                        }
                    }

                    w.write("SET FOREIGN_KEY_CHECKS = 1;\n");
                }

                byte[] sqlBytes = Files.readAllBytes(tempSql);
                byte[] gzBytes = GzipUtil.compress(sqlBytes);
                String checksum = ChecksumUtil.sha256(gzBytes);
                long size = gzBytes.length;

                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(backupDir + db + ".sql.gz")
                                .build(),
                        RequestBody.fromBytes(gzBytes));

                log.info("Base de datos {} respaldada en {}{} ({} bytes, sha256: {})",
                        db, backupDir, db + ".sql.gz", size, checksum);

                schemaInfos.add(new SchemaInfo(dbName, "databases/" + db + ".sql.gz",
                        size, checksum, totalRows, new LinkedHashMap<>(inventory)));
            } catch (Exception e) {
                throw new RuntimeException("Error respaldando " + db + ": " + e.getMessage(), e);
            } finally {
                if (tempSql != null) {
                    try { Files.deleteIfExists(tempSql); } catch (IOException ignored) {}
                }
            }
        }

        return schemaInfos;
    }

    private S3Info backupStorage(BackupJob job) {
        String sourcePrefix = STORAGE_PREFIX;
        String destPrefix = BACKUPS_PREFIX + job.getBackupId() + "/storage/" + STORAGE_PREFIX;

        List<S3Object> objects = listAllObjects(sourcePrefix);
        long totalSize = 0;

        for (S3Object obj : objects) {
            String destKey = obj.key().replace(sourcePrefix, destPrefix);
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(obj.key())
                    .destinationBucket(bucketName)
                    .destinationKey(destKey)
                    .build());
            totalSize += obj.size();
        }

        log.info("Storage copiado de {} a {} ({} objetos, {} bytes)",
                sourcePrefix, destPrefix, objects.size(), totalSize);

        return new S3Info(objects.size(), totalSize);
    }

    private void uploadMetadata(BackupJob job, List<SchemaInfo> schemaInfos, long totalSizeBytes,
                                 long durationMs, S3Info s3Info) {
        uploadMetadata(job, schemaInfos, totalSizeBytes, durationMs, s3Info, "automatic", null, null);
    }

    private void uploadMetadata(BackupJob job, List<SchemaInfo> schemaInfos, long totalSizeBytes,
                                 long durationMs, S3Info s3Info, String source, String createdBy,
                                 String description) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("backupVersion", 2);
            root.put("backupId", job.getBackupId());
            root.put("createdAt", LocalDateTime.now().toString());
            root.put("mysqlVersion", getMysqlVersion());
            root.put("jobId", job.getJobId());
            root.put("totalSizeBytes", totalSizeBytes);
            root.put("buildDurationMs", durationMs);
            if (source != null) root.put("source", source);
            if (createdBy != null) root.put("createdBy", createdBy);
            if (description != null) root.put("description", description);

            ArrayNode compatibleVersions = root.putArray("compatibleVersions");
            compatibleVersions.add("8.0.x");

            ArrayNode schemasArray = root.putArray("schemas");
            for (SchemaInfo si : schemaInfos) {
                ObjectNode schemaNode = schemasArray.addObject();
                schemaNode.put("name", si.name);
                schemaNode.put("dumpFile", si.dumpFile);
                schemaNode.put("sizeBytes", si.sizeBytes);
                schemaNode.put("checksumSha256", si.checksumSha256);
                schemaNode.put("rowCount", si.rowCount);

                ObjectNode objectsNode = schemaNode.putObject("objects");
                for (Map.Entry<String, List<String>> entry : si.inventory.entrySet()) {
                    ArrayNode arr = objectsNode.putArray(entry.getKey());
                    for (String item : entry.getValue()) {
                        arr.add(item);
                    }
                }
            }

            ObjectNode checksumsNode = root.putObject("checksums");
            ObjectNode sizesNode = root.putObject("sizes");
            for (SchemaInfo si : schemaInfos) {
                checksumsNode.put(si.dumpFile, si.checksumSha256);
                sizesNode.put(si.dumpFile, si.sizeBytes);
            }

            ObjectNode s3Node = root.putObject("s3");
            s3Node.put("bucket", bucketName);
            s3Node.put("region", System.getenv("AWS_S3_REGION"));
            ArrayNode prefixesArray = s3Node.putArray("prefixes");
            ObjectNode prefixNode = prefixesArray.addObject();
            prefixNode.put("prefix", STORAGE_PREFIX);
            prefixNode.put("objectCount", s3Info.objectCount);
            prefixNode.put("totalSizeBytes", s3Info.totalSizeBytes);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            String key = BACKUPS_PREFIX + job.getBackupId() + "/" + METADATA_FILE;

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(json));

            log.info("Metadata subida a {}", key);
        } catch (Exception e) {
            throw new RuntimeException("Error generando metadata: " + e.getMessage(), e);
        }
    }

    private String getMysqlVersion() {
        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
            if (rs.next()) return rs.getString(1);
        } catch (Exception e) {
            log.warn("No se pudo obtener versión MySQL: {}", e.getMessage());
        }
        return "unknown";
    }

    private Map<String, List<String>> buildObjectInventory(Statement stmt, String dbName) throws Exception {
        Map<String, List<String>> inventory = new LinkedHashMap<>();

        inventory.put("tables", getObjectNames(stmt, "SHOW TABLES FROM `" + dbName + "`"));

        inventory.put("views", getObjectNames(stmt,
                "SHOW FULL TABLES FROM `" + dbName + "` WHERE Table_Type = 'VIEW'"));

        inventory.put("triggers", getObjectNames(stmt,
                "SHOW TRIGGERS FROM `" + dbName + "`"));

        inventory.put("procedures", getObjectNamesFromInfoSchema(stmt, dbName, "PROCEDURE"));

        inventory.put("functions", getObjectNamesFromInfoSchema(stmt, dbName, "FUNCTION"));

        inventory.put("events", getObjectNames(stmt,
                "SHOW EVENTS FROM `" + dbName + "`"));

        return inventory;
    }

    private List<String> getObjectNames(Statement stmt, String query) throws Exception {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }

    private List<String> getObjectNamesFromInfoSchema(Statement stmt, String dbName, String type) throws Exception {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery(
                "SELECT ROUTINE_NAME FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA = '" + dbName + "' AND ROUTINE_TYPE = '" + type + "'")) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }

    private long writeTableData(Statement stmt, String table, Writer w) throws Exception {
        List<String> colNames = new ArrayList<>();
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "` LIMIT 0")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                colNames.add(meta.getColumnName(i));
            }
        }

        StringBuilder cols = new StringBuilder();
        for (String c : colNames) {
            if (cols.length() > 0) cols.append(", ");
            cols.append("`").append(c).append("`");
        }

        long rowCount = 0;
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
            int batch = 0;
            StringBuilder ins = new StringBuilder();
            while (rs.next()) {
                rowCount++;
                if (batch == 0) {
                    ins.setLength(0);
                    ins.append("INSERT INTO `").append(table).append("` (").append(cols).append(") VALUES ");
                } else {
                    ins.append(",\n");
                }
                ins.append("(");
                for (int i = 1; i <= colNames.size(); i++) {
                    if (i > 1) ins.append(", ");
                    Object val = rs.getObject(i);
                    if (val == null) {
                        ins.append("NULL");
                    } else if (val instanceof Number || val instanceof Boolean) {
                        ins.append(val.toString());
                    } else if (val instanceof byte[]) {
                        ins.append("X'").append(bytesToHex((byte[]) val)).append("'");
                    } else {
                        String s = val.toString();
                        ins.append("'").append(s.replace("\\", "\\\\").replace("'", "\\'")).append("'");
                    }
                }
                ins.append(")");
                batch++;

                if (batch >= 100) {
                    w.write(ins.toString() + ";\n");
                    batch = 0;
                }
            }
            if (batch > 0) w.write(ins.toString() + ";\n");
            w.write("\n");
        }

        return rowCount;
    }

    private String getCreateRoutine(Statement stmt, String type, String name) throws Exception {
        try (ResultSet rs = stmt.executeQuery("SHOW CREATE " + type + " `" + name + "`")) {
            if (rs.next()) {
                String ddl = rs.getString(3);
                return ddl.replaceAll("(?i)DEFINER\\s*=\\s*\\S+", "");
            }
        }
        return null;
    }

    @Override
    public BackupJobResponse getJobStatus(String jobId) {
        BackupJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job no encontrado: " + jobId));
        return buildJobResponse(job);
    }

    @Override
    public List<BackupListResponse> listBackups() {
        List<String> prefixes = listCommonPrefixes(BACKUPS_PREFIX);
        List<BackupListResponse> backups = new ArrayList<>();

        for (String prefix : prefixes) {
            try {
                String metadataContent = getObjectContent(prefix + METADATA_FILE);
                if (metadataContent != null) {
                    BackupListResponse response = parseMetadata(metadataContent);
                    backups.add(response);
                } else {
                    String backupId = prefix.replace(BACKUPS_PREFIX, "").replace("/", "");
                    backups.add(BackupListResponse.builder()
                            .id(backupId)
                            .createdAt(backupId.replace("_", "T"))
                            .databases(DATABASES)
                            .build());
                }
            } catch (Exception e) {
                log.warn("Error leyendo metadata de {}", prefix, e);
            }
        }

        backups.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return backups;
    }

    @Override
    public DownloadResponse downloadBackup(String backupId) {
        String prefix = BACKUPS_PREFIX + backupId + "/";

        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("backup-" + backupId, ".zip");
            List<S3Object> objects = listAllObjects(prefix);

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip.toFile()))) {
                for (S3Object obj : objects) {
                    String entryName = obj.key().replace(prefix, "");
                    if (!entryName.isEmpty()) {
                        zos.putNextEntry(new ZipEntry(entryName));
                        byte[] content = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(obj.key())
                                .build()).asByteArray();
                        zos.write(content);
                        zos.closeEntry();
                    }
                }
            }

            String zipKey = BACKUPS_PREFIX + backupId + "/" + backupId + ".zip";
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(zipKey)
                            .build(),
                    RequestBody.fromFile(tempZip));

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(r -> r
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(zipKey)
                            .build()));

            return DownloadResponse.builder()
                    .url(presigned.url().toString())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error generando descarga: " + e.getMessage(), e);
        } finally {
            if (tempZip != null) {
                try { Files.deleteIfExists(tempZip); } catch (IOException ignored) {}
            }
        }
    }

    private record SchemaInfo(String name, String dumpFile, long sizeBytes,
                               String checksumSha256, long rowCount,
                               Map<String, List<String>> inventory) {}

    private record S3Info(int objectCount, long totalSizeBytes) {}

    private void createSafetyBackup() {
        String safetyBackupId = "pre-restore-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        log.info("Creando backup de seguridad {}", safetyBackupId);
        BackupJob safetyJob = BackupJob.builder()
                .backupId(safetyBackupId)
                .build();
        backupDatabases(safetyJob);
        backupStorage(safetyJob);
    }

    @Override
    public BackupJobResponse uploadRestore(Path zipPath) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String jobId = "upl-" + UUID.randomUUID().toString().substring(0, 8);

        BackupJob job = BackupJob.builder()
                .jobId(jobId)
                .backupId("upload-" + timestamp)
                .status("PENDING")
                .progress(0)
                .message("Iniciando restauración desde archivo subido")
                .build();
        jobRepository.save(job);

        CompletableFuture.runAsync(() -> executeUploadRestore(job, zipPath));

        return buildJobResponse(job);
    }

    @Override
    public BackupJobResponse uploadBackup(Path zipPath) {
        return uploadBackup(zipPath, null, null);
    }

    @Override
    public BackupJobResponse uploadBackup(Path zipPath, String createdBy, String description) {
        if (description != null) {
            description = description.trim();
            if (description.length() > 60) description = description.substring(0, 60);
            if (description.isEmpty()) description = null;
        }
        String backupId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + "-upload";
        String jobId = "upb-" + UUID.randomUUID().toString().substring(0, 8);

        BackupJob job = BackupJob.builder()
                .jobId(jobId)
                .backupId(backupId)
                .status("PENDING")
                .progress(0)
                .message("Iniciando subida de backup")
                .build();
        jobRepository.save(job);

        String capturedCreatedBy = createdBy;
        String capturedDescription = description;

        CompletableFuture.runAsync(() -> executeUploadBackup(job, zipPath, capturedCreatedBy, capturedDescription));

        return buildJobResponse(job);
    }

    private void executeUploadBackup(BackupJob job, Path zipPath, String createdBy, String description) {
        Path extractDir = null;
        try {
            updateJob(job, "RUNNING", 5, "Extrayendo archivo ZIP");
            extractDir = Files.createTempDirectory("backup-upload-");
            validateAndExtractZip(zipPath, extractDir);

            String backupId = job.getBackupId();
            String backupPrefix = BACKUPS_PREFIX + backupId + "/";

            AtomicLong totalSizeBytes = new AtomicLong(0);

            updateJob(job, "RUNNING", 25, "Subiendo bases de datos a S3");
            Path databasesDir = extractDir.resolve("databases");
            if (Files.isDirectory(databasesDir)) {
                try (var files = Files.list(databasesDir)) {
                    files.filter(f -> f.toString().endsWith(".sql.gz")).forEach(sqlGz -> {
                        try {
                            byte[] content = Files.readAllBytes(sqlGz);
                            totalSizeBytes.addAndGet(content.length);
                            String dbKey = sqlGz.getFileName().toString();
                            s3Client.putObject(PutObjectRequest.builder()
                                            .bucket(bucketName)
                                            .key(backupPrefix + "databases/" + dbKey)
                                            .build(),
                                    RequestBody.fromBytes(content));
                            log.info("Archivo {} subido a S3", dbKey);
                        } catch (IOException e) {
                            throw new RuntimeException("Error subiendo " + sqlGz.getFileName(), e);
                        }
                    });
                }
            }

            updateJob(job, "RUNNING", 50, "Subiendo archivos de storage a S3");
            Path storageDir = extractDir.resolve("storage");
            if (Files.isDirectory(storageDir)) {
                try (var files = Files.walk(storageDir)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        try {
                            long fileSize = Files.size(file);
                            totalSizeBytes.addAndGet(fileSize);
                            String relativeKey = backupPrefix + "storage/" + storageDir.relativize(file).toString().replace("\\", "/");
                            s3Client.putObject(PutObjectRequest.builder()
                                            .bucket(bucketName)
                                            .key(relativeKey)
                                            .build(),
                                    RequestBody.fromFile(file));
                        } catch (IOException e) {
                            throw new RuntimeException("Error subiendo " + file.getFileName(), e);
                        }
                    });
                }
            }

            updateJob(job, "RUNNING", 75, "Generando metadatos");
            uploadSimpleMetadata(job, backupId, backupPrefix, totalSizeBytes.get(), "uploaded", createdBy, description);

            updateJob(job, "SUCCESS", 100, "Backup subido exitosamente");
            log.info("Upload-backup {} completado", job.getJobId());
        } catch (Exception e) {
            log.error("Error en upload-backup {}: {}", job.getJobId(), e.getMessage(), e);
            updateJob(job, "FAILED", 0, "Error: " + e.getMessage());
        } finally {
            cleanupQuietly(zipPath);
            if (extractDir != null) cleanupDirQuietly(extractDir);
        }
    }

    private void uploadSimpleMetadata(BackupJob job, String backupId, String backupPrefix,
                                       long totalSizeBytes, String source, String createdBy,
                                       String description) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("backupVersion", 2);
            root.put("backupId", backupId);
            root.put("createdAt", LocalDateTime.now().toString());
            root.put("mysqlVersion", getMysqlVersion());
            root.put("jobId", job.getJobId());
            root.put("totalSizeBytes", totalSizeBytes);
            if (source != null) root.put("source", source);
            if (createdBy != null) root.put("createdBy", createdBy);
            if (description != null) root.put("description", description);

            ArrayNode schemasArray = root.putArray("schemas");
            for (String db : DATABASES) {
                ObjectNode schemaNode = schemasArray.addObject();
                schemaNode.put("name", db);
                schemaNode.put("dumpFile", "databases/" + db + ".sql.gz");
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(backupPrefix + METADATA_FILE)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(json));

            log.info("Metadata generada para backup {}", backupId);
        } catch (Exception e) {
            throw new RuntimeException("Error generando metadata: " + e.getMessage(), e);
        }
    }

    private void executeUploadRestore(BackupJob job, Path zipPath) {
        Path extractDir = null;
        try {
            updateJob(job, "RUNNING", 5, "Validando archivo subido");
            extractDir = Files.createTempDirectory("restore-upload-");
            validateAndExtractZip(zipPath, extractDir);

            updateJob(job, "RUNNING", 15, "Creando backup de seguridad antes de restaurar");
            createSafetyBackup();

            updateJob(job, "RUNNING", 30, "Restaurando bases de datos desde archivo");
            restoreDatabasesFromZip(extractDir, job);

            Path storageDir = extractDir.resolve("storage");
            if (Files.isDirectory(storageDir)) {
                updateJob(job, "RUNNING", 60, "Restaurando archivos de storage desde archivo");
                restoreStorageFromZip(storageDir, job);
            } else {
                log.info("No se encontró directorio storage en el zip, se omite restauración de storage");
            }

            updateJob(job, "SUCCESS", 100, "Restauración desde archivo completada exitosamente");
            log.info("Upload-restore {} completado", job.getJobId());
        } catch (Exception e) {
            log.error("Error en upload-restore {}: {}", job.getJobId(), e.getMessage(), e);
            updateJob(job, "FAILED", 0, "Error: " + e.getMessage());
        } finally {
            cleanupQuietly(zipPath);
            if (extractDir != null) cleanupDirQuietly(extractDir);
        }
    }

    private void validateAndExtractZip(Path zipPath, Path extractDir) {
        if (!Files.exists(zipPath) || !zipPath.toString().endsWith(".zip")) {
            throw new IllegalArgumentException("El archivo debe ser un ZIP válido");
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            boolean hasDatabases = false;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();

                if (name.startsWith("databases/") && name.endsWith(".sql.gz")) {
                    hasDatabases = true;
                }

                Path target = extractDir.resolve(name).normalize();
                if (!target.startsWith(extractDir)) {
                    throw new SecurityException("Zip path traversal detected: " + name);
                }
                Files.createDirectories(target.getParent());
                Files.copy(zis, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }

            if (!hasDatabases) {
                throw new IllegalArgumentException("El ZIP no contiene archivos de base de datos en databases/");
            }
        } catch (SecurityException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al procesar el archivo ZIP: " + e.getMessage(), e);
        }
    }

    private void restoreDatabasesFromZip(Path extractDir, BackupJob job) {
        Path databasesDir = extractDir.resolve("databases");
        if (!Files.isDirectory(databasesDir)) {
            throw new IllegalArgumentException("No se encontró el directorio databases/ en el ZIP");
        }

        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";

        try (var files = Files.list(databasesDir)) {
            List<Path> sqlGzFiles = files
                    .filter(f -> f.toString().endsWith(".sql.gz"))
                    .toList();

            if (sqlGzFiles.isEmpty()) {
                throw new IllegalArgumentException("No se encontraron archivos .sql.gz en databases/");
            }

            for (Path sqlGz : sqlGzFiles) {
                String fileName = sqlGz.getFileName().toString();
                String dbKey = fileName.replace(".sql.gz", "");

                String dbName = dbKey.equals("core_db") ? coreDbName : authDbName;

                log.info("Restaurando base de datos {} desde archivo {}", dbName, fileName);
                updateJob(job, "RUNNING", 30 + (int)(20.0 * (sqlGzFiles.indexOf(sqlGz) + 1) / sqlGzFiles.size()),
                        "Restaurando base de datos " + fileName);

                String sql;
                try (FileInputStream fis = new FileInputStream(sqlGz.toFile());
                     GZIPInputStream gzis = new GZIPInputStream(fis);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    gzis.transferTo(baos);
                    sql = baos.toString(StandardCharsets.UTF_8);
                }

                try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP DATABASE IF EXISTS `" + dbName + "`");

                    for (String statement : sql.split(";\n")) {
                        String s = statement.trim();
                        if (!s.isEmpty()) {
                            stmt.execute(s);
                        }
                    }
                }

                log.info("Base de datos {} restaurada exitosamente desde upload", dbName);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error restaurando bases de datos desde archivo: " + e.getMessage(), e);
        }
    }

    private void restoreStorageFromZip(Path storageDir, BackupJob job) {
        try (var files = Files.walk(storageDir)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                String s3Key = storageDir.relativize(file).toString().replace("\\", "/");
                String contentType = s3Key.endsWith(".jpg") || s3Key.endsWith(".jpeg") ? "image/jpeg"
                        : s3Key.endsWith(".png") ? "image/png"
                        : s3Key.endsWith(".mp4") ? "video/mp4"
                        : "application/octet-stream";

                s3Client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(s3Key)
                                .contentType(contentType)
                                .build(),
                        RequestBody.fromFile(file));

                log.debug("Archivo storage subido a S3: {}", s3Key);
            });
        } catch (Exception e) {
            throw new RuntimeException("Error restaurando storage desde archivo: " + e.getMessage(), e);
        }
    }

    private static void cleanupQuietly(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }
    }

    private static void cleanupDirQuietly(Path dir) {
        try (var files = Files.walk(dir)) {
            files.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    @Override
    public void deleteBackup(String backupId) {
        String prefix = BACKUPS_PREFIX + backupId + "/";
        List<S3Object> objects = listAllObjects(prefix);

        for (S3Object obj : objects) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(obj.key())
                    .build());
        }

        log.info("Backup {} eliminado ({} objetos)", backupId, objects.size());
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

    private List<String> listCommonPrefixes(String prefix) {
        ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .delimiter("/")
                .build());

        return response.commonPrefixes().stream()
                .map(cp -> cp.prefix())
                .collect(Collectors.toList());
    }

    private String getObjectContent(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()).asUtf8String();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    private BackupListResponse parseMetadata(String json) {
        try {
            Map<String, Object> metadata = objectMapper.readValue(json, Map.class);
            String id = (String) metadata.getOrDefault("backupId", "");
            String createdAt = (String) metadata.getOrDefault("createdAt", "");
            Number totalSizeBytes = (Number) metadata.get("totalSizeBytes");
            String description = (String) metadata.get("description");
            String source = (String) metadata.get("source");
            String createdBy = (String) metadata.get("createdBy");

            List<String> databases = new ArrayList<>();
            List<Map<String, Object>> schemas = (List<Map<String, Object>>) metadata.get("schemas");
            if (schemas != null) {
                for (Map<String, Object> schema : schemas) {
                    databases.add((String) schema.get("name"));
                }
            }
            if (databases.isEmpty()) {
                databases = DATABASES;
            }

            return BackupListResponse.builder()
                    .id(id)
                    .createdAt(createdAt)
                    .databases(databases)
                    .totalSizeBytes(totalSizeBytes != null ? totalSizeBytes.longValue() : null)
                    .description(description)
                    .source(source)
                    .createdBy(createdBy)
                    .build();
        } catch (Exception e) {
            log.warn("Error parseando metadata JSON: {}", e.getMessage());
            return BackupListResponse.builder()
                    .id("")
                    .createdAt("")
                    .databases(DATABASES)
                    .build();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void updateJob(BackupJob job, String status, int progress, String message) {
        BackupJob managed = jobRepository.findByJobId(job.getJobId())
                .orElseThrow(() -> new RuntimeException("Job no encontrado: " + job.getJobId()));
        managed.setStatus(status);
        managed.setProgress(progress);
        managed.setMessage(message);
        if ("FAILED".equals(status)) {
            managed.setErrorMessage(message);
        }
        jobRepository.save(managed);
    }

    private BackupJobResponse buildJobResponse(BackupJob job) {
        return BackupJobResponse.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .message(job.getMessage())
                .build();
    }

}
