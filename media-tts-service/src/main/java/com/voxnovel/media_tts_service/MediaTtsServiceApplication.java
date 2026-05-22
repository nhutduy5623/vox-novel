package com.voxnovel.media_tts_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MediaTtsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaTtsServiceApplication.class, args);
	}

}
