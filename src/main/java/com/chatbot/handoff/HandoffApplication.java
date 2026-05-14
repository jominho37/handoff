package com.chatbot.handoff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HandoffApplication {

	public static void main(String[] args) throws IOException {
		String credJson = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");
		if (credJson != null && !credJson.isBlank()) {
			Path credFile = Files.createTempFile("gcp-credentials", ".json");
			Files.writeString(credFile, credJson);
			System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credFile.toString());
		}

		SpringApplication.run(HandoffApplication.class, args);
	}

}
