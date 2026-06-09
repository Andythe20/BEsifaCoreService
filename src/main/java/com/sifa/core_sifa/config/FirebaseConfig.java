package com.sifa.core_sifa.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase Admin SDK already initialized");
                return;
            }

            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:firebase/*.json");

            if (resources.length == 0) {
                throw new RuntimeException("No Firebase credentials file found in classpath:firebase/");
            }

            Resource credentialsFile = resources[0];
            log.info("Loading Firebase credentials from: {}", credentialsFile.getFilename());

            try (InputStream serviceAccount = credentialsFile.getInputStream()) {
                // 1. Leer las credenciales básicas
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

                // 2. CRUCIAL: Agregar el scope explícito para Firebase Cloud Messaging
                credentials = credentials.createScoped(
                        java.util.Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));

                log.info("Firebase service account loaded with FCM scopes");

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            }

        } catch (Exception e) {
            log.error("Firebase initialization failed", e);
            throw new RuntimeException("Firebase initialization failed: " + e.getMessage(), e);
        }
    }
}
