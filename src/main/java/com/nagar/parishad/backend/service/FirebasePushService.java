package com.nagar.parishad.backend.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.nagar.parishad.backend.entity.User;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushService {

    public void sendPushNotification(User user, String title, String body, java.util.Map<String, String> data) {
        if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            AndroidNotification androidNotification = AndroidNotification.builder()
                    .setChannelId("townseva_high_priority")
                    .setPriority(AndroidNotification.Priority.MAX)
                    .setDefaultSound(true)
                    .setDefaultVibrateTimings(true)
                    .setVisibility(AndroidNotification.Visibility.PUBLIC)
                    .build();

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(androidNotification)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent FCM message: " + response);
        } catch (Exception e) {
            System.err.println("Failed to send FCM message to user " + user.getId() + ": " + e.getMessage());
        }
    }
}
