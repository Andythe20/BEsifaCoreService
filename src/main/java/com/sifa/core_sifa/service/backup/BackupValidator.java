package com.sifa.core_sifa.service.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sifa.core_sifa.dto.backup.RestoreValidationResponse;
import com.sifa.core_sifa.util.ChecksumUtil;
import com.sifa.core_sifa.util.GzipUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class BackupValidator {

    private final S3Client s3Client;
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

    private static final String BACKUPS_PREFIX = "backups/";
    private static final String METADATA_FILE = "metadata.json";

    public RestoreValidationResponse validate(String backupId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String backupPrefix = BACKUPS_PREFIX + backupId + "/";

        String metadataContent = getS3ObjectContent(backupPrefix + METADATA_FILE);
        if (metadataContent == null) {
            errors.add("Metadata no encontrada para el backup: " + backupId);
            return RestoreValidationResponse.invalid(errors);
        }

        Map<String, Object> metadata;
        try {
            metadata = objectMapper.readValue(metadataContent, Map.class);
        } catch (Exception e) {
            errors.add("Metadata inválida: " + e.getMessage());
            return RestoreValidationResponse.invalid(errors);
        }

        Integer backupVersion = (Integer) metadata.get("backupVersion");
        if (backupVersion == null || backupVersion < 2) {
            errors.add("Versión de backup no soportada.");
            return RestoreValidationResponse.invalid(errors);
        }

        String mysqlVersion = (String) metadata.get("mysqlVersion");

        List<Map<String, Object>> schemas = (List<Map<String, Object>>) metadata.get("schemas");
        if (schemas == null || schemas.isEmpty()) {
            errors.add("No se encontraron schemas en el backup");
            return RestoreValidationResponse.invalid(errors);
        }

        Map<String, String> checksums = (Map<String, String>) metadata.get("checksums");

        List<String> schemaNames = new ArrayList<>();
        for (Map<String, Object> schema : schemas) {
            String name = (String) schema.get("name");
            String dumpFile = (String) schema.get("dumpFile");
            String expectedChecksum = (String) schema.get("checksumSha256");
            schemaNames.add(name);

            String dumpKey = backupPrefix + dumpFile;
            byte[] dumpData = getS3ObjectBytes(dumpKey);
            if (dumpData == null) {
                errors.add("Dump no encontrado: " + dumpFile);
                continue;
            }

            if (!GzipUtil.isValidGzip(dumpData)) {
                errors.add("Formato gzip inválido para " + dumpFile);
                continue;
            }

            if (expectedChecksum != null) {
                String actualChecksum = ChecksumUtil.sha256(dumpData);
                if (!actualChecksum.equals(expectedChecksum)) {
                    errors.add("Checksum mismatch para " + dumpFile
                            + ". Esperado: " + expectedChecksum
                            + ", Actual: " + actualChecksum);
                }
            }

            try {
                GzipUtil.decompress(dumpData);
            } catch (Exception e) {
                errors.add("Error descomprimiendo " + dumpFile + ": " + e.getMessage());
            }
        }

        String currentMysqlVersion = getCurrentMysqlVersion();
        if (currentMysqlVersion != null && mysqlVersion != null) {
            if (!currentMysqlVersion.startsWith(mysqlVersion.substring(0, 3))) {
                warnings.add("Versión MySQL actual (" + currentMysqlVersion
                        + ") difiere del backup (" + mysqlVersion + ")");
            }
        }

        if (!errors.isEmpty()) {
            return RestoreValidationResponse.invalid(errors);
        }

        return RestoreValidationResponse.valid(backupId, mysqlVersion, schemaNames, warnings);
    }

    private String getS3ObjectContent(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build())
                    .asUtf8String();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    private byte[] getS3ObjectBytes(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build())
                    .asByteArray();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    private String getCurrentMysqlVersion() {
        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener versión MySQL: {}", e.getMessage());
        }
        return null;
    }
}
