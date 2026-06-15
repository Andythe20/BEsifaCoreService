package com.sifa.core_sifa.service.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockStorageServiceImplTest {

    private final MockStorageServiceImpl storageService = new MockStorageServiceImpl();

    @Test
    void uploadFile_retornaUrlMock() {
        var file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

        var url = storageService.uploadFile(file, "infraccion_1");

        assertThat(url).startsWith("https://mock.sifa.cl/");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    void uploadFiles_retornaUrlsParaCadaArchivo() {
        List<MultipartFile> files = List.of(
                new MockMultipartFile("file1", "f1.jpg", "image/jpeg", "data1".getBytes()),
                new MockMultipartFile("file2", "f2.jpg", "image/jpeg", "data2".getBytes())
        );

        var urls = storageService.uploadFiles(files, "infraccion_1");

        assertThat(urls).hasSize(2);
        assertThat(urls.get(0)).contains("infraccion_1_1");
        assertThat(urls.get(1)).contains("infraccion_1_2");
    }

    @Test
    void deleteFile_noLanzaExcepcion() {
        storageService.deleteFile("https://mock.sifa.cl/test.jpg");
    }
}
