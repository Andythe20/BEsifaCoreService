package com.example.core_sifa.service.storage;

import com.example.core_sifa.service.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("prod")
@Slf4j
@RequiredArgsConstructor
public class S3StorageServiceImpl implements IStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private static final String FOLDER_PREFIX = "infracciones/";

    /**
     * Sube un archivo al bucket S3.
     *
     * La estructura final queda:
     *
     * infracciones/ABCD11_20260511_131500_1.jpg
     *
     * Mucho más legible y fácil de rastrear.
     */
    @Override
    public String uploadFile(MultipartFile file, String fileName) {

        String extension = getFileExtension(file.getOriginalFilename());

        // Armamos la key final del archivo
        String key = FOLDER_PREFIX + fileName + "." + extension;

        try {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes()));

            String url = buildFileUrl(key);

            log.info("Archivo subido correctamente: {}", url);

            return url;

        } catch (IOException e) {

            log.error("Error subiendo archivo a S3", e);

            throw new RuntimeException("No se pudo subir el archivo", e);
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String patente) {

        List<String> urls = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {

            MultipartFile file = files.get(i);

            String extension = getFileExtension(file.getOriginalFilename());

            // Nombre limpio del archivo
            String fileName = buildFileName(
                    patente,
                    i + 1);

            String key = FOLDER_PREFIX + fileName + "." + extension;

            try {

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(
                        putObjectRequest,
                        RequestBody.fromBytes(file.getBytes()));

                String url = buildFileUrl(key);

                urls.add(url);

                log.info("Archivo subido correctamente: {}", url);

            } catch (IOException e) {

                log.error("Error subiendo archivo a S3", e);

                throw new RuntimeException("No se pudo subir el archivo", e);
            }
        }

        return urls;
    }

    @Override
    public void deleteFile(String fileUrl) {

        String key = extractKeyFromUrl(fileUrl);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);

        log.info("Archivo eliminado: {}", key);
    }

    /**
     * Genera nombres legibles:
     * TZPW11_20260511_131500_123_1.jpg
     */
    private String buildFileName(
            String patente,
            int index) {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));

        return String.format(
                "%s_%s_%d",
                patente.toUpperCase(),
                timestamp,
                index);
    }

    /**
     * Construye la URL pública del archivo.
     */
    private String buildFileUrl(String key) {

        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,
                key);
    }

    /**
     * Obtiene la extensión del archivo.
     */
    private String getFileExtension(String filename) {

        if (filename == null || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Extrae la key desde la URL completa del archivo.
     */
    private String extractKeyFromUrl(String fileUrl) {

        String domain = String.format(
                "https://%s.s3.%s.amazonaws.com/",
                bucketName,
                region);

        return fileUrl.replace(domain, "");
    }
}