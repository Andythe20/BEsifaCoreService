package com.sifa.core_sifa.service.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FirebasePushService implements IPushService {

    @Override
    public String send(String token, String title, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        String messageId = FirebaseMessaging.getInstance().send(message);
        log.info("Push notification sent successfully. MessageId: {}, Token: {}...",
                messageId, token.substring(0, Math.min(token.length(), 20)));
        return messageId;
    }
}
