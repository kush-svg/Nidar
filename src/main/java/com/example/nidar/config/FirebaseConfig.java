package com.example.nidar.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;


    @Bean
    public FirebaseApp firebaseApp() {
        try {
            // Guard against re-initialization on hot reload
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }

            // Load service account JSON from classpath (src/main/resources/)
            InputStream serviceAccount =
                    new ClassPathResource(serviceAccountPath).getInputStream();

            FirebaseOptions builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(builder);
        } catch (Exception e) {
            System.err.println("Warning: Firebase initialization failed, returning null. Reason: " + e.getMessage());
            return null;
        }
    }
}
