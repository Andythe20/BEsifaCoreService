package com.sifa.core_sifa.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sifa.core_sifa.service.IStorageService;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("dev")
@Slf4j
public class MockStorageServiceImpl implements IStorageService {

    @Override
    public String uploadFile(MultipartFile file, String fileName) {

        // URL falsa para desarrollo local
        String mockUrl = "https://mock.sifa.cl/"
                + fileName
                + ".jpg";

        log.info("MOCK STORAGE -> Archivo simulado: {}", mockUrl);

        return mockUrl;
    }

    @Override
    public List<String> uploadFiles(
            List<MultipartFile> files,
            String infraccionId) {

        List<String> urls = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {

            urls.add(
                    uploadFile(
                            files.get(i),
                            infraccionId + "_" + (i + 1)
                    )
            );
        }

        return urls;
    }

    @Override
    public void deleteFile(String fileUrl) {

        log.info("MOCK STORAGE -> Archivo eliminado: {}", fileUrl);
    }
}