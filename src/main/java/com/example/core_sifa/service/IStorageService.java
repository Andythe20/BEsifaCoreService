package com.example.core_sifa.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IStorageService {
    String uploadFile(MultipartFile file, String infraccionId);

    List<String> uploadFiles(List<MultipartFile> files, String infraccionId);

    void deleteFile(String fileUrl);
}
