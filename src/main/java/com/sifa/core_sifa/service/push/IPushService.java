package com.sifa.core_sifa.service.push;

import com.google.firebase.messaging.FirebaseMessagingException;

public interface IPushService {

    String send(String token, String title, String body) throws FirebaseMessagingException;
}
