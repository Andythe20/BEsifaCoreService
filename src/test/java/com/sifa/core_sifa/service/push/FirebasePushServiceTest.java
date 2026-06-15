package com.sifa.core_sifa.service.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class FirebasePushServiceTest {

    private final FirebasePushService pushService = new FirebasePushService();

    @Test
    void send_cuandoExito_retornaMessageId() throws Exception {
        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            var mockInstance = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
            given(mockInstance.send(any(Message.class))).willReturn("message-id-123");

            var result = pushService.send("valid-token", "Title", "Body");

            assertThat(result).isEqualTo("message-id-123");
        }
    }

    @Test
    void send_cuandoFirebaseFalla_lanzaExcepcion() throws Exception {
        try (MockedStatic<FirebaseMessaging> mocked = mockStatic(FirebaseMessaging.class)) {
            var mockInstance = mock(FirebaseMessaging.class);
            mocked.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
            var mockException = mock(FirebaseMessagingException.class);
            given(mockInstance.send(any(Message.class))).willThrow(mockException);

            assertThatThrownBy(() -> pushService.send("bad-token", "Title", "Body"))
                    .isInstanceOf(FirebaseMessagingException.class);
        }
    }
}
