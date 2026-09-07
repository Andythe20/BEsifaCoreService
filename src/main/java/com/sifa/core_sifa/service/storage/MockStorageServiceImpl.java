package com.sifa.core_sifa.service.storage;

import com.sifa.core_sifa.dto.storage.StorageUploadResult;
import com.sifa.core_sifa.service.IStorageService;
import com.sifa.core_sifa.util.ChecksumUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación de desarrollo/local del storage.
 * <p>
 * Guarda el contenido de los archivos en memoria (hash + bytes) para que el
 * flujo de integridad (calcular/recalcular hash) pueda probarse localmente
 * sin depender de un bucket S3 real.
 */
@Service
@Profile("dev")
@Slf4j
public class MockStorageServiceImpl implements IStorageService {

    // Mapa URL -> contenido binario del archivo en memoria
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String uploadFile(MultipartFile file, String fileName) {

        String mockUrl = "https://mock.sifa.cl/" + fileName + ".jpg";

        store(file, mockUrl);

        log.info("MOCK STORAGE -> Archivo simulado: {}", mockUrl);

        return mockUrl;
    }

    @Override
    public List<String> uploadFiles(
            List<MultipartFile> files,
            String infraccionId) {

        List<String> urls = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            urls.add(uploadFile(files.get(i), infraccionId + "_" + (i + 1)));
        }

        return urls;
    }

    @Override
    public List<StorageUploadResult> uploadFilesDetailed(List<MultipartFile> files, String infraccionId) {
        List<StorageUploadResult> results = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String mockUrl = "https://mock.sifa.cl/" + infraccionId + "_" + (i + 1) + ".jpg";
            store(file, mockUrl);

            results.add(StorageUploadResult.builder()
                    .url(mockUrl)
                    .sha256Hash(computeHash(file))
                    .versionObjeto(0)
                    .build());
        }

        return results;
    }

    @Override
    public void deleteFile(String fileUrl) {
        storage.remove(fileUrl);
        log.info("MOCK STORAGE -> Archivo eliminado: {}", fileUrl);
    }

    @Override
    public String uploadApk(MultipartFile file) {

        String mockUrl = "https://mock.sifa.cl/app/sifa_go.apk";

        store(file, mockUrl);

        log.info("MOCK STORAGE -> APK simulado: {}", mockUrl);

        return mockUrl;
    }

    @Override
    public byte[] downloadFile(String fileUrl) {
        byte[] bytes = storage.get(fileUrl);
        if (bytes == null) {
            throw new RuntimeException("No se pudo descargar el archivo (no existe en memoria): " + fileUrl);
        }
        return bytes;
    }

    private void store(MultipartFile file, String url) {
        try {
            storage.put(url, file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar el archivo en memoria", e);
        }
    }

    private String computeHash(MultipartFile file) {
        try {
            return ChecksumUtil.sha256(file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular el hash del archivo", e);
        }
    }
}
