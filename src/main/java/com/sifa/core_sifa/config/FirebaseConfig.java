package com.sifa.core_sifa.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    // Inyectamos el JSON directamente desde las variables de entorno
    @Value("${FIREBASE_CREDENTIALS}")
    private String firebaseCredentialsJson;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase Admin SDK already initialized");
                return;
            }

            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:firebase/*.json");

            // Validar que la variable de entorno no esté vacía
            if (firebaseCredentialsJson == null || firebaseCredentialsJson.trim().isEmpty()) {
                throw new RuntimeException("La variable de entorno FIREBASE_CREDENTIALS está vacía o no definida.");
            }
            log.info("Loading Firebase credentials from: variable (String)");

            // Convertir el String JSON a un InputStream en memoria
            try (InputStream serviceAccount = new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8))) {
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
