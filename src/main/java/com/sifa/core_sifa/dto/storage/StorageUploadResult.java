package com.sifa.core_sifa.dto.storage;

import lombok.Builder;

/**
 * Resultado de la subida de un archivo al storage.
 * <p>
 * Junto a la URL se entrega el hash SHA-256 del contenido subido y la versión
 * del objeto en S3, para persistir la integridad de la evidencia sin necesidad
 * de volver a leer el archivo desde el cliente.
 */
@Builder
public record StorageUploadResult(
        String url,
        String sha256Hash,
        Integer versionObjeto
) {
}
