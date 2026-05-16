package org.example.ais_sst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AisSstApplication {

	public static void main(String[] args) {
		SpringApplication.run(AisSstApplication.class, args);
	}

}
