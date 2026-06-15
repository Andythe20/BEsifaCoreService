package com.sifa.core_sifa;

import com.sifa.core_sifa.config.FirebaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
class CoreSifaApplicationTests {

	@MockitoBean
	private FirebaseConfig firebaseConfig;

	@Test
	void contextLoads() {
	}

}
