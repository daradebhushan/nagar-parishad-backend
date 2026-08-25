package com.nagar.parishad.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
            // 1. Try environment variable or system property
            String envPath = System.getenv("FIREBASE_CONFIG_PATH");
            if (envPath != null && new File(envPath).exists()) {
                serviceAccount = new FileInputStream(envPath);
            }

            // 2. Try deployment directory path
            if (serviceAccount == null) {
                File prodFile = new File("/Users/bhushan/loknagar-deployment/backend/loknagar-firebase-adminsdk.json");
                if (prodFile.exists()) {
                    serviceAccount = new FileInputStream(prodFile);
                }
            }

            // 3. Try local research path
            if (serviceAccount == null) {
                File researchFile = new File("/Volumes/Extreme SSD/loknagar/antigravity-research/firebase-credentials/loknagar-firebase-adminsdk.json");
                if (researchFile.exists()) {
                    serviceAccount = new FileInputStream(researchFile);
                }
            }

            // 4. Try current working directory
            if (serviceAccount == null) {
                File localFile = new File("loknagar-firebase-adminsdk.json");
                if (localFile.exists()) {
                    serviceAccount = new FileInputStream(localFile);
                }
            }

            // 5. Try classpath
            if (serviceAccount == null) {
                serviceAccount = getClass().getResourceAsStream("/loknagar-firebase-adminsdk.json");
            }
            
            if (serviceAccount == null) {
                System.err.println("Firebase credentials not found. Push notifications will be disabled.");
                return null; // Return null gracefully so the app still starts
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize Firebase Admin SDK: " + e.getMessage());
            return null;
        }
    }
}
