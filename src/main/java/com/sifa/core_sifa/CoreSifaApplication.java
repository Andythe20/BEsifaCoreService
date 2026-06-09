package com.sifa.core_sifa;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreSifaApplication {

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
	}

	public static void main(String[] args) {
		SpringApplication.run(CoreSifaApplication.class, args);
	}

}
