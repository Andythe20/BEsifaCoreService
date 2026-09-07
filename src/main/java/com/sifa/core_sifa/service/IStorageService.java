package com.sifa.core_sifa.service;

import com.sifa.core_sifa.dto.storage.StorageUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IStorageService {
    String uploadFile(MultipartFile file, String infraccionId);

    List<String> uploadFiles(List<MultipartFile> files, String infraccionId);

    /**
     * Sube varios archivos entregando, además de la URL, el hash SHA-256 del
     * contenido y la versión del objeto en S3. Permite persistir la integridad
     * de la evidencia en el momento de la recepción.
     */
    List<StorageUploadResult> uploadFilesDetailed(List<MultipartFile> files, String infraccionId);

    void deleteFile(String fileUrl);

    String uploadApk(MultipartFile file);

    /**
     * Descarga el contenido binario del archivo referenciado por su URL.
     * Necesario para re-calcular el hash y verificar la integridad al consultar.
     */
    byte[] downloadFile(String fileUrl);

}
